package limax.pkix;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.function.Function;

import limax.util.Pair;
import limax.util.SecurityUtils;
import limax.util.SecurityUtils.PublicKeyAlgorithm;

enum KeyContainer {
	FILE {
		@Override
		KeyInfo load(KeyUpdater keyUpdater, String describe, Function<String, char[]> passphraseCallback)
				throws UnrecoverableKeyException, Exception {
			int pos = describe.indexOf('@');
			String alias = describe.substring(0, pos);
			Path path = Paths.get(describe.substring(pos + 1));
			Path pathKey = path.resolve(alias + ".key");
			Path pathP7b = path.resolve(alias + ".p7b");
			if (!Files.exists(pathKey))
				throw new KeyStoreException("key file absent " + pathKey);
			if (!Files.exists(pathP7b))
				throw new KeyStoreException("p7b file absent " + pathP7b);
			try {
				keyUpdater.recover((b0, b1) -> sync(path, alias, null, null, b0, b1));
			} catch (Exception e) {
			}
			PrivateKey privateKey;
			Certificate[] chain;
			try (InputStream in = Files.newInputStream(pathP7b)) {
				chain = CertificateFactory.getInstance("X.509").generateCertificates(in).toArray(new Certificate[0]);
			}
			char[] passphrase = passphraseCallback.apply("KeyFile [" + pathKey + "] Password:");
			try {
				privateKey = PublicKeyAlgorithm.loadPrivateKey(new String(Files.readAllBytes(pathKey)), passphrase);
			} catch (Exception e1) {
				throw new UnrecoverableKeyException("KeyFile [" + pathKey + "] wrong password.");
			}
			return new KeyInfo(this, keyUpdater, alias, path, passphrase, privateKey, chain);
		}

		@Override
		KeyInfo save(KeyUpdater keyUpdater, String describe, PrivateKey privateKey, Certificate[] chain,
				Function<String, char[]> passphraseCallback) throws Exception {
			int pos = describe.indexOf('@');
			String alias = describe.substring(0, pos);
			Path path = Paths.get(describe.substring(pos + 1));
			Path pathKey = path.resolve(alias + ".key");
			char[] passphrase = passphraseCallback.apply("KeyFile [" + pathKey + "] Password:");
			if (!Arrays.equals(passphrase, passphraseCallback.apply("Confirm KeyFile [" + pathKey + "] Password:")))
				throw new RuntimeException("Differ Password Input.");
			Files.createDirectories(path);
			KeyInfo keyInfo = new KeyInfo(this, keyUpdater, alias, path, passphrase, privateKey, chain);
			keyInfo.setKeyEntry(privateKey, chain);
			return keyInfo;
		}

		@Override
		void sync(Path path, String alias, KeyStore keyStore, char[] passphrase, byte[] privateKeyBytes,
				byte[] certificateChainBytes) throws Exception {
			Files.write(path.resolve(alias + ".key"), privateKeyBytes, options);
			Files.write(path.resolve(alias + ".p7b"), certificateChainBytes, options);
		}
	},
	PKCS11 {
		@Override
		KeyInfo load(KeyUpdater keyUpdater, String describe, Function<String, char[]> passphraseCallback)
				throws UnrecoverableKeyException, Exception {
			int pos = describe.indexOf('@');
			String alias = describe.substring(0, pos);
			Path path = Paths.get(describe.substring(pos + 1));
			Pair<char[], KeyStore> pair = SecurityUtils.loadPKCS11KeyStore(path, passphraseCallback);
			char[] passphrase = pair.getKey();
			KeyStore keyStore = pair.getValue();
			try {
				keyUpdater.recover(passphrase,
						(privateKey, chain) -> setKeyEntry(keyStore, alias, privateKey, passphrase, chain));
			} catch (Exception e) {
			}
			if (keyStore.getKey(alias, null) == null)
				throw new KeyStoreException("alias absent " + alias);
			return new KeyInfo(this, keyUpdater, alias, null, passphrase, keyStore);
		}

		@Override
		KeyInfo save(KeyUpdater keyUpdater, String describe, PrivateKey privateKey, Certificate[] chain,
				Function<String, char[]> passphraseCallback) throws Exception {
			int pos = describe.indexOf('@');
			String alias = describe.substring(0, pos);
			Path path = Paths.get(describe.substring(pos + 1));
			Pair<char[], KeyStore> pair = SecurityUtils.loadPKCS11KeyStore(path, passphraseCallback);
			KeyInfo keyInfo = new KeyInfo(this, keyUpdater, alias, null, pair.getKey(), pair.getValue());
			keyInfo.setKeyEntry(privateKey, chain);
			return keyInfo;
		}

		@Override
		void sync(Path path, String alias, KeyStore keyStore, char[] passphrase, byte[] privateKeyBytes,
				byte[] certificateChainBytes) throws Exception {
		}
	},
	PKCS12, JKS, JCEKS;
	private static final OpenOption[] options = new OpenOption[] { StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.SYNC };

