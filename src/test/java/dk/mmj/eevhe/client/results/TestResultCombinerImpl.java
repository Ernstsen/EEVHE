package dk.mmj.eevhe.client.results;

import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.entities.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static dk.mmj.eevhe.TestHelper.computePartialDecryptions;
import static dk.mmj.eevhe.TestHelper.runDKG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestResultCombinerImpl {

    private HashMap<Integer, PartialKeyPair> dkgRes;
    private List<PersistedBallot> ballots;
    private List<Candidate> candidates;
    private Map<Integer, PartialResultList> partialResultsMap;
    private PublicKey pk;

    @Before
    public void setUp() throws Exception {
        dkgRes = runDKG();
        pk = dkgRes.get(1).getPublicKey();
        this.ballots = Arrays.asList(
                new PersistedBallot(SecurityUtils.generateBallot(2, 3, "1", pk)),
                new PersistedBallot(SecurityUtils.generateBallot(1, 3, "2", pk)),
                new PersistedBallot(SecurityUtils.generateBallot(0, 3, "3", pk)),
                new PersistedBallot(SecurityUtils.generateBallot(2, 3, "4", pk))
        );
        Thread.sleep(5);//To ensure that time passes after persisted ballots has their TS set
        this.candidates = Arrays.asList(
                new Candidate(0, "Mette", "A"),
                new Candidate(1, "Jakob", "V"),
                new Candidate(2, "Morten", "B")
        );

        partialResultsMap = computePartialDecryptions(() -> ballots, candidates, dkgRes);
    }

    @Test
    public void testCombineWithoutForce() {
        long endTime = new Date().getTime();
        ArrayList<PartialPublicInfo> publicInfos = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            PartialKeyPair partialKeyPair = dkgRes.get(i);
            publicInfos.add(new PartialPublicInfo(i, partialKeyPair.getPublicKey(),
                    partialKeyPair.getPartialPublicKey(), candidates, endTime));
        }

        ResultCombinerImpl combiner = new ResultCombinerImpl(
                false,
                pk,
                candidates,
                () -> publicInfos,
                () -> {
                    fail("should not fetch ballots, when DAs agree and force is false");
                    return null;
                },
                endTime);

        ElectionResult electionResult = combiner.computeResult(new ArrayList<>(partialResultsMap.values()));

        assertEquals("4 votes were cast", 4, electionResult.getVotesTotal());
        assertEquals("Candidate 0 should have 1 vote", 1, electionResult.getCandidateVotes().get(0).intValue());
        assertEquals("Candidate 1 should have 1 vote", 1, electionResult.getCandidateVotes().get(1).intValue());
        assertEquals("Candidate 2 should have 2 votes", 2, electionResult.getCandidateVotes().get(2).intValue());
    }

    @Test
    public void testCombineStillWorkWhenDAsDisagreeOnVoteCount() {
        long endTime = new Date().getTime();
        ArrayList<PartialPublicInfo> publicInfos = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            PartialKeyPair partialKeyPair = dkgRes.get(i);
            publicInfos.add(new PartialPublicInfo(i, partialKeyPair.getPublicKey(),
                    partialKeyPair.getPartialPublicKey(), candidates, endTime));
        }

        ResultCombinerImpl combiner = new ResultCombinerImpl(
                false,
                pk,
                candidates,
                () -> publicInfos,
                () -> ballots,
                endTime);

        ArrayList<PartialResultList> res = new ArrayList<>(partialResultsMap.values());
        res.get(0).setVoteCount(88);
        res.get(1).setVoteCount(8);
        res.get(2).setVoteCount(882);
        ElectionResult electionResult = combiner.computeResult(res);

        assertEquals("4 votes were cast", 4, electionResult.getVotesTotal());
        assertEquals("Candidate 0 should have 1 vote", 1, electionResult.getCandidateVotes().get(0).intValue());
        assertEquals("Candidate 1 should have 1 vote", 1, electionResult.getCandidateVotes().get(1).intValue());
        assertEquals("Candidate 2 should have 2 votes", 2, electionResult.getCandidateVotes().get(2).intValue());
    }

    @Test
    public void testCombineStillWorkWhenDAsDisagreeOnCiphertext() {
        long endTime = new Date().getTime();
        ArrayList<PartialPublicInfo> publicInfos = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            PartialKeyPair partialKeyPair = dkgRes.get(i);
            publicInfos.add(new PartialPublicInfo(i, partialKeyPair.getPublicKey(),
                    partialKeyPair.getPartialPublicKey(), candidates, endTime));
        }

        ResultCombinerImpl combiner = new ResultCombinerImpl(
                false,
                pk,
                candidates,
                () -> publicInfos,
                () -> ballots,
                endTime);

        ArrayList<PartialResultList> res = new ArrayList<>(partialResultsMap.values());
        res.get(0).getResults().get(0).getCipherText().setC(res.get(0).getResults().get(0).getCipherText().getC().add(BigInteger.valueOf(5)));
        res.get(1).getResults().get(1).getCipherText().setC(res.get(1).getResults().get(1).getCipherText().getC().add(BigInteger.valueOf(12)));
        res.get(2).getResults().get(2).getCipherText().setC(res.get(2).getResults().get(2).getCipherText().getC().add(BigInteger.valueOf(25)));
        ElectionResult electionResult = combiner.computeResult(res);

        assertEquals("4 votes were cast", 4, electionResult.getVotesTotal());
        assertEquals("Candidate 0 should have 1 vote", 1, electionResult.getCandidateVotes().get(0).intValue());
        assertEquals("Candidate 1 should have 1 vote", 1, electionResult.getCandidateVotes().get(1).intValue());
        assertEquals("Candidate 2 should have 2 votes", 2, electionResult.getCandidateVotes().get(2).intValue());
    }

    @Test
    public void testCombineWithForce() {
        long endTime = new Date().getTime();
        ArrayList<PartialPublicInfo> publicInfos = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            PartialKeyPair partialKeyPair = dkgRes.get(i);
            publicInfos.add(new PartialPublicInfo(i, partialKeyPair.getPublicKey(),
                    partialKeyPair.getPartialPublicKey(), candidates, endTime));
        }

        ResultCombinerImpl combiner = new ResultCombinerImpl(
                true,
                pk,
                candidates,
                () -> publicInfos,
                () -> ballots,
                endTime);

        ElectionResult electionResult = combiner.computeResult(new ArrayList<>(partialResultsMap.values()));

        assertEquals("4 votes were cast", 4, electionResult.getVotesTotal());
        assertEquals("Candidate 0 should have 1 vote", 1, electionResult.getCandidateVotes().get(0).intValue());
        assertEquals("Candidate 1 should have 1 vote", 1, electionResult.getCandidateVotes().get(1).intValue());
        assertEquals("Candidate 2 should have 2 votes", 2, electionResult.getCandidateVotes().get(2).intValue());
    }
}
