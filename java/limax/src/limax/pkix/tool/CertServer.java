package limax.pkix.tool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.security.auth.x500.X500Principal;

import org.w3c.dom.Element;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import limax.codec.JSON;
import limax.codec.JSONException;
import limax.codec.Octets;
import limax.codec.RFC2822Address;
import limax.codec.SHA1;
import limax.codec.SinkOctets;
import limax.codec.StreamSource;
import limax.codec.asn1.ASN1ObjectIdentifier;
import limax.pkix.CAService;
import limax.pkix.ExtKeyUsage;
import limax.pkix.GeneralName;
import limax.pkix.KeyUsage;
import limax.pkix.X509EndEntityCertificateParameter;
import limax.util.ElementHelper;
import limax.util.Helper;
import limax.util.SecurityUtils;
import limax.util.Trace;
import limax.util.XMLUtils;

class CertServer {
	private final static ThreadLocal<SimpleDateFormat> defaultDateFormat = ThreadLocal
			.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
	private final static long CACHE_TIME_OUT = 30000;
	private final static int MASK_USAGE_MANDATORY = 2;
	private final static int MASK_USAGE_TRUE = 1;
	private final CAService ca;
	private final Date caLifetime;
	private final int port;
	private final OcspServer ocspServer;
	private final AuthCode authCode;
	private final Archive archive;
	private final KeyPairGenerator privateKeyGenerator;
	private final Pattern subjectPattern;
	private final String subjectTemplate;
	private final boolean notBeforeMandatory;
	private final boolean notAfterMandatory;
	private final long notAfterPeriod;
	private final long notAfterPeriodLow;
	private final long notAfterPeriodHigh;
	private final Map<KeyUsage, Integer> keyUsage;
	private final Map<ExtKeyUsage, Integer> extKeyUsage;