	KeyInfo load(KeyUpdater keyUpdater, String describe, Function<String, char[]> passphraseCallback)
			throws UnrecoverableKeyException, Exception {
		int pos = describe.indexOf('@');
		Path path = Paths.get(describe.substring(pos + 1));
		char[] passphrase = passphraseCallback.apply(name() + " [" + path + "] Password:");
		KeyStore keyStore = KeyStore.getInstance(name());
		try (InputStream in = Files.newInputStream(path)) {
			keyStore.load(in, passphrase);
		}
		String alias = pos == -1 ? findKeyAlias(keyStore) : describe.substring(0, pos);
		try {
			keyUpdater.recover(passphrase, (privateKey, chain) -> {
				setKeyEntry(keyStore, alias, privateKey, passphrase, chain);
				sync(path, null, keyStore, passphrase, null, null);
			});
		} catch (Exception e) {
		}
		if (keyStore.getKey(alias, passphrase) == null)
			throw new KeyStoreException("alias absent " + alias);
		return new KeyInfo(this, keyUpdater, alias, path, passphrase, keyStore);
	}

	KeyInfo save(KeyUpdater keyUpdater, String describe, PrivateKey privateKey, Certificate[] chain,
			Function<String, char[]> passphraseCallback) throws Exception {
		int pos = describe.indexOf('@');
		Path path = Paths.get(describe.substring(pos + 1));
		KeyStore keyStore = KeyStore.getInstance(name());
		char[] passphrase = passphraseCallback.apply(name() + " [" + path + "] Password:");
		String alias;
		if (Files.exists(path)) {
			try (InputStream in = Files.newInputStream(path)) {
				keyStore.load(in, passphrase);
			}
			alias = pos > -1 ? describe.substring(0, pos) : findKeyAlias(keyStore);
		} else {
			if (!Arrays.equals(passphrase, passphraseCallback.apply("Confirm " + name() + " [" + path + "] Password:")))
				throw new RuntimeException("Differ Password Input.");
			keyStore.load(null, null);
			Files.createDirectories(path.toAbsolutePath().getParent());
			alias = pos > -1 ? describe.substring(0, pos) : "";
		}
		KeyInfo keyInfo = new KeyInfo(this, keyUpdater, alias, path, passphrase, keyStore);
		keyInfo.setKeyEntry(privateKey, chain);
		return keyInfo;
	}

	void sync(Path path, String alias, KeyStore keyStore, char[] passphrase, byte[] privateKeyBytes,
			byte[] certificateChainBytes) throws Exception {
		try (OutputStream out = Files.newOutputStream(path, options)) {
			keyStore.store(out, passphrase);
		}
	}

	private static String findKeyAlias(KeyStore keyStore) throws KeyStoreException {
		for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements();) {
			String alias = e.nextElement();
			if (keyStore.isKeyEntry(alias))
				return alias;
		}
		return null;
	}

	static void setKeyEntry(KeyStore keyStore, String alias, PrivateKey privateKey, char[] passphrase,
			Certificate[] chain) throws Exception {
		keyStore.setKeyEntry(alias, privateKey, passphrase, SecurityUtils.sortCertificateChain(chain));
	}

	static KeyContainer valueOf(URI location) {
		return KeyContainer.valueOf(location.getScheme().toUpperCase());
	}
}
