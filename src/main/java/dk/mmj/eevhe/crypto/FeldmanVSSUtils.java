package dk.mmj.eevhe.crypto;

import dk.mmj.eevhe.entities.PartialKeyPair;
import dk.mmj.eevhe.entities.PartialSecretKey;
import dk.mmj.eevhe.entities.PublicKey;

import java.math.BigInteger;
import java.util.Arrays;

import static java.math.BigInteger.valueOf;

public class FeldmanVSSUtils {
    /**
     * @param g          The generator
     * @param p          The prime modulus
     * @param polynomial The polynomial
     * @return Coefficient commitment g^coefficient for all coefficients in polynomial
     */
    public static BigInteger[] computeCoefficientCommitments(BigInteger g, BigInteger p, BigInteger[] polynomial) {
        BigInteger[] coefficientCommitments = new BigInteger[polynomial.length];

        for (int i = 0; i < polynomial.length; i++) {
            coefficientCommitments[i] = g.modPow(polynomial[i], p);
        }

        return coefficientCommitments;
    }

    /**
     * @param coefficientCommitments Coefficient commitments
     * @param j                      DA id > 0
     * @param p                      Prime modulus p
     * @param q                      Prime modulus q = (p-1) / 2
     * @return Combined coefficient commitments g^f_i(j)
     */
    static BigInteger combineCoefficientCommitments(BigInteger[] coefficientCommitments, BigInteger j, BigInteger p, BigInteger q) {
        BigInteger acc = BigInteger.ONE;

        for (int t = 0; t < coefficientCommitments.length; t++) {
            BigInteger jExp = j.pow(t);
            acc = acc.multiply(coefficientCommitments[t].modPow(jExp, p));
        }

        return acc.mod(p);
    }


    /**
     * @param g                      Generator g
     * @param u                      u_i, which is equal to f_i(j)
     * @param coefficientCommitments Coefficient commitments
     * @param j                      DA id &#62; 0
     * @param p                      Prime modulus p
     * @param q                      Prime modulus q = (p-1)/2
     * @return Whether g^u_i equals g^combinedCoefficientCommitments or not
     */
    public static boolean verifyCommitmentRespected(BigInteger g, BigInteger u, BigInteger[] coefficientCommitments,
                                                    BigInteger j, BigInteger p, BigInteger q) {
        BigInteger combinedCoefficientCommitments = combineCoefficientCommitments(coefficientCommitments, j, p, q);

        BigInteger gU = g.modPow(u, p);

        return gU.equals(combinedCoefficientCommitments);
    }

    /**
     * <b>NOTE: does not include dLogPublicValue, as unable to generate that value without solving discrete log </b>
     *
     * @param g  Generator g
     * @param q  Prime modulus q = (p-1)/2
     * @param u  For all i: u_i, which is equal to f_i(j)
     * @param gV For all i: g^v_i
     * @return Partial key pair containing public key containing (h,g,q,p), partial public key h_j, partial secret key s_j
     */
    public static PartialKeyPair generateKeyPair(BigInteger g, BigInteger q, BigInteger[] u, BigInteger[] gV) {
        BigInteger p = valueOf(2).multiply(q).add(valueOf(1));

        BigInteger h = Arrays.stream(gV).reduce(BigInteger::multiply).orElse(BigInteger.ZERO).mod(p);
        PublicKey publicKey = new PublicKey(h, g, q);

        BigInteger partialSecretKey = Arrays.stream(u).reduce(BigInteger::add).orElse(BigInteger.ZERO).mod(q);
        BigInteger partialPublicKey = g.modPow(partialSecretKey, p);

        return new PartialKeyPair(new PartialSecretKey(partialSecretKey, null, p), partialPublicKey, publicKey);
    }
}
