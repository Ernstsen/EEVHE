package dk.mmj.eevhe.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.TestUsingBouncyCastle;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.TestUtils;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.signature.SignatureHelper;
import dk.mmj.eevhe.entities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class TestFetchingUtilities extends TestUsingBouncyCastle {
    private static final Logger logger = LogManager.getLogger(TestFetchingUtilities.class);
    private X509CertificateHolder daOneCert;
    private AsymmetricKeyParameter daOneSk;
    private X509CertificateHolder electionCert;

    @Before
    public void setUp() throws Exception {
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

        daOneCert = cb.build(signer);
        daOneSk = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        electionCert = CertificateHelper.readCertificate(Files.readAllBytes(Paths.get("certs/test_glob.pem")));

    }

    @Test
    public void testGetSignaturePublicKey() throws IOException {

        AsymmetricKeyParameter signaturePublicKey = FetchingUtilities.getSignaturePublicKey(
                new PartialPublicInfo(1, null, null, null, -1, CertificateHelper.certificateToPem(daOneCert)),
                CertificateHelper.getPublicKeyFromCertificate(electionCert),
                logger
        );

        List<byte[]> msg = Collections.singletonList("This is a message".getBytes(StandardCharsets.UTF_8));

        String sign = SignatureHelper.sign(daOneSk, msg);

        assertTrue("Signature should be verified", SignatureHelper.verifySignature(signaturePublicKey, msg, sign));
    }

    @Test
    public void wontReturnKeyWhenNotOwnCertificate() throws IOException {

        AsymmetricKeyParameter signaturePublicKey = FetchingUtilities.getSignaturePublicKey(
                new PartialPublicInfo(2, null, null, null, -1, CertificateHelper.certificateToPem(daOneCert)),
                CertificateHelper.getPublicKeyFromCertificate(electionCert),
                logger
        );

        assertNull("No pk should have been returned", signaturePublicKey);
    }

    @Test
    public void wontReturnKeyWhenCertBroken() throws IOException {

        AsymmetricKeyParameter signaturePublicKey = FetchingUtilities.getSignaturePublicKey(
                new PartialPublicInfo(2, null, null, null, -1, CertificateHelper.certificateToPem(daOneCert) + "="),
                CertificateHelper.getPublicKeyFromCertificate(electionCert),
                logger
        );

        assertNull("No pk should have been returned", signaturePublicKey);
    }

    @Test
    public void fetchBallots() throws JsonProcessingException {
        KeyPair keyPair = TestUtils.generateKeysFromP2048bitsG2();

        List<PersistedBallot> ballots = Arrays.asList(
                new PersistedBallot(SecurityUtils.generateBallot(1, 5, "1", keyPair.getPublicKey())),
                new PersistedBallot(SecurityUtils.generateBallot(2, 5, "1", keyPair.getPublicKey()))
        );
        ObjectMapper mapper = new ObjectMapper();


        WebTarget bulletinBoard = mock(WebTarget.class);

        WebTarget ballotsTarget = mock(WebTarget.class);
        when(bulletinBoard.path("getBallots")).thenReturn(ballotsTarget);

        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(ballotsTarget.request()).thenReturn(commitBuilder);
        String ballotsString = mapper.writeValueAsString(ballots.toArray(new PersistedBallot[0]));
        when(commitBuilder.get(String.class)).thenReturn(ballotsString);

        List<PersistedBallot> fetched = FetchingUtilities.getBallots(logger, bulletinBoard);

        assertEquals("Fetched ballots did not match expected", ballots, fetched);
    }

    @Test
    public void fetchBallotsWithError() throws JsonProcessingException {
        KeyPair keyPair = TestUtils.generateKeysFromP2048bitsG2();

        List<PersistedBallot> ballots = Arrays.asList(
                new PersistedBallot(SecurityUtils.generateBallot(1, 5, "1", keyPair.getPublicKey())),
                new PersistedBallot(SecurityUtils.generateBallot(2, 5, "1", keyPair.getPublicKey()))
        );

        ObjectMapper mapper = new ObjectMapper();
        WebTarget bulletinBoard = mock(WebTarget.class);

        WebTarget ballotsTarget = mock(WebTarget.class);
        when(bulletinBoard.path("getBallots")).thenReturn(ballotsTarget);

        final Invocation.Builder invocationBuilder = mock(Invocation.Builder.class);
        when(ballotsTarget.request()).thenReturn(invocationBuilder);
        when(invocationBuilder.get(String.class)).thenReturn(mapper.writeValueAsString(ballots));

        List<PersistedBallot> fetched = FetchingUtilities.getBallots(logger, bulletinBoard);

        assertNull("Should return null", fetched);
    }

    @Test
    public void testFetchPublicInfos() throws IOException {
        KeyPair keyPair = TestUtils.generateKeysFromP2048bitsG2();

        ObjectMapper mapper = new ObjectMapper();

        List<Candidate> candidates = Arrays.asList(
                new Candidate(0, "Mette", "A"),
                new Candidate(1, "Lars", "V")
        );

        SignedEntity<PartialPublicInfo> validInfo = new SignedEntity<>(new PartialPublicInfo(1, keyPair.getPublicKey(), keyPair.getSecretKey(), candidates,
                System.currentTimeMillis(), CertificateHelper.certificateToPem(daOneCert)), daOneSk);
        SignedEntity<PartialPublicInfo> invalidInfo = new SignedEntity<>(new PartialPublicInfo(2, keyPair.getPublicKey(), keyPair.getSecretKey(), candidates,
                System.currentTimeMillis(), CertificateHelper.certificateToPem(daOneCert)), daOneSk);
        SignedEntity<PartialPublicInfo>[] infos = new SignedEntity[]{validInfo, invalidInfo};

        WebTarget bulletinBoard = mock(WebTarget.class);
        WebTarget publicInfoTarget = mock(WebTarget.class);

        when(bulletinBoard.path("publicInfo")).thenReturn(publicInfoTarget);
        final Invocation.Builder invocationBuilder = mock(Invocation.Builder.class);
        when(publicInfoTarget.request()).thenReturn(invocationBuilder);
        when(invocationBuilder.get(String.class)).thenReturn(mapper.writeValueAsString(infos));

        List<PartialPublicInfo> publicInfos = FetchingUtilities.getPublicInfos(logger, bulletinBoard,
                CertificateHelper.getPublicKeyFromCertificate(electionCert));

        assertNotNull("Should have fetched public infos", publicInfos);
        assertEquals("Only one info should be returned", 1, publicInfos.size());
        assertEquals("Wrong info was included", validInfo.getEntity(), publicInfos.get(0));
    }

    @Test
    public void testFetchPublicInfosError() throws IOException {
        KeyPair keyPair = TestUtils.generateKeysFromP2048bitsG2();

        ObjectMapper mapper = new ObjectMapper();

        List<Candidate> candidates = Arrays.asList(
                new Candidate(0, "Mette", "A"),
                new Candidate(1, "Lars", "V")
        );

        ArrayList<SignedEntity<PartialPublicInfo>> infos = new ArrayList<>();
        infos.add(new SignedEntity<>(new PartialPublicInfo(1, keyPair.getPublicKey(), keyPair.getSecretKey(), candidates,
                System.currentTimeMillis(), CertificateHelper.certificateToPem(daOneCert)), daOneSk));
        infos.add(new SignedEntity<>(new PartialPublicInfo(2, keyPair.getPublicKey(), keyPair.getSecretKey(), candidates,
                System.currentTimeMillis(), CertificateHelper.certificateToPem(daOneCert)), daOneSk));

        WebTarget bulletinBoard = mock(WebTarget.class);
        WebTarget publicInfoTarget = mock(WebTarget.class);
        when(bulletinBoard.path("publicInfo")).thenReturn(publicInfoTarget);
        final Invocation.Builder invocationBuilder = mock(Invocation.Builder.class);
        when(publicInfoTarget.request()).thenReturn(invocationBuilder);
        when(invocationBuilder.get(String.class)).thenReturn(mapper.writeValueAsString(infos).substring(0, 68));

        List<PartialPublicInfo> publicInfos = FetchingUtilities.getPublicInfos(logger, bulletinBoard,
                CertificateHelper.getPublicKeyFromCertificate(electionCert));

        assertNull("Should not be able to fetch infos", publicInfos);
    }
}
