package dk.mmj.eevhe.crypto.signature;

import dk.mmj.eevhe.entities.CertificateDTO;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.interfaces.CertificateFetcher;
import dk.mmj.eevhe.interfaces.CertificateProvider;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentVerifierProviderBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class CertificateProviderImpl implements CertificateProvider {

    private final CertificateFetcher fetcher;
    private final AsymmetricKeyParameter electionPk;
    private Map<Integer, String> cache;

    /**
     * Constructor for the CertificateProvider
     *
     * @param fetcher    fetcher for the certificates
     * @param electionPk the publicKey for the election
     */
    public CertificateProviderImpl(CertificateFetcher fetcher, AsymmetricKeyParameter electionPk) {
        this.fetcher = fetcher;
        this.electionPk = electionPk;
    }

    /**
     * @return map between daIds and the map as a .pem formatted string
     */
    @Override
    public Map<Integer, String> generateCertMap() {
        if (cache != null) {
            return cache;
        }
        try {
            return cache = fetcher.getCertificates().stream()
                    .filter(se -> verifyCert(se, electionPk))
                    .map(SignedEntity::getEntity)
                    .collect(Collectors.toMap(CertificateDTO::getId, CertificateDTO::getCert));
        } catch (IOException exception) {
            throw new RuntimeException("Failed to fetch certificates", exception);
        }
    }

    /**
     * @param e self-signed certificate to be verified
     * @return whether the certificate is valid
     */
    private boolean verifyCert(SignedEntity<CertificateDTO> e, AsymmetricKeyParameter electionPk) {
        try {
            byte[] cert = e.getEntity().getCert().getBytes(StandardCharsets.UTF_8);
            X509CertificateHolder certificate = CertificateHelper.readCertificate(cert);

            ContentVerifierProvider verifier = new BcRSAContentVerifierProviderBuilder(new DefaultDigestAlgorithmIdentifierFinder())
                    .build(electionPk);

            boolean signedByCA = certificate.isSignatureValid(verifier);
            boolean selfSigned = e.verifySignature(CertificateHelper.getPublicKeyFromCertificate(cert));
            boolean notImposter = certificate.getSubject().equals(new X500Name("CN=DA" + e.getEntity().getId()));
            return selfSigned && signedByCA && notImposter;
        } catch (Exception exception) {
            return false;
        }
    }
}
