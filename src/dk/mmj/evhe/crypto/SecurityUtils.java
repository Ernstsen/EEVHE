package dk.mmj.evhe.crypto;

import dk.mmj.evhe.entities.CipherText;
import dk.mmj.evhe.entities.PublicKey;
import dk.mmj.evhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.evhe.entities.VoteDTO;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

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
    public static VoteDTO generateVote(int vote, String id, PublicKey publicKey) {
        BigInteger r = SecurityUtils.getRandomNumModN(publicKey.getQ());
        CipherText ciphertext = ElGamal.homomorphicEncryption(publicKey, BigInteger.valueOf(vote), r);
        VoteDTO.Proof proof = VoteProofUtils.generateProof(ciphertext, publicKey, r, id, BigInteger.valueOf(vote));

        return new VoteDTO(ciphertext, id, proof);
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
     * @return a map where the key is authority index and value is the corresponding secret value
     */
    public static Map<Integer, BigInteger> generateSecretValues(BigInteger[] polynomial, int authorities) {
        Map<Integer, BigInteger> secretValuesMap = new HashMap<>();

        for (int i = 0; i < authorities; i++) {
            int authorityIndex = i + 1;
            BigInteger acc = BigInteger.ZERO;

            for (int j = 0; j < polynomial.length; j++) {
                acc = acc.add(BigInteger.valueOf(authorityIndex).pow(j).multiply(polynomial[j]));
            }

            secretValuesMap.put(authorityIndex, acc);
        }

        return secretValuesMap;
    }

    /**
     * Generates the public values for each authority
     *
     * @param secretValuesMap The secret values
     * @param g               generator for group Gq where p = 2q + 1
     * @param p               the p value mentioned above
     * @return a map where the key is an authority index and value is the corresponding public value
     */
    public static Map<Integer, BigInteger> generatePublicValues(Map<Integer, BigInteger> secretValuesMap, BigInteger g, BigInteger p) {
        return secretValuesMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> g.modPow(e.getValue(), p)
                ));
    }

    /**
     * Generates a lagrange coefficient
     *
     * @param authorityIndexes array of decryption authority's indexes, corresponding to x-values
     * @param currentIndexValue current decryption authority's index
     * @param p p the modulus prime
     * @return the lagrange coefficient
     */
    public static BigInteger generateLagrangeCoefficient(int[] authorityIndexes, int currentIndexValue, BigInteger p) {
        BigInteger lagrangeCoefficient = BigInteger.ONE;
        BigInteger currentIndexBig = BigInteger.valueOf(currentIndexValue);

        for (int authorityIndex : authorityIndexes) {
            if (authorityIndex != currentIndexValue) {
                BigInteger iBig = BigInteger.valueOf(authorityIndex);

                lagrangeCoefficient = lagrangeCoefficient.multiply(iBig.multiply(iBig.subtract(currentIndexBig).modInverse(p))).mod(p);
            }
        }

        return lagrangeCoefficient;
    }

    /**
     * Computes partial
     *
     * @param a is the base value
     * @param secretValue the secret value only known by the specific decryption authorities
     * @param p the modulus prime
     * @return the partial value
     */
    public static BigInteger computePartial(BigInteger a, BigInteger secretValue, BigInteger p) {
        return a.modPow(secretValue, p);
    }

    /**
     * Combines partials
     *
     * @param partialsMap a map where the key is an authority index and value is a corresponding partial
     * @param p the modulus prime
     * @return the combination of the partials
     */
    public static BigInteger combinePartials(Map<Integer, BigInteger> partialsMap, BigInteger p) {
        BigInteger acc = BigInteger.ONE;

        Integer[] authorityIndexesInteger = partialsMap.keySet().toArray(new Integer[partialsMap.keySet().size()]);
        int[] authorityIndexes = new int[authorityIndexesInteger.length];
        for (int i = 0; i < authorityIndexesInteger.length; i++) {
            authorityIndexes[i] = authorityIndexesInteger[i].intValue();
        }

        partialsMap.forEach((x, partial) ->
                acc.multiply(partial.modPow(generateLagrangeCoefficient(authorityIndexes, x, p), p))
        );

        return acc;
    }
}
