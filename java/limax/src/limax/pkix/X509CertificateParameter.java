package limax.pkix;

import java.net.URI;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.function.Function;

public interface X509CertificateParameter extends X509CertificateBaseParameter {
	URI getOcspURI();

	Function<X509Certificate, URI> getCRLDPMapping();

	PublicKey getPublicKey();
}
