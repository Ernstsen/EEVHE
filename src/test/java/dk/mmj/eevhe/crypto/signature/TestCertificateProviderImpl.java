package dk.mmj.eevhe.crypto.signature;

import dk.mmj.eevhe.TestUsingBouncyCastle;
import dk.mmj.eevhe.entities.CertificateDTO;
import dk.mmj.eevhe.entities.SignedEntity;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

public class TestCertificateProviderImpl extends TestUsingBouncyCastle {

    @Test
    public void testCertMapGeneration() throws IOException, NoSuchProviderException, NoSuchAlgorithmException, OperatorCreationException {
        AsymmetricKeyParameter sk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
        AsymmetricKeyParameter electionPk = CertificateHelper.getPublicKeyFromCertificate(Paths.get("certs/test_glob.pem"));

        ArrayList<SignedEntity<CertificateDTO>> certs = new ArrayList<>();


        //Valid
        CertRes cert1 = generateCert(sk, 1);
        certs.add(new SignedEntity<>(new CertificateDTO(CertificateHelper.certificateToPem(cert1.cert), 1),
                PrivateKeyFactory.createKey(cert1.keyPair.getPrivate().getEncoded())));

        //Invalid: Lies about identity
        certs.add(new SignedEntity<>(new CertificateDTO(CertificateHelper.certificateToPem(cert1.cert), 2),
                PrivateKeyFactory.createKey(cert1.keyPair.getPrivate().getEncoded())));

        //Invalid: Not signed by election certificate - DAs are not allowed to spawn new certificates/identities
        CertRes cert3 = generateCert(PrivateKeyFactory.createKey(cert1.keyPair.getPrivate().getEncoded()), 3);
        certs.add(new SignedEntity<>(new CertificateDTO(CertificateHelper.certificateToPem(cert3.cert), 3),
                PrivateKeyFactory.createKey(cert3.keyPair.getPrivate().getEncoded())));

        //Invalid: Valid certificate for id=4 is sent and signed by id=3
        CertRes cert4 = generateCert(sk, 4);
        certs.add(new SignedEntity<>(new CertificateDTO(CertificateHelper.certificateToPem(cert4.cert), 4),
                PrivateKeyFactory.createKey(cert3.keyPair.getPrivate().getEncoded())));

        //Valid
        CertRes cert5 = generateCert(sk, 5);
        certs.add(new SignedEntity<>(new CertificateDTO(CertificateHelper.certificateToPem(cert5.cert), 5),
                PrivateKeyFactory.createKey(cert5.keyPair.getPrivate().getEncoded())));

        //Invalid: Certificate is corrupt
        CertRes cert6 = generateCert(sk, 6);
        certs.add(new SignedEntity<>(new CertificateDTO(CertificateHelper.certificateToPem(cert6.cert).substring(39), 6),
                PrivateKeyFactory.createKey(cert6.keyPair.getPrivate().getEncoded())));


        CertificateProviderImpl provider = new CertificateProviderImpl(() -> certs, electionPk);

        Map<Integer, String> res = provider.generateCertMap();

        assertEquals("Result should have exactly two entries", 2, res.size());
        assertNotNull("DA 1 should not be disqualified", res.get(1));
        assertNotNull("DA 5 should not be disqualified", res.get(5));
        assertEquals("Incorrect certificate for da 1", CertificateHelper.certificateToPem(cert1.cert), res.get(1));
        assertEquals("Incorrect certificate for da 5", CertificateHelper.certificateToPem(cert5.cert), res.get(5));

        assertSame("Cache did not take effect", res, provider.generateCertMap());
    }

    @Test
    public void ioExceptionPropagated() {
        CertificateProviderImpl provider = new CertificateProviderImpl(() -> {
            throw new IOException("Expected");
        }, null);

        assertThrows("Should throw RuntimeException", RuntimeException.class, provider::generateCertMap);
    }

    private CertRes generateCert(AsymmetricKeyParameter sk, int id)
            throws NoSuchProviderException, NoSuchAlgorithmException, OperatorCreationException {
        AlgorithmIdentifier sha256WithRSASignature = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA");
        AlgorithmIdentifier digestSha = new DefaultDigestAlgorithmIdentifierFinder().find("SHA-256");
        java.security.KeyPair keyPair = KeyHelper.generateRSAKeyPair();
        X509v3CertificateBuilder cb = new X509v3CertificateBuilder(
                new X500Name("CN=EEVHE_TESTSUITE"),
                BigInteger.valueOf(id),
                new Date(), new Date(System.currentTimeMillis() + (60 * 1000)),
                new X500Name("CN=DA" + id),
                new SubjectPublicKeyInfo(sha256WithRSASignature, keyPair.getPublic().getEncoded())
        );

        ContentSigner signer = new BcRSAContentSignerBuilder(
                sha256WithRSASignature,
                digestSha
        ).build(sk);

        X509CertificateHolder build = cb.build(signer);
        return new CertRes(build, keyPair);
    }

    private static class CertRes {
        X509CertificateHolder cert;
        KeyPair keyPair;

        public CertRes(X509CertificateHolder cert, KeyPair keyPair) {
            this.cert = cert;
            this.keyPair = keyPair;
        }
    }
}
