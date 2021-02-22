package dk.mmj.eevhe.crypto;

import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import org.apache.commons.collections4.ListUtils;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static java.math.BigInteger.valueOf;

/**
 * Class used for methods not tied directly to ElGamal
 */
public class SecurityUtils {
    /**
     * Find a random number in the range [1;n)
     *
     * @param n n-1 is upper limit in interval
     * @return random number in range [1;n)
     */
    public static BigInteger getRandomNumModN(BigInteger n) {
        Random random = new SecureRandom();
        BigInteger result = null;

        while (result == null || result.compareTo(new BigInteger("0")) == 0) {
            result = new BigInteger(n.bitLength(), random).mod(n);
        }

        return result;
    }

    /**
     * Hashes an array of values.
     *
     * @param payloads is an array of byte-arrays, containing values to be hashed.
     * @return SHA256 hash of the given payloads.
     */
    public static byte[] hash(byte[][] payloads) {
        SHA256Digest sha256Digest = new SHA256Digest();

        for (byte[] payload : payloads) {
            sha256Digest.update(payload, 0, payload.length);
        }

        byte[] hash = new byte[sha256Digest.getDigestSize()];
        sha256Digest.doFinal(hash, 0);

        return hash;
    }

    /**
     * Generates the ciphertext, vote, and proof.
     *
     * @param vote      the vote as an integer.
     * @param id        the ID of the person voting.
     * @param publicKey the public key used to encrypt the vote.
     * @return a VoteDTO containing the ciphertext, id and proof for the encrypted vote.
     */
    public static CandidateVoteDTO generateVote(int vote, String id, PublicKey publicKey) {
        BigInteger r = SecurityUtils.getRandomNumModN(publicKey.getQ());
        CipherText ciphertext = ElGamal.homomorphicEncryption(publicKey, valueOf(vote), r);
        Proof proof = VoteProofUtils.generateProof(ciphertext, publicKey, r, id, valueOf(vote));

        return new CandidateVoteDTO(ciphertext, id, proof);
    }

    public static BallotDTO generateBallot(int vote, int candidates, String id, PublicKey publicKey) {
        ArrayList<CandidateVoteDTO> votes = new ArrayList<>();
        BigInteger[] rVals = new BigInteger[candidates];
        List<CipherText> cipherTexts = new ArrayList<>();

        boolean voted = false;
        for (int i = 0; i < candidates; i++) {
            int isYes = i == vote ? 1 : 0;
            //If votes is yes, flip voted boolean as to register vote is not blank
            voted |= i == vote;

            BigInteger r = SecurityUtils.getRandomNumModN(publicKey.getQ());
            rVals[i] = r;

            CipherText ciphertext = ElGamal.homomorphicEncryption(publicKey, valueOf(isYes), r);
            cipherTexts.add(ciphertext);

            Proof proof = VoteProofUtils.generateProof(ciphertext, publicKey, r, id, valueOf(isYes));

            votes.add(new CandidateVoteDTO(ciphertext, id, proof));
        }

        BigInteger rSum = Arrays.stream(rVals).reduce(BigInteger.ZERO, BigInteger::add);
        CipherText cipherTextSum = concurrentSum(cipherTexts, 500);

        //Sum of votes is one if a vote was cast, zero if the votes was blank (outside range of candidate list)
        BigInteger sumOfVotes = voted ? BigInteger.ONE : BigInteger.ZERO;
        Proof proof = VoteProofUtils.generateProof(cipherTextSum, publicKey, rSum, id, sumOfVotes);

        return new BallotDTO(votes, id, proof);
    }

    /**
     * Generates a polynomial
     *
     * @param degree the degree of the polynomial
     * @param q      q-1 specifies the maximum value of coefficients in the polynomial
     * @return a BigInteger array representing the polynomial
     */
    public static BigInteger[] generatePolynomial(int degree, BigInteger q) {
        BigInteger[] polynomial = new BigInteger[degree + 1];
        for (int i = 0; i <= degree; i++) {
            polynomial[i] = getRandomNumModN(q);
        }

        return polynomial;
    }

    /**
     * Generates the secret values for each authority using Shamir's secret sharing scheme
     *
     * @param polynomial  the polynomial used for the scheme
     * @param authorities the amount of authorities
     * @param q           q = p/2 - 1
     * @return a map where the key is authority index and value is the corresponding secret value
     */
    static Map<Integer, BigInteger> generateSecretValues(BigInteger[] polynomial, int authorities, BigInteger q) {
        Map<Integer, BigInteger> secretValuesMap = new HashMap<>();

        for (int i = 0; i < authorities; i++) {
            int authorityIndex = i + 1;
            BigInteger evaluation = evaluatePolynomial(polynomial, authorityIndex);

            secretValuesMap.put(authorityIndex, evaluation.mod(q));
        }

        return secretValuesMap;
    }

    /**
     * Evaluates polynomial
     *
     * @param polynomial The polynomial to evaluate
     * @param x          The variable to evaluate the polynomial at
     * @return The BigInteger value of the evaluated polynomial
     */
    public static BigInteger evaluatePolynomial(BigInteger[] polynomial, int x) {
        BigInteger acc = BigInteger.ZERO;

        for (int j = 0; j < polynomial.length; j++) {
            acc = acc.add(valueOf(x).pow(j).multiply(polynomial[j]));
        }

        return acc;
    }

