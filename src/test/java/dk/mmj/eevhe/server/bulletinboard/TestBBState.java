package dk.mmj.eevhe.server.bulletinboard;

import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.entities.*;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.math.BigInteger.valueOf;

public class TestBBState {
    @Test
    public void testAddGetBallot() {
        BBState bbState = BBState.getInstance();

        CipherText c = new CipherText(valueOf(165), valueOf(684983));
        Proof p1 = new Proof(valueOf(64986), valueOf(859483), valueOf(92873452), valueOf(293885671));
        CipherText c2 = new CipherText(valueOf(1652), valueOf(68498));
        Proof p2 = new Proof(valueOf(4986), valueOf(8359483), valueOf(873452), valueOf(885671));
        List<CandidateVoteDTO> candidates = Arrays.asList(new CandidateVoteDTO(c, "id", p1), new CandidateVoteDTO(c2, "id2", p2));
        Proof p3 = new Proof(valueOf(486), valueOf(359483), valueOf(73452), valueOf(85671));

        BallotDTO ballotDTO = new BallotDTO(candidates, "id", p3);
        PersistedBallot persistedBallot = new PersistedBallot(ballotDTO);

        Assert.assertEquals("Ballot should not exist in BBState at this point", 0, bbState.getBallots().getBallots().size());
        Assert.assertFalse("Ballot should not exist in BBState at this point", bbState.hasVoted(persistedBallot));

        bbState.addBallot(persistedBallot);

        Assert.assertTrue("Ballot should exist in BBState at this point", bbState.hasVoted(persistedBallot));

        Assert.assertEquals("BBState should contain BallotList with exactly one ballot",
                                    new BallotList(new ArrayList<PersistedBallot>() {{add(persistedBallot);}}),
                                    bbState.getBallots());
        Assert.assertTrue("BBState should contain this specific ballot", bbState.hasVoted(persistedBallot));
    }

//    @Test
//    public void testAddGetResult() throws IOException {
//        BBState bbState = BBState.getInstance();
//
//        DLogProofUtils.Proof dp1 = new DLogProofUtils.Proof(valueOf(54), valueOf(9846));
//        DLogProofUtils.Proof dp2 = new DLogProofUtils.Proof(valueOf(62968), valueOf(613658874));
//
//        AsymmetricKeyParameter sk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
//        String cert = new String(Files.readAllBytes(Paths.get("certs/test_glob_key.pem")));
//
//        SignedEntity<PartialResultList> partialResultList = new SignedEntity<>(new PartialResultList(
//                Arrays.asList(
//                        new PartialResult(1, valueOf(234), dp1, c),
//                        new PartialResult(2, valueOf(6854), dp2, c2)),
//                58,
//                18
//        ), sk);
//
//        Assert.assertEquals("Partial Result should not exist in BBState at this point", 0, bbState.getResults().getResults().size());
//
//        bbState.addResult(partialResultList);
//
//        Assert.assertEquals("BBState should contain this specific Partial Result", partialResultList, bbState.getResults());
//
//    }
}
