package dk.mmj.eevhe.crypto;

import dk.mmj.eevhe.crypto.exceptions.UnableToDecryptException;
import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParameters;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.crypto.TestUtils.*;
import static java.math.BigInteger.valueOf;
import static org.junit.Assert.*;

public class TestSecurityUtils {

    public static final int ITERATIONS = 20;

    @Test
    public void shouldCreateCorrectVote1() {
        KeyPair keyPair = generateKeysFromP2048bitsG2();
        String id = "TESTID";
        CandidateVoteDTO candidateVoteDTO = SecurityUtils.generateVote(1, id, keyPair.getPublicKey());

        boolean verified = VoteProofUtils.verifyProof(candidateVoteDTO, keyPair.getPublicKey());
        assertTrue("Unable to verify generated vote", verified);

        try {
            int message = ElGamal.homomorphicDecryption(keyPair, candidateVoteDTO.getCipherText(), 1000);
            assertEquals("Decrypted message to wrong value", 1, message);
        } catch (UnableToDecryptException e) {
            fail("Unable to decrypt generated ciphertext");
        }
    }

    @Test
    public void shouldCreateCorrectVote1newTest() {
        KeyPair keyPair = generateKeysFromP2048bitsG2();
        String id = "TESTID";
        for (int i = 0; i < 5; i++) {
            BallotDTO ballotDTO = SecurityUtils.generateBallot(i, 5, id, keyPair.getPublicKey());

            boolean verified = VoteProofUtils.verifyBallot(ballotDTO, keyPair.getPublicKey());
            assertTrue("Unable to verify generated vote", verified);

            try {
                int message = ElGamal.homomorphicDecryption(keyPair, SecurityUtils.voteSum(ballotDTO.getCandidateVotes(), keyPair.getPublicKey()), 1000);
                assertEquals("Decrypted message to wrong value", 1, message);
            } catch (UnableToDecryptException e) {
                fail("Unable to decrypt generated ciphertext");
            }
        }
    }


    /**
     * Tests that the ballot-creations used in negative test, actually works
     */
    @Test
    public void shouldAcceptLocalBallotCreation() {
        KeyPair keyPair = generateKeysFromP2048bitsG2();
        String id = "TESTID";
        for (int i = 0; i < 5; i++) {

            BallotDTO ballotDTO = generateBallot(5, Collections.singletonList(i), keyPair.getPublicKey(), id);

            boolean verified = VoteProofUtils.verifyBallot(ballotDTO, keyPair.getPublicKey());
            assertTrue("Unable to verify generated vote", verified);

            try {
                int message = ElGamal.homomorphicDecryption(keyPair, SecurityUtils.voteSum(ballotDTO.getCandidateVotes(), keyPair.getPublicKey()), 1000);
                assertEquals("Decrypted message to wrong value", 1, message);
            } catch (UnableToDecryptException e) {
                fail("Unable to decrypt generated ciphertext");
            }
        }
    }

    @Test
    public void shouldRejectBallot() {
        KeyPair keyPair = generateKeysFromP2048bitsG2();
        String id = "TESTID";
        for (List<Integer> list : Arrays.asList(
                Arrays.asList(2, 3),
                Arrays.asList(1, 4),
                Arrays.asList(3, 4),
                Arrays.asList(2, 4))) {

            BallotDTO ballotDTO = generateBallot(5, list, keyPair.getPublicKey(), id);

            boolean verified = VoteProofUtils.verifyBallot(ballotDTO, keyPair.getPublicKey());
            assertFalse("Verified invalidly generated vote", verified);
        }
    }

    @Test
    public void shouldCreateCorrectVote0() {
        KeyPair keyPair = generateKeysFromP2048bitsG2();
        String id = "TESTID";
        CandidateVoteDTO candidateVoteDTO = SecurityUtils.generateVote(0, id, keyPair.getPublicKey());

        boolean verified = VoteProofUtils.verifyProof(candidateVoteDTO, keyPair.getPublicKey());
        assertTrue("Unable to verify generated vote", verified);

        try {
            int message = ElGamal.homomorphicDecryption(keyPair, candidateVoteDTO.getCipherText(), 1000);
            assertEquals("Decrypted message to wrong value", 0, message);
        } catch (UnableToDecryptException e) {
            fail("Unable to decrypt generated ciphertext");
        }
    }

