package dk.mmj.eevhe.client;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParametersImpl;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.ServerState;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoard;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoardConfigBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestVoter {
    private static final Logger logger = LogManager.getLogger(TestVoter.class);
    private BigInteger h;
    private PublicKey pk;

    @Before
    public void setUp() throws Exception {
        KeyGenerationParametersImpl params = new KeyGenerationParametersImpl(4, 50);
        DistKeyGenResult keygen = ElGamal.generateDistributedKeys(params, 2, 3);

         h = SecurityUtils.combinePartialPublicKeys(keygen.getPublicValues(), params.getPrimePair().getP());
         pk = new PublicKey(h, keygen.getG(), keygen.getQ());
    }

    @Test
    public void testSingleVote() throws InterruptedException, NoSuchBuilderException, WrongFormatException {
        int port = 4894;
        BulletinBoard.BulletinBoardConfiguration config = (BulletinBoard.BulletinBoardConfiguration)
                new SingletonCommandLineParser(new BulletinBoardConfigBuilder()).parse(new String[]{"--port=" + port});
        BulletinBoard bulletinBoard = new BulletinBoard(config);
        Thread thread = new Thread(bulletinBoard);
        thread.start();

        String id = "id";
        Voter.VoterConfiguration conf = new Voter.VoterConfiguration("https://localhost:" + port, id, 2, null);
        Voter voter = new Voter(conf);
        JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);

        initializeBulletinBoard(target);

        voter.run();

        BallotList fetchedBallotList = target.path("getBallots").request()
                .get(new GenericType<>(BallotList.class));

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
    public void testMultiVote() throws InterruptedException, NoSuchBuilderException, WrongFormatException {
        int port = 4896;
        BulletinBoard.BulletinBoardConfiguration config = (BulletinBoard.BulletinBoardConfiguration)
                new SingletonCommandLineParser(new BulletinBoardConfigBuilder()).parse(new String[]{"--port=" + port});
        BulletinBoard bulletinBoard = new BulletinBoard(config);
        Thread thread = new Thread(bulletinBoard);
        thread.start();

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

    private void initializeBulletinBoard(JerseyWebTarget target) {
        List<Candidate> candidates = Arrays.asList(
                new Candidate(0, "name1", "desc1"),
                new Candidate(1, "name2", "desc3"),
                new Candidate(2, "name3", "desc2")
        );
        PartialPublicInfo ppi = new PartialPublicInfo(1, pk, h, candidates, new Date().getTime() + (1_000 * 5));

        assertEquals("should be successful in posting public info to bb", 204,
                target.path("publicInfo").request().post(Entity.entity(ppi, MediaType.APPLICATION_JSON)).getStatus()
        );
    }

}
