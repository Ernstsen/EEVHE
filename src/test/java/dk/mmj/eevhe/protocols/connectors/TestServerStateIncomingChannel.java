package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.TestUsingBouncyCastle;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.server.ServerState;
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
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.stream.Collectors;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.assertEquals;

public class TestServerStateIncomingChannel extends TestUsingBouncyCastle {
    private IncomingChannel channel;
    private ArrayList<SignedEntity<PartialSecretMessageDTO>> expected;

    @Before
    public void setUp() throws NoSuchProviderException, NoSuchAlgorithmException, IOException, OperatorCreationException {
        AsymmetricKeyParameter sk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
        AlgorithmIdentifier sha256WithRSASignature = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA");
        AlgorithmIdentifier digestSha = new DefaultDigestAlgorithmIdentifierFinder().find("SHA-256");

        java.security.KeyPair keyPair = KeyHelper.generateRSAKeyPair();
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
        ).build(sk);

        X509CertificateHolder daOneCert = cb.build(signer);
        AsymmetricKeyParameter daOneSk = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());

        ArrayList<String> ids = new ArrayList<>();
        ids.add("1");
        ids.add("2");

        SignedEntity<PartialSecretMessageDTO> s1 = new SignedEntity<>(new PartialSecretMessageDTO(valueOf(15616), valueOf(1414), 57, 1), daOneSk);
        SignedEntity<PartialSecretMessageDTO> s2 = new SignedEntity<>(new PartialSecretMessageDTO(valueOf(1566), valueOf(141), 257, 1), daOneSk);
        SignedEntity<PartialSecretMessageDTO> s3 = new SignedEntity<>(new PartialSecretMessageDTO(valueOf(156), valueOf(14), 25, 1), daOneSk);

        final ServerState inst = ServerState.getInstance();
        inst.put("1", s1);
        inst.put("2", s2);
        inst.put("3", s3);

        expected = new ArrayList<>();
        expected.add(s1);
        expected.add(s2);

        HashMap<Integer, String> certMap = new HashMap<>();
        certMap.put(1, CertificateHelper.certificateToPem(daOneCert));

        channel = new ServerStateIncomingChannel(ids, () -> certMap);
    }

    @Test
    public void receiveSecrets() {
        final List<PartialSecretMessageDTO> received = channel.receiveSecrets();
        assertEquals("Unexpected list of received secrets",
                expected.stream().map(SignedEntity::getEntity).collect(Collectors.toList()),
                received
        );
    }
}