    @Test
    public void shouldReturn3AsLagrangeCoefficientForIndex1WithSParams() {
        int[] authorityIndexes = new int[]{1, 2, 3};

        BigInteger lagrangeCoefficient = SecurityUtils.generateLagrangeCoefficient(authorityIndexes, 1, valueOf(5));

        assertEquals("Lagrange coefficient incorrect", valueOf(3), lagrangeCoefficient);
    }

    @Test
    public void shouldReturn2AsLagrangeCoefficientForIndex2WithSParams() {
        int[] authorityIndexes = new int[]{1, 2, 3};

        BigInteger lagrangeCoefficient = SecurityUtils.generateLagrangeCoefficient(authorityIndexes, 2, valueOf(5));

        assertEquals("Lagrange coefficient incorrect", valueOf(2), lagrangeCoefficient);
    }

    @Test
    public void shouldReturn1AsLagrangeCoefficientForIndex3WithSParams() {
        int[] authorityIndexes = new int[]{1, 2, 3};

        BigInteger lagrangeCoefficient = SecurityUtils.generateLagrangeCoefficient(authorityIndexes, 3, valueOf(5));

        assertEquals("Lagrange coefficient incorrect", valueOf(1), lagrangeCoefficient);
    }

    private void testRecoveringOfSecretKey(KeyGenerationParameters params, int[] authorityIndexes, int excludedIndex) {
        BigInteger[] polynomial = SecurityUtils.generatePolynomial(1, params.getPrimePair().getQ());
        Map<Integer, BigInteger> secretValues = SecurityUtils.generateSecretValues(polynomial, 3, params.getPrimePair().getQ());

        BigInteger acc = BigInteger.ZERO;
        for (Map.Entry<Integer, BigInteger> e : secretValues.entrySet()) {
            if (e.getKey() != excludedIndex) {
                BigInteger lagrangeCoefficient = SecurityUtils.generateLagrangeCoefficient(authorityIndexes, e.getKey(), params.getPrimePair().getQ());
                acc = acc.add(e.getValue().multiply(lagrangeCoefficient));
            }
        }

        assertEquals("Secret keys did not match", polynomial[0], acc.mod(params.getPrimePair().getQ()));
    }

    @Test
    public void shouldRecoverSecretKeyWithSecrets123WhenNIs3WithSParams() {
        // No index is excluded
        testRecoveringOfSecretKey(getKeyGenParamsFromP11G2(), new int[]{1, 2, 3}, 0);
    }

    @Test
    public void shouldRecoverSecretKeyWithSecrets12WhenNIs3WithSParams() {
        testRecoveringOfSecretKey(getKeyGenParamsFromP11G2(), new int[]{1, 2}, 3);
    }

    @Test
    public void shouldRecoverSecretKeyWithSecrets13WhenNIs3WithSParams() {
        testRecoveringOfSecretKey(getKeyGenParamsFromP11G2(), new int[]{1, 3}, 2);
    }

    @Test
    public void shouldRecoverSecretKeyWithSecrets23WhenNIs3WithSParams() {
        testRecoveringOfSecretKey(getKeyGenParamsFromP11G2(), new int[]{2, 3}, 1);
    }

    @Test
    public void shouldRecoverSecretKeyWithSecrets123WhenNIs3WithLParams() {
        // No index is excluded
        testRecoveringOfSecretKey(getKeyGenParamsFromP2048bitsG2(), new int[]{1, 2, 3}, 0);
    }

    @Test
    public void shouldRecoverSecretKeyWithSecrets12WhenNIs3WithLParams() {
        testRecoveringOfSecretKey(getKeyGenParamsFromP2048bitsG2(), new int[]{1, 2}, 3);
    }