    /**
     * Generates the public values for each authority
     *
     * @param secretValuesMap The secret values
     * @param g               generator for group Gq where p = 2q + 1
     * @param p               p the modulus prime
     * @return a map where the key is an authority index and value is the corresponding public value
     */
    static Map<Integer, BigInteger> generatePublicValues(Map<Integer, BigInteger> secretValuesMap, BigInteger g, BigInteger p) {
        return secretValuesMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> g.modPow(e.getValue(), p)
                ));
    }

    /**
     * Generates a lagrange coefficient
     *
     * @param authorityIndexes  array of decryption authority's indexes, corresponding to x-values
     * @param currentIndexValue current decryption authority's index
     * @param q                 q = p - 1 / 2
     * @return the lagrange coefficient
     */
    static BigInteger generateLagrangeCoefficient(int[] authorityIndexes, int currentIndexValue, BigInteger q) {
        BigInteger acc = BigInteger.ONE;
        BigInteger currentIndexBig = valueOf(currentIndexValue);

        for (int authorityIndex : authorityIndexes) {
            if (authorityIndex != currentIndexValue) {
                BigInteger iBig = valueOf(authorityIndex);
                BigInteger diff = iBig.subtract(currentIndexBig);
                BigInteger diffModInv = diff.modInverse(q);
                acc = acc.multiply(iBig.multiply(diffModInv)).mod(q);
            }
        }

        return acc;
    }

    /**
     * Computes partial
     *
     * @param c           is the base value
     * @param secretValue the secret value only known by the specific decryption authorities
     * @param p           the modulus prime
     * @return the partial value
     */
    public static BigInteger computePartial(BigInteger c, BigInteger secretValue, BigInteger p) {
        return c.modPow(secretValue, p);
    }

    /**
     * Combines partials
     *
     * @param partialsMap a map where the key is an authority index and value is a corresponding partial
     * @param p           the modulus prime
     * @return the combination of the partials
     */
    public static BigInteger combinePartials(Map<Integer, BigInteger> partialsMap, BigInteger p) {
        BigInteger q = p.subtract(BigInteger.ONE).divide(valueOf(2));
        Integer[] authorityIndexesInteger = partialsMap.keySet().toArray(new Integer[0]);
        int[] authorityIndexes = new int[authorityIndexesInteger.length];
        for (int i = 0; i < authorityIndexesInteger.length; i++) {
            authorityIndexes[i] = authorityIndexesInteger[i];
        }

        return partialsMap.keySet().stream()
                .map(key -> partialsMap.get(key).modPow(generateLagrangeCoefficient(authorityIndexes, key, q), p))
                .reduce(BigInteger.ONE, BigInteger::multiply).mod(p);
    }

    /**
     * Computes the sum of all votes.
     * <br/>
     * Before sum is computed all proofs are verified, and those that could not are discarded.
     * <br/>
     * The method are executed synchronously
     *
     * @param votes     list of votes which should be summed
     * @param publicKey public key the votes are encrypted under
     * @return sum of all votes - meaning the product of the ciphertexts
     */
    static CipherText voteSum(List<? extends CandidateVoteDTO> votes, PublicKey publicKey) {
        CipherText acc = new CipherText(BigInteger.ONE, BigInteger.ONE);

        return votes.stream()
                .filter(v -> VoteProofUtils.verifyProof(v, publicKey))
                .map(CandidateVoteDTO::getCipherText)
                .reduce(acc, ElGamal::homomorphicAddition);
    }

    /**
     * Computes the sum of all votes.
     * <br>
     * Before sum is computed all proofs are verified, and those that could not are discarded.
     * <br>
     * The method is executed asynchronously.
     * All votes are partitioned into subsets of size <code>partitionSize</code>, which are summed in their own thread
     *
     * @param votes         list of votes
     * @param publicKey     public key the votes are encrypted under
     * @param partitionSize size of partitions
     * @return sum of all votes
     */
    public static CipherText concurrentVoteSum(List<? extends CandidateVoteDTO> votes, PublicKey publicKey, int partitionSize) {

        List<CipherText> cipherTexts = votes.parallelStream()
                .filter(v -> VoteProofUtils.verifyProof(v, publicKey))
                .map(CandidateVoteDTO::getCipherText)
                .collect(Collectors.toList());

        return concurrentSum(cipherTexts, partitionSize);
    }

    /**
     * Concurrently sums votes contained in list of cipherTexts.
     * <br/>
     * Partitions cipherTexts and sums them in different threads.
     *
     * @param cipherTexts   list of cipherTexts to be summed
     * @param partitionSize size of partitions.
     * @return sum of all cipherTexts
     */
    static CipherText concurrentSum(List<CipherText> cipherTexts, int partitionSize) {
        ConcurrentLinkedQueue<CipherText> result = new ConcurrentLinkedQueue<>();

        if (cipherTexts.size() > 2 * partitionSize) {
            List<Thread> threads = new ArrayList<>();

            List<List<CipherText>> partitions = ListUtils.partition(cipherTexts, partitionSize);
            for (List<CipherText> partition : partitions) {
                Thread thread = new Thread(new VoteSummer(result, partition));
                thread.start();
                threads.add(thread);
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Filtering thread was interrupted", e);
                }
            }

            return concurrentSum(new ArrayList<>(result), partitionSize);

        } else {
            CipherText acc = new CipherText(BigInteger.ONE, BigInteger.ONE);

            return cipherTexts.stream().reduce(acc, ElGamal::homomorphicAddition);
        }
    }

    private static class VoteSummer implements Runnable {
        private final Collection<CipherText> resultRef;
        private final List<CipherText> values;

        VoteSummer(Collection<CipherText> resultRef, List<CipherText> values) {
            this.resultRef = resultRef;
            this.values = values;
        }

        @Override
        public void run() {
            CipherText acc = new CipherText(BigInteger.ONE, BigInteger.ONE);

            CipherText sum = values.stream().reduce(acc, ElGamal::homomorphicAddition);
            resultRef.add(sum);
        }
    }
}
