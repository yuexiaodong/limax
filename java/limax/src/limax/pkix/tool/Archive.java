package limax.pkix.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;

class Archive {
	private final Path path;

	Archive(String path) throws IOException {
		this.path = Paths.get(path);
		Files.createDirectories(this.path);
	}

	void store(X509Certificate cert) throws Exception {
		Files.write(path.resolve(cert.getSerialNumber().toString(16) + ".cer"), cert.getEncoded());
	}
}