    @Test
    public void shouldRecoverSecretKeyWithSecrets13WhenNIs3WithLParams() {
        testRecoveringOfSecretKey(getKeyGenParamsFromP2048bitsG2(), new int[]{1, 3}, 2);
    }

    @Test
    public void shouldRecoverSecretKeyWithSecrets23WhenNIs3WithLParams() {
        testRecoveringOfSecretKey(getKeyGenParamsFromP2048bitsG2(), new int[]{2, 3}, 1);
    }

    private void testRecoveringOfPublicKey(List<Integer> excludedIndexes, boolean positiveTest) {
        KeyGenerationParameters params = getKeyGenParamsFromP2048bitsG2();
        BigInteger p = params.getPrimePair().getP();
        BigInteger q = params.getPrimePair().getQ();
        BigInteger g = params.getGenerator();
        BigInteger[] polynomial = SecurityUtils.generatePolynomial(1, q);
        BigInteger h = g.modPow(polynomial[0], p);

        Map<Integer, BigInteger> secretValues = SecurityUtils.generateSecretValues(polynomial, 3, q).entrySet().stream()
                .filter(e -> !excludedIndexes.contains(e.getKey())).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        Map<Integer, BigInteger> publicValues = SecurityUtils.generatePublicValues(secretValues, g, p);

        BigInteger hFromPartials = SecurityUtils.combinePartials(publicValues, p);

        if (positiveTest) {
            assertEquals("Public keys did not match", h, hFromPartials);
        } else {
            assertNotEquals("Public keys match; they should not match", h, hFromPartials);
        }
    }

    @Test
    public void testEvaluatePolynomial() {
        BigInteger[] polynomial = new BigInteger[]{valueOf(11), valueOf(2), valueOf(2)};
        int x = 5;
        BigInteger actualResult = SecurityUtils.evaluatePolynomial(polynomial, x);
        BigInteger expectedResult = polynomial[0].add(valueOf(x).multiply(polynomial[1]))
                                                 .add(valueOf(x).pow(2).multiply(polynomial[2]));

        assertEquals("Evaluation of polynomial failed", actualResult, expectedResult);
    }

    @Test
    public void testComputeCoefficientCommitments() {
        BigInteger[] polynomial = new BigInteger[]{valueOf(11), valueOf(2), valueOf(2)};
        BigInteger g = valueOf(2);
        BigInteger p = valueOf(7919);
        BigInteger[] actualResult = SecurityUtils.computeCoefficientCommitments(g, p, polynomial);

        BigInteger[] expectedResult = new BigInteger[3];
        expectedResult[0] = g.modPow(polynomial[0], p);
        expectedResult[1] = g.modPow(polynomial[1], p);
        expectedResult[2] = g.modPow(polynomial[2], p);

        assertEquals("Computation of coefficient commitments failed", actualResult, expectedResult);
    }

    @Test
    public void shouldBeAbleToRecoverPublicKeyWithSecrets123WhenNIs3() {
        testRecoveringOfPublicKey(Collections.singletonList(0), true);
    }

    @Test
    public void shouldBeAbleToRecoverPublicKeyWithSecrets12WhenNIs3() {
        testRecoveringOfPublicKey(Collections.singletonList(3), true);
    }

    @Test
    public void shouldBeAbleToRecoverPublicKeyWithSecrets13WhenNIs3() {
        testRecoveringOfPublicKey(Collections.singletonList(2), true);
    }

    @Test
    public void shouldBeAbleToRecoverPublicKeyWithSecrets23WhenNIs3() {
        testRecoveringOfPublicKey(Collections.singletonList(1), true);
    }

    @Test
    public void shouldNotBeAbleToRecoverPublicKeyWithSecret1WhenNIs2() {
        testRecoveringOfPublicKey(Arrays.asList(2, 3), false);
    }

    @Test
    public void shouldNotBeAbleToRecoverPublicKeyWithSecret2WhenNIs2() {
        testRecoveringOfPublicKey(Arrays.asList(1, 3), false);
    }

    @Test
    public void shouldNotBeAbleToRecoverPublicKeyWithSecret3WhenNIs2() {
        testRecoveringOfPublicKey(Arrays.asList(1, 2), false);
    }

