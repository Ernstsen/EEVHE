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
import dk.mmj.eevhe.entities.wrappers.PublicInfoWrapper;
import dk.mmj.eevhe.entities.wrappers.StringListWrapper;
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
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestFetchingUtilities extends TestUsingBouncyCastle {
    private static final Logger logger = LogManager.getLogger(TestFetchingUtilities.class);
    private X509CertificateHolder daOneCert;
    private AsymmetricKeyParameter daOneSk;
    private X509CertificateHolder electionCert;
    private X509CertificateHolder bbOneCert;
    private AsymmetricKeyParameter bbOneSk;
    private X509CertificateHolder bbTwoCert;
    private AsymmetricKeyParameter bbTwoSk;
    private X509CertificateHolder bbThreeCert;
    private AsymmetricKeyParameter bbThreeSk;

    @Before
    public void setUp() throws Exception {
        AsymmetricKeyParameter electionSk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));

        AlgorithmIdentifier sha256WithRSASignature = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA");
        AlgorithmIdentifier digestSha = new DefaultDigestAlgorithmIdentifierFinder().find("SHA-256");

        electionCert = CertificateHelper.readCertificate(Files.readAllBytes(Paths.get("certs/test_glob.pem")));

        {
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
            ).build(electionSk);

            daOneCert = cb.build(signer);
            daOneSk = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        }
        {
            java.security.KeyPair keyPair = KeyHelper.generateRSAKeyPair();
            X509v3CertificateBuilder cb = new X509v3CertificateBuilder(
                    new X500Name("CN=EEVHE_TESTSUITE"),
                    BigInteger.valueOf(1),
                    new Date(), new Date(System.currentTimeMillis() + (60 * 1000)),
                    new X500Name("CN=BB_PEER" + 1),
                    new SubjectPublicKeyInfo(sha256WithRSASignature, keyPair.getPublic().getEncoded())
            );

            ContentSigner signer = new BcRSAContentSignerBuilder(
                    sha256WithRSASignature,
                    digestSha
            ).build(electionSk);
            bbOneCert = cb.build(signer);
            bbOneSk = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        }
        {
            java.security.KeyPair keyPair = KeyHelper.generateRSAKeyPair();
            X509v3CertificateBuilder cb = new X509v3CertificateBuilder(
                    new X500Name("CN=EEVHE_TESTSUITE"),
                    BigInteger.valueOf(1),
                    new Date(), new Date(System.currentTimeMillis() + (60 * 1000)),
                    new X500Name("CN=BB_PEER" + 2),
                    new SubjectPublicKeyInfo(sha256WithRSASignature, keyPair.getPublic().getEncoded())
            );

            ContentSigner signer = new BcRSAContentSignerBuilder(
                    sha256WithRSASignature,
                    digestSha
            ).build(electionSk);
            bbTwoCert = cb.build(signer);
            bbTwoSk = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        }
        {
            java.security.KeyPair keyPair = KeyHelper.generateRSAKeyPair();
            X509v3CertificateBuilder cb = new X509v3CertificateBuilder(
                    new X500Name("CN=EEVHE_TESTSUITE"),
                    BigInteger.valueOf(1),
                    new Date(), new Date(System.currentTimeMillis() + (60 * 1000)),
                    new X500Name("CN=BB_BB_PEER" + 3),
                    new SubjectPublicKeyInfo(sha256WithRSASignature, keyPair.getPublic().getEncoded())
            );

            ContentSigner signer = new BcRSAContentSignerBuilder(
                    sha256WithRSASignature,
                    digestSha
            ).build(electionSk);
            bbThreeCert = cb.build(signer);
            bbThreeSk = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        }
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

//        List<PersistedBallot> fetched = FetchingUtilities.getBallots(logger, bulletinBoard, );
//
//        assertEquals("Fetched ballots did not match expected", ballots, fetched);
        fail("Temporarily disabled");//TODO!
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

//        List<PersistedBallot> fetched = FetchingUtilities.getBallots(logger, bulletinBoard);
        //TODO!
