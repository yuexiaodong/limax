package limax.pkix;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import limax.codec.asn1.ASN1OctetString;
import limax.codec.asn1.ASN1PrimitiveObject;
import limax.codec.asn1.ASN1Sequence;
import limax.codec.asn1.DecodeBER;
import limax.util.ConcurrentEnvironment;
import limax.util.SecurityUtils;
import limax.util.Trace;

public class SSLContextAllocator {
	private static final int renewLifespanPercent = Integer
			.getInteger("limax.pkix.SSLContextAllocator.renewLifespanPercent", 80);
	private static final long renewRetryDelay = Long.getLong("limax.pkix.SSLContextAllocator.RenewRetryDelay",
			TimeUnit.HOURS.toMillis(1));
	private static final ScheduledExecutorService scheduler = ConcurrentEnvironment.getInstance()
			.newScheduledThreadPool("SSLContextAllocator scheduler", 2, true);
	private final KeyInfo keyInfo;
	private final URL url;
	private Future<?> future;
	private final Set<TrustAnchor> trustAnchors = new HashSet<>();
	private final AtomicInteger serial = new AtomicInteger();
	private final Map<Integer, Consumer<SSLContextAllocator>> listeners = new ConcurrentHashMap<>();
	private Set<PKIXRevocationChecker.Option> options;

	private void action(X509Certificate cert) {
		try {
			PublicKey publicKey = cert.getPublicKey();
			KeyPair keyPair = SecurityUtils.PublicKeyAlgorithm.valueOf(publicKey).reKey(publicKey);
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			X509CertSelector selector = new X509CertSelector();
			selector.setIssuer(cert.getIssuerX500Principal());
			conn.setSSLSocketFactory(keyInfo.createSSLContext(trustAnchors, selector, null).getSocketFactory());
			conn.setDoOutput(true);
			conn.connect();
			try (OutputStream out = conn.getOutputStream()) {
				out.write(keyPair.getPublic().getEncoded());
			}
			try (InputStream in = conn.getInputStream()) {
				keyInfo.setKeyEntry(keyPair.getPrivate(),
						CertificateFactory.getInstance("X.509").generateCertificates(in).toArray(new Certificate[0]));
			}
			listeners.values().forEach(l -> l.accept(this));
			schedule();
		} catch (Exception e) {
			if (Trace.isWarnEnabled())
				Trace.warn("Certificate renew fail, backoff " + renewRetryDelay + " ms.", e);
		}
	}

	private synchronized void schedule() throws Exception {
		if (future != null)
			future.cancel(true);
		X509Certificate cert = (X509Certificate) keyInfo.getCertificateChain()[0];
		long notBefore = cert.getNotBefore().getTime();
		long notAfter = cert.getNotAfter().getTime();
		long nextUpdate = (notAfter - notBefore) * renewLifespanPercent / 100 + notBefore;
		future = scheduler.scheduleAtFixedRate(() -> action(cert), nextUpdate - System.currentTimeMillis(),
				renewRetryDelay, TimeUnit.MILLISECONDS);
		if (Trace.isInfoEnabled())
			Trace.info("Certificate [" + cert.getSubjectX500Principal() + "] expire at [" + new Date(notAfter)
					+ "] renew task schedule at [" + new Date(nextUpdate) + "]");
	}

	public SSLContextAllocator(URI location, Function<String, char[]> passphraseCallback) throws Exception {
		keyInfo = KeyInfo.load(location, passphraseCallback);
		Certificate[] chain = keyInfo.getCertificateChain();
		X509Certificate cert = (X509Certificate) chain[0];
		ASN1Sequence accessDescriptions = (ASN1Sequence) DecodeBER
				.decode(((ASN1OctetString) DecodeBER.decode(cert.getExtensionValue("1.3.6.1.5.5.7.1.1"))).get());
		ASN1Sequence accessDescription = (ASN1Sequence) accessDescriptions.get(0);
		ASN1PrimitiveObject accessLocation = (ASN1PrimitiveObject) accessDescription.get(1);
		URI uri = URI.create(new String(accessLocation.getData()));
		url = new URL("https", uri.getHost(), uri.getPath());
		addTrust(chain[chain.length - 1]);
		schedule();
	}

	public void setRevocationCheckerOptions(Set<PKIXRevocationChecker.Option> options) {
		this.options = options;
	}

	public void addTrust(TrustAnchor anchor) {
		trustAnchors.add(anchor);
	}

	public void addTrust(Certificate cert) {
		addTrust(new TrustAnchor((X509Certificate) cert, null));
	}

	public void addTrust(InputStream x509ORpkcs7Stream) throws CertificateException {
		CertificateFactory.getInstance("X.509").generateCertificates(x509ORpkcs7Stream).forEach(cert -> addTrust(cert));
	}

	public void addTrust(KeyStore keyStore) throws KeyStoreException {
		for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements();) {
			String alias = e.nextElement();
			if (keyStore.isCertificateEntry(alias))
				addTrust(keyStore.getCertificate(alias));
		}
	}

	public int addChangeListener(Consumer<SSLContextAllocator> listener) {
		int id = serial.getAndIncrement();
		listeners.put(id, listener);
		return id;
	}

	public void removeChangeListener(int id) {
		listeners.remove(id);
	}

	public SSLContext alloc() throws Exception {
		return alloc(null);
	}

	public SSLContext alloc(X509CertSelector selector) throws Exception {
		return keyInfo.createSSLContext(trustAnchors, selector, options);
	}
}
