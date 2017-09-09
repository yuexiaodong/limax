package limax.pkix.tool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import limax.codec.Octets;
import limax.codec.SinkOctets;
import limax.codec.StreamSource;
import limax.pkix.CAService;
import limax.pkix.ExtKeyUsage;
import limax.pkix.KeyUsage;
import limax.pkix.X509CertificateRenewParameter;
import limax.pkix.X509EndEntityCertificateParameter;
import limax.util.SecurityUtils;
import limax.util.Trace;

class CertUpdateServer {
	private static final int RESTART_DELAY_SECOND = 2;
	private static final long RESTART_MILLISECOND_BEFORE_CERTIFICATE_EXPIRE = 10000L;
	private static final char[] passphrase = "passphrase".toCharArray();
	private final CAService ca;
	private final String domain;
	private final int port;
	private final int renewLifespanPercent;
	private final String signatureAlgorithm;
	private final int lifetime;
	private final OcspServer ocspServer;
	private final Archive archive;
	private final ScheduledExecutorService scheduler;

	private class Handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			SSLSession session = ((HttpsExchange) exchange).getSSLSession();
			javax.security.cert.X509Certificate peer = session.getPeerCertificateChain()[0];
			long notAfter = peer.getNotAfter().getTime();
			long notBefore = peer.getNotBefore().getTime();
			long now = System.currentTimeMillis();
			Headers headers = exchange.getResponseHeaders();
			if ((now - notBefore) * 100 > renewLifespanPercent * (notAfter - notBefore)) {
				String response = "";
				try (InputStream in = exchange.getRequestBody()) {
					X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
							.generateCertificate(new ByteArrayInputStream(peer.getEncoded()));
					Octets data = new Octets();
					new StreamSource(in, new SinkOctets(data)).flush();
					PublicKey publicKey = SecurityUtils.PublicKeyAlgorithm
							.loadPublicKey(new X509EncodedKeySpec(data.getBytes()));
					X509Certificate[] chain = ca.sign(new X509CertificateRenewParameter() {
						@Override
						public X509Certificate getCertificate() {
							return cert;
						}

						@Override
						public PublicKey getPublicKey() {
							return publicKey;
						}

						@Override
						public Function<X509Certificate, URI> getCRLDPMapping() {
							return cacert -> ocspServer.getCRLDP(cacert);
						}
					});
					archive.store(chain[0]);
					response = SecurityUtils.assemblePKCS7(chain);
					headers.set("Content-Type", "application/x-pkcs7-certificates");
					if (Trace.isInfoEnabled())
						Trace.info("CertUpdateServer renew [" + cert.getSubjectX500Principal() + "] expire at ["
								+ new Date(notAfter - notBefore + now) + "]");
				} catch (Exception e) {
				}
				exchange.sendResponseHeaders(200, response.length());
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response.getBytes());
				}
			} else {
				exchange.sendResponseHeaders(200, -1);
			}
		}
	}

	CertUpdateServer(CAService ca, String domain, int port, int renewLifespanPercent, String signatureAlgorithm,
			int lifetime, OcspServer ocspServer, Archive archive, ScheduledExecutorService scheduler) {
		this.ca = ca;
		this.domain = domain;
		this.port = port;
		this.renewLifespanPercent = renewLifespanPercent;
		this.signatureAlgorithm = signatureAlgorithm;
		this.lifetime = lifetime;
		this.ocspServer = ocspServer;
		this.scheduler = scheduler;
		this.archive = archive;
	}

	void start() throws Exception {
		KeyPair keyPair = Main.keyPairGenerator(ca, signatureAlgorithm);
		X500Principal subject = new X500Principal("CN=" + domain);
		X509Certificate[] chain = ca.sign(new X509EndEntityCertificateParameter() {
			@Override
			public X500Principal getSubject() {
				return subject;
			}

			@Override
			public PublicKey getPublicKey() {
				return keyPair.getPublic();
			}

			@Override
			public EnumSet<KeyUsage> getKeyUsages() {
				return EnumSet.of(KeyUsage.digitalSignature, KeyUsage.keyEncipherment);
			}

			@Override
			public EnumSet<ExtKeyUsage> getExtKeyUsages() {
				return EnumSet.of(ExtKeyUsage.ServerAuth);
			}

			@Override
			public URI getOcspURI() {
				return ocspServer.getOcspURI();
			}

			@Override
			public Function<X509Certificate, URI> getCRLDPMapping() {
				return cacert -> ocspServer.getCRLDP(cacert);
			}

			@Override
			public Date getNotAfter() {
				return new Date(getNotBefore().getTime() + TimeUnit.DAYS.toMillis(lifetime));
			}
		});
		X509Certificate[] cas = ca.getCACertificates();
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		keyStore.setKeyEntry("", keyPair.getPrivate(), passphrase, Arrays.copyOf(chain, chain.length - 1));
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("PKIX");
		keyManagerFactory.init(keyStore, passphrase);
		Set<TrustAnchor> trustAnchors = Stream.concat(Arrays.stream(cas), Stream.of(chain[chain.length - 1]))
				.map(cert -> new TrustAnchor(cert, null)).collect(Collectors.toSet());
		X509CertSelector selector = new X509CertSelector();
		selector.setIssuer(cas[0].getSubjectX500Principal());
		PKIXBuilderParameters pkixBuilderParameters = new PKIXBuilderParameters(trustAnchors, selector);
		pkixBuilderParameters.addCertPathChecker(ocspServer.getCertPathChecker());
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
		trustManagerFactory.init(new CertPathTrustManagerParameters(pkixBuilderParameters));
		SSLContext ctx = SSLContext.getInstance("TLSv1.2");
		ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		InetSocketAddress addr = new InetSocketAddress(port);
		HttpsServer server = HttpsServer.create(addr, 0);
		server.setHttpsConfigurator(new HttpsConfigurator(ctx) {
			public void configure(HttpsParameters params) {
				params.setWantClientAuth(true);
			}
		});
		server.createContext("/", new Handler());
		server.setExecutor(null);
		server.start();
		if (Trace.isInfoEnabled())
			Trace.info("CertUpdateServer start on " + addr);
		scheduler.schedule(() -> {
			if (Trace.isInfoEnabled())
				Trace.info("CertUpdateServer restarting on expire.");
			server.stop(RESTART_DELAY_SECOND);
			try {
				start();
			} catch (Exception e) {
				Trace.fatal("CertUpdateServer fail", e);
			}
		}, chain[0].getNotAfter().getTime() - System.currentTimeMillis()
				- RESTART_MILLISECOND_BEFORE_CERTIFICATE_EXPIRE, TimeUnit.MILLISECONDS);
	}
}