    @Test
    public void shouldBeSameSumNoWrongProof() {
        KeyPair keyPair = generateKeysFromP2048bitsG2();
        PublicKey publicKey = keyPair.getPublicKey();

        ArrayList<CandidateVoteDTO> votes = new ArrayList<>();
        int amount = ITERATIONS;
        for (int i = 0; i < amount; i++) {
            votes.add(SecurityUtils.generateVote(i % 2, "ID" + 1, publicKey));
        }

        long time = new Date().getTime();
        CipherText oldSum = SecurityUtils.voteSum(votes, publicKey);
        long elapsedOld = new Date().getTime() - time;
        System.out.println("Did old sum of " + amount + " votes in " + elapsedOld + "ms");

        time = new Date().getTime();
        CipherText concSum = SecurityUtils.concurrentVoteSum(votes, publicKey, amount / 10);
        long elapsedConc = new Date().getTime() - time;
        System.out.println("Did concurrent sum of " + amount + " votes in " + elapsedConc + "ms");

        assertEquals("Sums did not match.", oldSum, concSum);
    }

    @Test
    public void shouldBeSameSumSOMEWrongProof() {
        KeyPair keyPair = generateKeysFromP2048bitsG2();
        PublicKey publicKey = keyPair.getPublicKey();

        List<? extends CandidateVoteDTO> votes = generateVotes(ITERATIONS, publicKey);

        CipherText oldSum = SecurityUtils.voteSum(votes, publicKey);

        CipherText concSum = SecurityUtils.concurrentVoteSum(votes, publicKey, ITERATIONS / 10);

        assertEquals("Sums did not match.", oldSum, concSum);
    }

    @Test
    public void benchmarkFilter() {
        KeyPair keyPair = generateKeysFromP2048bitsG2();
        PublicKey publicKey = keyPair.getPublicKey();
        long endTime = new Date().getTime() + 5000;

        List<PersistedVote> votes = generateVotes(ITERATIONS, publicKey);

        List<PersistedVote> collect = votes.stream().filter(v -> v.getTs().getTime() < endTime).collect(Collectors.toList());

        List<PersistedVote> collectConc = votes.stream().filter(v -> v.getTs().getTime() < endTime).collect(Collectors.toList());

        assertEquals("Filters did not match in results", collect.size(), collectConc.size());
    }

    private BallotDTO generateBallot(int candidates, List<Integer> chosenVotes, PublicKey publicKey, String id) {
        ArrayList<CandidateVoteDTO> votes = new ArrayList<>();
        BigInteger[] rVals = new BigInteger[candidates];
        List<CipherText> cipherTexts = new ArrayList<>();

        boolean voted = false;
        for (int i = 0; i < candidates; i++) {
            boolean isChosen = chosenVotes.contains(i);
            int isYes = isChosen ? 1 : 0;
            //If votes is yes, flip voted boolean as to register vote is not blank
            voted |= isChosen;

            BigInteger r = SecurityUtils.getRandomNumModN(publicKey.getQ());
            rVals[i] = r;

            CipherText ciphertext = ElGamal.homomorphicEncryption(publicKey, valueOf(isYes), r);
            cipherTexts.add(ciphertext);

            Proof proof = VoteProofUtils.generateProof(ciphertext, publicKey, r, id, valueOf(isYes));

            votes.add(new CandidateVoteDTO(ciphertext, id, proof));
        }

        BigInteger rSum = Arrays.stream(rVals).reduce(BigInteger.ZERO, BigInteger::add);
        CipherText cipherTextSum = SecurityUtils.concurrentSum(cipherTexts, 500);

        //Sum of votes is one if a vote was cast, zero if the votes was blank (outside range of candidate list)
        BigInteger sumOfVotes = voted ? BigInteger.ONE : BigInteger.ZERO;
        Proof proof = VoteProofUtils.generateProof(cipherTextSum, publicKey, rSum, id, sumOfVotes);

        return new BallotDTO(votes, id, proof);
    }
}
