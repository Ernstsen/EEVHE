package dk.mmj.eevhe.client;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.TestUsingBouncyCastle;
import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParametersImpl;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.ServerState;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoard;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoardConfigBuilder;
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
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestVoter extends TestUsingBouncyCastle {
    private static final Logger logger = LogManager.getLogger(TestVoter.class);
    private BigInteger h;
    private PublicKey pk;
    private X509CertificateHolder daOneCert;
    private AsymmetricKeyParameter daOneSk;

    @Before
    public void setUp() throws Exception {
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
    }

    @Test
    public void testSingleVote() throws InterruptedException, NoSuchBuilderException, WrongFormatException, IOException {
        int port = 4894;
        BulletinBoard.BulletinBoardConfiguration config =
                new SingletonCommandLineParser<>(new BulletinBoardConfigBuilder()).parse(new String[]{"--port=" + port});
        BulletinBoard bulletinBoard = new BulletinBoard(config);
        Thread thread = new Thread(bulletinBoard);
        thread.start();
        Thread.sleep(2_000);

        String id = "id";
        Voter.VoterConfiguration conf = new Voter.VoterConfiguration("https://localhost:" + port, id, 2, null);
        Voter voter = new Voter(conf);
        JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);

        initializeBulletinBoard(target);

        voter.run();

        BallotList fetchedBallotList = target.path("getBallots").request()
                .get(BallotList.class);

        List<PersistedBallot> ballots = fetchedBallotList.getBallots();
        assertEquals("Did not find exactly one vote!", 1, ballots.size());

        PersistedBallot ballot = ballots.get(0);
        assertTrue("Failed to verify ballot", VoteProofUtils.verifyBallot(ballot, pk));

        //Terminate BB
        bulletinBoard.terminate();
        thread.join();
        ServerState.getInstance().reset();
    }

    @Test
    public void testMultiVote() throws InterruptedException, NoSuchBuilderException, WrongFormatException, IOException {
        int port = 4896;
        BulletinBoard.BulletinBoardConfiguration config =
                new SingletonCommandLineParser<>(new BulletinBoardConfigBuilder()).parse(new String[]{"--port=" + port});
        BulletinBoard bulletinBoard = new BulletinBoard(config);
        Thread thread = new Thread(bulletinBoard);
        thread.start();
        Thread.sleep(2_000);

        Voter.VoterConfiguration conf = new Voter.VoterConfiguration("https://localhost:" + port, null, null, 20);
        Voter voter = new Voter(conf);
        JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);

        initializeBulletinBoard(target);

        voter.run();

        BallotList fetchedBallotList = target.path("getBallots").request()
                .get(new GenericType<>(BallotList.class));

        List<PersistedBallot> ballots = fetchedBallotList.getBallots();
        assertEquals("Did not find exactly twenty votes!", 20, ballots.size());

        for (PersistedBallot ballot : ballots) {
            assertTrue("Failed to verify ballot", VoteProofUtils.verifyBallot(ballot, pk));
        }

        //Terminate BB
        bulletinBoard.terminate();
        thread.join();
        ServerState.getInstance().reset();
    }

    private void initializeBulletinBoard(JerseyWebTarget target) throws IOException {
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

}
