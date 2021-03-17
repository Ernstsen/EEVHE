package dk.mmj.eevhe.crypto.signature;

import dk.mmj.eevhe.TestUsingBouncyCastle;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestCertificateSuite extends TestUsingBouncyCastle {

    @Test
    public void testLoadSignAndVerify() throws IOException {
        AsymmetricKeyParameter pk = CertificateHelper.getPublicKeyFromCertificate(Paths.get("certs/test_glob.pem"));
        AsymmetricKeyParameter sk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));

        List<byte[]> msgParts = Arrays.asList(
                "firstMessagePart".getBytes(StandardCharsets.UTF_8),
                "second part".getBytes(StandardCharsets.UTF_8),
                "third and last part".getBytes(StandardCharsets.UTF_8)
        );

        String signature = SignatureHelper.sign(sk, msgParts);

        assertTrue("Should verify message signature", SignatureHelper.verifySignature(pk, msgParts, signature));
    }


    @Test
    public void testLoadRewriteSignAndVerify() throws IOException, NoSuchProviderException, NoSuchAlgorithmException, OperatorCreationException {
        AsymmetricKeyParameter parentSk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));

        AlgorithmIdentifier sha256WithRSASignature = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA");
        AlgorithmIdentifier digestSha = new DefaultDigestAlgorithmIdentifierFinder().find("SHA-256");

        KeyPair keyPair = KeyHelper.generateRSAKeyPair();
        X509v3CertificateBuilder cb = new X509v3CertificateBuilder(
                new X500Name("CN=EEVHE_TESTSUITE"),
                BigInteger.valueOf(1),
                new Date(), new Date(System.currentTimeMillis() + (60 * 1000)),
                new X500Name("CN=DA" + 1),
                new SubjectPublicKeyInfo(sha256WithRSASignature, keyPair.getPublic().getEncoded())
        );

        ContentSigner signer = new BcRSAContentSignerBuilder(
                sha256WithRSASignature,
                digestSha
        ).build(parentSk);

        X509CertificateHolder cert = cb.build(signer);
        AsymmetricKeyParameter sk = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());

        ByteArrayOutputStream ous = new ByteArrayOutputStream();
        CertificateHelper.writeCertificate(ous, cert);
        byte[] serialized = ous.toByteArray();

        AsymmetricKeyParameter pk = CertificateHelper.getPublicKeyFromCertificate(serialized);


        List<byte[]> msgParts = Arrays.asList(
                "firstMessagePart".getBytes(StandardCharsets.UTF_8),
                "second part".getBytes(StandardCharsets.UTF_8),
                "third and last part".getBytes(StandardCharsets.UTF_8)
        );

        String signature = SignatureHelper.sign(sk, msgParts);

        assertTrue("Should verify message signature", SignatureHelper.verifySignature(pk, msgParts, signature));
    }

    @Test
    public void generateKeyPairWorks() throws NoSuchProviderException, NoSuchAlgorithmException, IOException {
        KeyPair keyPair = KeyHelper.generateRSAKeyPair();
        AsymmetricKeyParameter sk = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        AsymmetricKeyParameter pk = PublicKeyFactory.createKey(keyPair.getPublic().getEncoded());

        PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());

        List<byte[]> msgParts = Arrays.asList(
                "firstMessagePart".getBytes(StandardCharsets.UTF_8),
                "second part".getBytes(StandardCharsets.UTF_8),
                "third and last part".getBytes(StandardCharsets.UTF_8)
        );

        String signature = SignatureHelper.sign(sk, msgParts);

        assertTrue("Should verify message signature", SignatureHelper.verifySignature(pk, msgParts, signature));
    }

    @Test
    public void writtenKeyIsUsable() throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        KeyPair keyPair = KeyHelper.generateRSAKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        ByteArrayOutputStream ous = new ByteArrayOutputStream();
        KeyHelper.writeKey(ous, privateKey.getEncoded());

        AsymmetricKeyParameter readKey = KeyHelper.readKey(ous.toByteArray());

        List<byte[]> msgParts = Arrays.asList(
                "firstMessagePart".getBytes(StandardCharsets.UTF_8),
                "second part".getBytes(StandardCharsets.UTF_8),
                "third and last part".getBytes(StandardCharsets.UTF_8)
        );

        String signature = SignatureHelper.sign(readKey, msgParts);

        AsymmetricKeyParameter pk = PublicKeyFactory.createKey(keyPair.getPublic().getEncoded());
        assertTrue("Should verify message signature", SignatureHelper.verifySignature(pk, msgParts, signature));
    }

}