	private static class DownloadCache {
		private static final Map<String, DownloadCache> cache = new ConcurrentHashMap<>();
		private static final Timer timer;
		private final String key;
		private final byte[] data;
		private final long timestamp;
		static {
			timer = new Timer(true);
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					long now = System.currentTimeMillis();
					for (Iterator<Map.Entry<String, DownloadCache>> it = cache.entrySet().iterator(); it.hasNext();) {
						if (it.next().getValue().timestamp < now)
							it.remove();
					}
				}
			}, CACHE_TIME_OUT, CACHE_TIME_OUT);
		}

		DownloadCache(String suffix, byte[] data) {
			this.key = Helper.toHexString(SHA1.digest(data)) + suffix;
			this.data = data;
			this.timestamp = System.currentTimeMillis();
			cache.put(this.key, this);
		}

		static DownloadCache get(String key) {
			return cache.get(key);
		}
	}

	class Handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String path = exchange.getRequestURI().getPath();
			Headers headers = exchange.getResponseHeaders();
			if (path.length() > 1) {
				DownloadCache c = DownloadCache.get(path.substring(1));
				if (c != null) {
					headers.set("Content-Type", Files.probeContentType(Paths.get(c.key)));
					headers.set("Cache-Control", "no-store");
					exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, c.data.length);
					try (OutputStream os = exchange.getResponseBody()) {
						os.write(c.data);
					}
					return;
				}
			}
			headers.set("Location", "/CertServer.html");
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_PERM, -1);
		}
	}

	private static JSON requestJSON(HttpExchange exchange) throws IOException {
		try {
			return JSON.parse(exchange.getRequestURI().getQuery());
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}

	private static void responseJSON(HttpExchange exchange, Object obj) throws IOException {
		byte[] data;
		try {
			data = JSON.stringify(obj).getBytes();
		} catch (JSONException e) {
			throw new IOException(e);
		}
		Headers headers = exchange.getResponseHeaders();
		headers.set("Content-Type", "application/json");
		headers.set("Cache-Control", "no-store");
		exchange.sendResponseHeaders(200, data.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(data);
		}
	}

	private X500Principal getSubject(JSON json, Map<String, Object> transform) {
		try {
			String subject = ((JSON) json.get("subject")).toString();
			if (!subject.isEmpty() && subjectPattern.matcher(subject).matches()) {
				X500Principal principal = new X500Principal(subject);
				transform.put("subject", principal.toString());
				return principal;
			}
		} catch (Exception e) {
		}
		transform.put("subject", null);
		return null;
	}

	private static Collection<GeneralName> getSubjectAltNames(JSON json, Map<String, Object> transform) {
		try {
			JSON list = json.get("subjectAltNames");
			if (list.isUndefined())
				return Collections.emptyList();
			List<GeneralName> subjectAltNames = new ArrayList<>();
			List<Object> results = new ArrayList<>();
			for (JSON item : list.toArray()) {
				try {
					String key = item.keySet().iterator().next();
					String val = item.get(key).toString();
					if (val.isEmpty())
						throw new RuntimeException();
					Object obj;
					switch (GeneralName.Type.valueOf(key)) {
					case rfc822Name:
						RFC2822Address address = new RFC2822Address(val);
						subjectAltNames.add(GeneralName.createRFC822Name(address));
						obj = address;
						break;
					case dNSName:
						subjectAltNames.add(GeneralName.createDNSName(val));
						obj = val;
						break;
					case directoryName:
						X500Principal name = new X500Principal(val);
						subjectAltNames.add(GeneralName.createDirectoryName(name));
						obj = name;
						break;
					case uniformResourceIdentifier:
						URI uri = new URI(val);
						subjectAltNames.add(GeneralName.createUniformResourceIdentifier(uri));
						obj = uri;
						break;
					case iPAddress:
						InetAddress ip = InetAddress.getByName(val);
						subjectAltNames.add(GeneralName.createIPAddress(ip));
						obj = ip.getHostAddress();
						break;
					case registeredID:
						subjectAltNames.add(GeneralName.createRegisteredID(val));
						obj = new ASN1ObjectIdentifier(val);
						break;
					default:
						throw new RuntimeException();
					}
					results.add(obj.toString());
				} catch (Exception e) {
					results.add(null);
				}
			}
			transform.put("subjectAltNames", results);
			return subjectAltNames;
		} catch (JSONException e) {
		}
		return null;
	}

	private Date getNotBefore(JSON json, Map<String, Object> transform) {
		try {
			String date = notBeforeMandatory ? defaultDateFormat.get().format(new Date())
					: json.get("notBefore").toString();
			transform.put("notBefore", date);
			return defaultDateFormat.get().parse(date);
		} catch (Exception e) {
		}
		transform.put("notBefore", null);
		return null;
	}

	private Date getNotAfter(JSON json, Date notBefore, Map<String, Object> transform) {
		try {
			String date = notAfterMandatory
					? defaultDateFormat.get().format(new Date(notBefore.getTime() + notAfterPeriod))
					: json.get("notAfter").toString();
			transform.put("notAfter", date);
			return defaultDateFormat.get().parse(date);
		} catch (Exception e) {
		}
		transform.put("notAfter", null);
		return null;
	}

	private EnumSet<KeyUsage> getKeyUsages(JSON json, Map<String, Object> transform) {
		try {
			List<KeyUsage> chosen = new ArrayList<>();
			for (Map.Entry<KeyUsage, Integer> e : keyUsage.entrySet())
				if (e.getValue() == (MASK_USAGE_MANDATORY | MASK_USAGE_TRUE))
					chosen.add(e.getKey());
			JSON list = json.get("keyUsage");
			if (!list.isUndefined()) {
				for (JSON item : list.toArray()) {
					try {
						KeyUsage usage = KeyUsage.valueOf(item.toString());
						if ((keyUsage.get(usage) & MASK_USAGE_MANDATORY) == 0)
							chosen.add(usage);
					} catch (Exception e) {
					}
				}
			}
			transform.put("keyUsage", chosen.stream().map(Object::toString).collect(Collectors.toList()));
			return chosen.isEmpty() ? EnumSet.noneOf(KeyUsage.class) : EnumSet.copyOf(chosen);
		} catch (JSONException e) {
		}
		return null;
	}

	private EnumSet<ExtKeyUsage> getExtKeyUsages(JSON json, Map<String, Object> transform) {
		try {
			List<ExtKeyUsage> chosen = new ArrayList<>();
			for (Map.Entry<ExtKeyUsage, Integer> e : extKeyUsage.entrySet())
				if (e.getValue() == (MASK_USAGE_MANDATORY | MASK_USAGE_TRUE))
					chosen.add(e.getKey());
			JSON list = json.get("extKeyUsage");
			if (!list.isUndefined()) {
				for (JSON item : list.toArray()) {
					try {
						ExtKeyUsage usage = ExtKeyUsage.valueOf(item.toString());
						if ((extKeyUsage.get(usage) & MASK_USAGE_MANDATORY) == 0)
							chosen.add(usage);
					} catch (Exception e) {
					}
				}
			}
			transform.put("extKeyUsage", chosen.stream().map(Object::toString).collect(Collectors.toList()));
			return chosen.isEmpty() ? EnumSet.noneOf(ExtKeyUsage.class) : EnumSet.copyOf(chosen);
		} catch (JSONException e) {
		}
		return null;
	}

	private static PublicKey getPublicKey(JSON json, Map<String, Object> transform) {
		try {
			JSON publicKey = json.get("publicKey");
			return publicKey.isUndefined() ? null
					: SecurityUtils.PublicKeyAlgorithm
							.loadPublicKey((X509EncodedKeySpec) SecurityUtils.loadPEM(publicKey.toString(), null));
		} catch (Exception e) {
		}
		transform.put("publicKey", null);
		return null;
	}

	private static char[] getPassphrase(JSON json) {
		try {
			JSON passphrase = json.get("pkcs12passphrase");
			if (passphrase.isString())
				return passphrase.toString().toCharArray();
		} catch (Exception e) {
		}
		return null;
	}

	private boolean verifyAuthCode(JSON json, Map<String, Object> transform) {
		try {
			if (authCode.verify(json.get("authCode").toString()))
				return true;
		} catch (Exception e) {
		}
		transform.put("authCode", null);
		return false;
	}

	private class HandlerSign implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			JSON json = requestJSON(exchange);
			if (json.isNull()) {
				Map<String, Object> initial = new HashMap<>();
				long now = System.currentTimeMillis();
				initial.put("subject", subjectTemplate);
				initial.put("notBefore",
						Collections.singletonMap(defaultDateFormat.get().format(new Date(now)), notBeforeMandatory));
				initial.put("notAfter", defaultDateFormat.get().format(new Date(now + notAfterPeriod)));
				initial.put("keyUsage", keyUsage);
				initial.put("extKeyUsage", extKeyUsage);
				responseJSON(exchange, initial);
				return;
			}
			Map<String, Object> transform = new HashMap<>();
			X500Principal subject = getSubject(json, transform);
			Collection<GeneralName> subjectAltNames = getSubjectAltNames(json, transform);
			Date notBefore = getNotBefore(json, transform);
			Date notAfter = getNotAfter(json, notBefore, transform);
			if (notAfter != null && notBefore != null) {
				long delta = notAfter.getTime() - notBefore.getTime();
				if (delta < notAfterPeriodLow || delta > notAfterPeriodHigh)
					transform.put("notAfter", null);
				if (notAfter.after(caLifetime)) {
					transform.put("notAfter", null);
					if (Trace.isErrorEnabled()) {
						Trace.error("CertServer CANNOT signature certificate notAfter = " + notAfter
								+ " but CA's lifetime = " + caLifetime);
					}
				}
			}
			EnumSet<KeyUsage> keyUsages = getKeyUsages(json, transform);
			EnumSet<ExtKeyUsage> extKeyUsages = getExtKeyUsages(json, transform);
			PublicKey publicKey = getPublicKey(json, transform);
			char[] passphrase = getPassphrase(json);
			if (verifyAuthCode(json, transform) && subject != null && subjectAltNames != null && notBefore != null
					&& notAfter != null && keyUsages != null && extKeyUsages != null
					&& (publicKey != null || passphrase != null)) {
				PublicKey _publicKey;
				PrivateKey _privateKey;
				if (publicKey == null) {
					KeyPair keyPair;
					synchronized (privateKeyGenerator) {
						keyPair = privateKeyGenerator.generateKeyPair();
					}
					_publicKey = keyPair.getPublic();
					_privateKey = keyPair.getPrivate();
				} else {
					_publicKey = publicKey;
					_privateKey = null;
				}
				try {
					X509Certificate[] chain = ca.sign(new X509EndEntityCertificateParameter() {
						@Override
						public X500Principal getSubject() {
							return subject;
						}

						@Override
						public Collection<GeneralName> getSubjectAltNames() {
							return subjectAltNames;
						}

						@Override
						public Date getNotBefore() {
							return notBefore;
						}

						@Override
						public Date getNotAfter() {
							return notAfter;
						}

						@Override
						public EnumSet<KeyUsage> getKeyUsages() {
							return keyUsages;
						}

						@Override
						public EnumSet<ExtKeyUsage> getExtKeyUsages() {
							return extKeyUsages;
						}

						@Override
						public PublicKey getPublicKey() {
							return _publicKey;
						}

						@Override
						public URI getOcspURI() {
							return ocspServer.getOcspURI();
						}

						@Override
						public Function<X509Certificate, URI> getCRLDPMapping() {
							return cacert -> ocspServer.getCRLDP(cacert);
						}
					});
					archive.store(chain[0]);
					DownloadCache cache;
					if (_privateKey == null) {
						cache = new DownloadCache(".p7b", SecurityUtils.assemblePKCS7(chain).getBytes());
					} else {
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
						pkcs12.load(null, null);
						pkcs12.setKeyEntry("", _privateKey, passphrase, chain);
						pkcs12.store(os, passphrase);
						cache = new DownloadCache(".p12", os.toByteArray());
					}
					transform.put("retrieveKey", cache.key);
					if (Trace.isInfoEnabled())
						Trace.info("CertServer sign [" + subject.toString() + "]");
				} catch (Exception e) {
					if (Trace.isErrorEnabled())
						Trace.error("CertServer sign certificate", e);
				}
			}
			responseJSON(exchange, transform);
		}
	}

	private interface X509CertificateConsumer {
		void accept(X509Certificate cert) throws Exception;
	}

	private class HandlerMaintance implements HttpHandler {
		private final String opname;
		private final X509CertificateConsumer consumer;

		HandlerMaintance(String op, X509CertificateConsumer consumer) {
			this.opname = op;
			this.consumer = consumer;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			JSON json = requestJSON(exchange);
			Map<String, Object> transform = new HashMap<>();
			X509Certificate certificate = null;
			try {
				Collection<? extends Certificate> chain = CertificateFactory.getInstance("X.509")
						.generateCertificates(new ByteArrayInputStream(json.get("certificate").toString().getBytes()));
				certificate = chain.size() == 1 ? (X509Certificate) chain.iterator().next()
						: (X509Certificate) SecurityUtils.sortCertificateChain(chain.toArray(new Certificate[0]))[0];
			} catch (Exception e) {
				transform.put("certificate", null);
			}
			if (certificate != null && verifyAuthCode(json, transform)) {
				Map<String, Object> r = new HashMap<>();
				try {
					consumer.accept(certificate);
					r.put("status", true);
					r.put("message", "Certificate " + certificate.getSerialNumber().toString(16) + " " + opname);
					if (Trace.isInfoEnabled())
						Trace.info(
								"CertServer " + opname + " [" + certificate.getSubjectX500Principal().toString() + "]");
				} catch (Exception e) {
					r.put("status", false);
					r.put("message", e.getMessage());
				}
				transform.put("result", r);
			}
			responseJSON(exchange, transform);
		}
	}

	CertServer(CAService ca, int port, OcspServer ocspServer, Element root, AuthCode authCode, Archive archive)
			throws Exception {
		this.ca = ca;
		this.caLifetime = Arrays.stream(ca.getCACertificates()).map(X509Certificate::getNotAfter)
				.max(Comparator.comparingLong(Date::getTime)).get();
		this.port = port;
		this.ocspServer = ocspServer;
		this.authCode = authCode;
		this.archive = archive;
		ElementHelper eh = new ElementHelper(root);
		String[] algo = eh.getString("publicKeyAlgorithmForGeneratePKCS12", "rsa/1024").split("/");
		this.privateKeyGenerator = KeyPairGenerator.getInstance(algo[0].toUpperCase());
		this.privateKeyGenerator.initialize(Integer.parseInt(algo[1]));
		eh = XMLUtils.getChildElements(root).stream().filter(e -> e.getTagName().equals("Subject"))
				.map(ElementHelper::new).findAny().get();
		this.subjectPattern = Pattern.compile(eh.getString("pattern", ".*"),
				eh.getBoolean("patternIgnorecase", false) ? Pattern.CASE_INSENSITIVE : 0);
		this.subjectTemplate = eh.getString("template");
		eh = XMLUtils.getChildElements(root).stream().filter(e -> e.getTagName().equals("NotBefore"))
				.map(ElementHelper::new).findAny().get();
		this.notBeforeMandatory = eh.getBoolean("mandatory", false);
		eh = XMLUtils.getChildElements(root).stream().filter(e -> e.getTagName().equals("NotAfter"))
				.map(ElementHelper::new).findAny().get();
		this.notAfterMandatory = eh.getBoolean("mandatory", false);
		this.notAfterPeriod = TimeUnit.DAYS.toMillis(eh.getInt("period", 30));
		if (notAfterMandatory) {
			this.notAfterPeriodLow = this.notAfterPeriodHigh = this.notAfterPeriod;
		} else {
			this.notAfterPeriodLow = TimeUnit.DAYS.toMillis(eh.getInt("periodLow"));
			this.notAfterPeriodHigh = TimeUnit.DAYS.toMillis(eh.getInt("periodHigh"));
		}
		if (notAfterPeriodLow > notAfterPeriodHigh || notAfterPeriod < notAfterPeriodLow
				|| notAfterPeriod > notAfterPeriodHigh) {
			Trace.fatal("CertServer NotAfter config error, periodLow <= period <= periodHigh must be satisfied.");
			System.exit(-1);
		}
		this.keyUsage = XMLUtils
				.getChildElements(XMLUtils.getChildElements(root).stream()
						.filter(e -> e.getTagName().equals("KeyUsage")).findAny().get())
				.stream().collect(() -> new EnumMap<KeyUsage, Integer>(KeyUsage.class), (m, e) -> {
					int mask = 0;
					ElementHelper _eh = new ElementHelper(e);
					if (_eh.getBoolean("mandatory", false))
						mask |= MASK_USAGE_MANDATORY;
					if (_eh.getBoolean("default", false))
						mask |= MASK_USAGE_TRUE;
					m.put(KeyUsage.valueOf(e.getTagName()), mask);
				}, EnumMap::putAll);
		this.extKeyUsage = XMLUtils
				.getChildElements(XMLUtils.getChildElements(root).stream()
						.filter(e -> e.getTagName().equals("ExtKeyUsage")).findAny().get())
				.stream().collect(() -> new EnumMap<ExtKeyUsage, Integer>(ExtKeyUsage.class), (m, e) -> {
					int mask = 0;
					ElementHelper _eh = new ElementHelper(e);
					if (_eh.getBoolean("mandatory", false))
						mask |= MASK_USAGE_MANDATORY;
					if (_eh.getBoolean("default", false))
						mask |= MASK_USAGE_TRUE;
					m.put(ExtKeyUsage.valueOf(e.getTagName()), mask);
				}, EnumMap::putAll);
	}

	void start() throws Exception {
		InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
		HttpServer server = HttpServer.create(addr, 0);
		server.createContext("/", new Handler());
		server.createContext("/sign", new HandlerSign());
		server.createContext("/revoke", new HandlerMaintance("revoke", c -> ocspServer.revoke(c)));
		server.createContext("/recall", new HandlerMaintance("recall", c -> ocspServer.recall(c)));
		Octets data = new Octets();
		try (InputStream in = CertServer.class.getResourceAsStream("CertServer.html")) {
			new StreamSource(in, new SinkOctets(data)).flush();
		}
		StaticWebData html = new StaticWebData(data.getBytes(), "text/html; charset=utf-8");
		server.createContext("/CertServer.html", exchange -> html.transfer(exchange));
		data.clear();
		try (InputStream in = CertServer.class.getResourceAsStream("CertServer.js")) {
			new StreamSource(in, new SinkOctets(data)).flush();
		}
		StaticWebData js = new StaticWebData(data.getBytes(), "application/x-javascript; charset=utf-8");
		server.createContext("/CertServer.js", exchange -> js.transfer(exchange));
		server.setExecutor(null);
		server.start();
		if (Trace.isInfoEnabled())
			Trace.info("CertServer start on " + addr);
	}
}
