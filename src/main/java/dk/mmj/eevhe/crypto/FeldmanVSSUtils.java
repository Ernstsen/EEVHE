package dk.mmj.eevhe.crypto;

import java.math.BigInteger;

/**
 * Utility class for functionality regarding the Feldman VSS protocol
 */
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
     * @return Combined coefficient commitments g^f_i(j)
     */
    static BigInteger combineCoefficientCommitments(BigInteger[] coefficientCommitments, BigInteger j, BigInteger p) {
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
     * @return Whether g^u_i equals g^combinedCoefficientCommitments or not
     */
    public static boolean verifyCommitmentRespected(BigInteger g, BigInteger u, BigInteger[] coefficientCommitments,
                                                    BigInteger j, BigInteger p) {
        BigInteger combinedCoefficientCommitments = combineCoefficientCommitments(coefficientCommitments, j, p);

        BigInteger gU = g.modPow(u, p);

        return gU.equals(combinedCoefficientCommitments);
    }
}
