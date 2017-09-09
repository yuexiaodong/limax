package limax.pkix;

import java.net.Socket;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

public class KeyInfo {
	private final KeyContainer keyContainer;
	private final KeyUpdater keyUpdater;
	private final String alias;
	private final Path path;
	private final char[] passphrase;
	private final KeyStore keyStore;

	KeyInfo(KeyContainer keyContainer, KeyUpdater keyUpdater, String alias, Path path, char[] passphrase,
			PrivateKey privateKey, Certificate[] chain) throws Exception {
		this.keyContainer = keyContainer;
		this.keyUpdater = keyUpdater;
		this.alias = alias;
		this.path = path;
		this.passphrase = passphrase;
		this.keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		KeyContainer.setKeyEntry(keyStore, alias, privateKey, passphrase, chain);
	}

	KeyInfo(KeyContainer keyContainer, KeyUpdater keyUpdater, String alias, Path path, char[] passphrase,
			KeyStore keyStore) throws UnrecoverableKeyException, Exception {
		this.keyContainer = keyContainer;
		this.keyUpdater = keyUpdater;
		this.alias = alias;
		this.path = path;
		this.passphrase = passphrase;
		this.keyStore = keyStore;
	}

	public PrivateKey getPrivateKey() throws Exception {
		return (PrivateKey) keyStore.getKey(alias, passphrase);
	}

	public Certificate[] getCertificateChain() throws KeyStoreException {
		return keyStore.getCertificateChain(alias);
	}

	public SSLContext createSSLContext(Set<TrustAnchor> trustAnchors, X509CertSelector selector,
			Set<PKIXRevocationChecker.Option> options) throws Exception {
		PKIXBuilderParameters pkixBuilderParameters = new PKIXBuilderParameters(trustAnchors,
				selector == null ? new X509CertSelector() : selector);
		if (options != null) {
			PKIXRevocationChecker pKIXRevocationChecker = (PKIXRevocationChecker) CertPathValidator.getInstance("PKIX")
					.getRevocationChecker();
			pKIXRevocationChecker.setOptions(options);
			pkixBuilderParameters.addCertPathChecker(pKIXRevocationChecker);
		} else {
			pkixBuilderParameters.setRevocationEnabled(false);
		}
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
		trustManagerFactory.init(new CertPathTrustManagerParameters(pkixBuilderParameters));
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(new KeyManager[] { new X509ExtendedKeyManager() {
			@Override
			public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
				return alias;
			}

			@Override
			public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
				return alias;
			}

			@Override
			public X509Certificate[] getCertificateChain(String alias) {
				try {
					Certificate[] chain = KeyInfo.this.getCertificateChain();
					return Arrays.stream(chain).limit(chain.length - 1).toArray(X509Certificate[]::new);
				} catch (KeyStoreException e) {
					return null;
				}
			}

			@Override
			public String[] getClientAliases(String keyType, Principal[] issuers) {
				return new String[] { alias };
			}

			@Override
			public PrivateKey getPrivateKey(String alias) {
				try {
					return KeyInfo.this.getPrivateKey();
				} catch (Exception e) {
					return null;
				}
			}

			@Override
			public String[] getServerAliases(String keyType, Principal[] issuers) {
				return null;
			}
		} }, trustManagerFactory.getTrustManagers(), null);
		return sslContext;
	}

	public void setKeyEntry(PrivateKey privateKey, Certificate[] chain) throws Exception {
		keyUpdater.save(privateKey, chain, passphrase, (privateKeyBytes, certificateChainBytes) -> {
			KeyContainer.setKeyEntry(keyStore, alias, privateKey, passphrase, chain);
			keyContainer.sync(path, alias, keyStore, passphrase, privateKeyBytes, certificateChainBytes);
		});
	}

	public static KeyInfo load(URI location, Function<String, char[]> passphraseCallback)
			throws UnrecoverableKeyException, Exception {
		return KeyContainer.valueOf(location).load(new KeyUpdater(location), location.getSchemeSpecificPart(),
				passphraseCallback);
	}

	public static KeyInfo save(URI location, PrivateKey privateKey, Certificate[] chain,
			Function<String, char[]> passphraseCallback) throws Exception {
		return KeyContainer.valueOf(location).save(new KeyUpdater(location), location.getSchemeSpecificPart(),
				privateKey, chain, passphraseCallback);
	}
}