//        assertNull("Should return null", fetched);
        fail("Temporarily disabled");
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


        List<SignedEntity<PublicInfoWrapper>> infos = Arrays.asList(
                new SignedEntity<>(new PublicInfoWrapper(Collections.singletonList(validInfo)), bbOneSk),
                new SignedEntity<>(new PublicInfoWrapper(Collections.singletonList(validInfo)), bbTwoSk),
                new SignedEntity<>(new PublicInfoWrapper(Collections.singletonList(invalidInfo)), bbThreeSk),
                new SignedEntity<>(new PublicInfoWrapper(Collections.singletonList(invalidInfo)), bbThreeSk)
        );

        WebTarget bulletinBoard = mock(WebTarget.class);
        WebTarget publicInfoTarget = mock(WebTarget.class);

        when(bulletinBoard.path("publicInfo")).thenReturn(publicInfoTarget);
        final Invocation.Builder invocationBuilder = mock(Invocation.Builder.class);
        when(publicInfoTarget.request()).thenReturn(invocationBuilder);
        when(invocationBuilder.get(String.class)).thenReturn(mapper.writeValueAsString(infos.toArray()));

        List<PartialPublicInfo> publicInfos = FetchingUtilities.getPublicInfos(
                logger, bulletinBoard,
                CertificateHelper.getPublicKeyFromCertificate(electionCert),
                Arrays.asList(bbOneCert, bbTwoCert, bbThreeCert)
        );

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

        List<PartialPublicInfo> publicInfos = FetchingUtilities.getPublicInfos(
                logger, bulletinBoard,
                CertificateHelper.getPublicKeyFromCertificate(electionCert),
                Collections.singletonList(daOneCert));

        assertNull("Should not be able to fetch infos", publicInfos);
    }

    @Test
    public void testVerifyAndDetermineCommon() {
        String expected = "ActualString";
        List<SignedEntity<String>> entities = Arrays.asList(
                new SignedEntity<>(expected, bbOneSk),
                new SignedEntity<>("FaultyString", bbTwoSk),
                new SignedEntity<>("FaultyString", bbTwoSk),
                new SignedEntity<>("FaultyString", bbTwoSk),
                new SignedEntity<>("FaultyString", bbTwoSk),
                new SignedEntity<>(expected, bbThreeSk)
        );


        String chosen = FetchingUtilities.verifyAndDetermineCommon(
                entities,
                Arrays.asList(bbOneCert, bbTwoCert, bbThreeCert),
                logger
        );
        assertEquals("Wrong resulting string", expected, chosen);

    }

    @Test
    public void testGetBBCertificates() throws IOException {
        List<String> expected = new ArrayList<>(Arrays.asList(
                CertificateHelper.certificateToPem(bbOneCert),
                CertificateHelper.certificateToPem(bbTwoCert),
                CertificateHelper.certificateToPem(bbThreeCert)
        ));
        List<String> corrupt = new ArrayList<>(Arrays.asList(
                CertificateHelper.certificateToPem(bbThreeCert),
                CertificateHelper.certificateToPem(bbThreeCert) + "d"
        ));


        List<SignedEntity<StringListWrapper>> certList = Arrays.asList(
                new SignedEntity<>(new StringListWrapper(expected), bbOneSk),
                new SignedEntity<>(new StringListWrapper(expected), bbTwoSk),
                new SignedEntity<>(new StringListWrapper(corrupt), bbThreeSk)
        );
        String certsString = new ObjectMapper().writeValueAsString(
                certList.toArray()
        );

        WebTarget bulletinBoard = mock(WebTarget.class);
        WebTarget certsPath = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        when(bulletinBoard.path("peerCertificates")).thenReturn(certsPath);
        when(certsPath.request()).thenReturn(request);
        when(request.get(String.class)).thenReturn(certsString);

        List<X509CertificateHolder> bbPeerCertificates = FetchingUtilities.getBBPeerCertificates(
                logger,
                bulletinBoard,
                CertificateHelper.getPublicKeyFromCertificate(electionCert));


        assertNotNull("No certificates returned", bbPeerCertificates);
        List<String> asPem = bbPeerCertificates.stream().map(certificateHolder -> {
            try {
                return CertificateHelper.certificateToPem(certificateHolder);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }).collect(Collectors.toList());
        assertEquals("Wrong list of bb certificates", expected, asPem);
    }
}
