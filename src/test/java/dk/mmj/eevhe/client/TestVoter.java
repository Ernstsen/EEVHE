package dk.mmj.eevhe.client;

import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.mmj.eevhe.Main;
import dk.mmj.eevhe.TestUsingBouncyCastle;
import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParametersImpl;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.AbstractServer;
import dk.mmj.eevhe.server.ServerState;
import dk.mmj.eevhe.server.bulletinboard.*;
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
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class TestVoter extends TestUsingBouncyCastle {
    private static final Logger logger = LogManager.getLogger(TestVoter.class);
    private static final List<AbstractServer> servers = new ArrayList<>();
    private static final int edgePort = 4894;
    private static final JerseyWebTarget edgeTarget = SSLHelper.configureWebTarget(logger, "https://localhost:" + edgePort);
    private static BigInteger h;
    private static PublicKey pk;
    private static X509CertificateHolder daOneCert;
    private static AsymmetricKeyParameter daOneSk;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Main.main(new String[]{"--configuration", "--bb_peer_addresses", "-1_https://localhost:18081"});

        KeyGenerationParametersImpl params = new KeyGenerationParametersImpl(4, 50);
        DistKeyGenResult keygen = ElGamal.generateDistributedKeys(params, 2, 3);

        h = SecurityUtils.lagrangeInterpolate(keygen.getPublicValues(), params.getPrimePair().getP());
        pk = new PublicKey(h, keygen.getG(), keygen.getQ());
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

        BulletinBoardPeer peer = new SingletonCommandLineParser<>(new BulletinBoardPeerConfigBuilder()).parse(new String[]{"--id=1", "--port=18081"}).produceInstance();
        BulletinBoardEdge edge = new SingletonCommandLineParser<>(new BulletinBoardEdgeConfigBuilder()).parse(new String[]{"--id=edge1", "--port=" + edgePort}).produceInstance();
        new Thread(peer).start();
        new Thread(edge).start();
        servers.add(peer);
        servers.add(edge);
        Thread.sleep(10_000);
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        for (AbstractServer server : servers) {
            server.terminate();
        }
        Thread.sleep(5_000);
    }

    private static void initializeBulletinBoard(JerseyWebTarget target) throws IOException {
        List<Candidate> candidates = Arrays.asList(
                new Candidate(0, "name1", "desc1"),
                new Candidate(1, "name2", "desc3"),
                new Candidate(2, "name3", "desc2")
        );

        SignedEntity<PartialPublicInfo> ppi = new SignedEntity<>(
                new PartialPublicInfo(1, pk, h, candidates, new Date().getTime() + (1_000 * 5),
                        CertificateHelper.certificateToPem(daOneCert)),
                daOneSk
        );

        assertEquals("should be successful in posting public info to bb", 204,
                target.path("publicInfo").request().post(Entity.entity(ppi, MediaType.APPLICATION_JSON)).getStatus()
        );
    }

    @Before
    public void setUp() throws Exception {
        ServerState.getInstance().put("bbState.1", new BulletinBoardState());
    }

    @Test
    public void testSingleVote() throws IOException {
        String id = "id";
        Voter.VoterConfiguration conf = new Voter.VoterConfiguration("https://localhost:" + edgePort, id, 2, null, Paths.get("certs/test_glob.pem"));
        Voter voter = new Voter(conf);

        initializeBulletinBoard(edgeTarget);

        voter.run();


        List<PersistedBallot> ballots = FetchingUtilities.getBallots(logger, edgeTarget, voter.getBBPeerCertificates());

        assertNotNull("Failed to fetch list of posted ballots", ballots);
        assertEquals("Did not find exactly one vote!", 1, ballots.size());
        PersistedBallot ballot = ballots.get(0);
        assertTrue("Failed to verify ballot", VoteProofUtils.verifyBallot(ballot, pk));
    }

    @Test
    public void testMultiVote() throws IOException {


        Voter.VoterConfiguration conf = new Voter.VoterConfiguration("https://localhost:" + edgePort, null, null, 5, Paths.get("certs/test_glob.pem"));
        Voter voter = new Voter(conf);
        JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + edgePort);

        initializeBulletinBoard(target);

        voter.run();

        List<PersistedBallot> ballots = FetchingUtilities.getBallots(
                logger, target,
                voter.getBBPeerCertificates()
        );

        assertNotNull("Failed to fetch list of posted ballots", ballots);
        assertEquals("Did not find exactly twenty votes!", 5, ballots.size());

        for (PersistedBallot ballot : ballots) {
            assertTrue("Failed to verify ballot", VoteProofUtils.verifyBallot(ballot, pk));
        }
    }

}
