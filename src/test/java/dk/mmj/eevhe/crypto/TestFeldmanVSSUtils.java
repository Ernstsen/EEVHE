package dk.mmj.eevhe.crypto;

import dk.mmj.eevhe.entities.PartialKeyPair;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.*;

public class TestFeldmanVSSUtils {
    @Test
    public void testComputeCoefficientCommitments() {
        BigInteger[] polynomial = new BigInteger[]{valueOf(11), valueOf(2), valueOf(2)};
        BigInteger g = valueOf(2);
        BigInteger p = valueOf(7919);
        BigInteger[] actualResult = FeldmanVSSUtils.computeCoefficientCommitments(g, p, polynomial);

        BigInteger[] expectedResult = new BigInteger[3];
        expectedResult[0] = g.modPow(polynomial[0], p);
        expectedResult[1] = g.modPow(polynomial[1], p);
        expectedResult[2] = g.modPow(polynomial[2], p);

        assertArrayEquals("Computation of coefficient commitments failed", expectedResult, actualResult);
    }

    @Test
    public void testComputeCoefficientCommitmentsBig() {
        BigInteger[] polynomial = new BigInteger[]{valueOf(3000), valueOf(100), valueOf(2)};
        BigInteger g = valueOf(2);
        BigInteger p = valueOf(7919);
        BigInteger[] actualResult = FeldmanVSSUtils.computeCoefficientCommitments(g, p, polynomial);

        BigInteger[] expectedResult = new BigInteger[3];
        expectedResult[0] = g.modPow(polynomial[0], p);
        expectedResult[1] = g.modPow(polynomial[1], p);
        expectedResult[2] = g.modPow(polynomial[2], p);

        assertArrayEquals("Computation of coefficient commitments failed", expectedResult, actualResult);
    }

    @Test
    public void testCombineCoefficientCommitments() {
        BigInteger g = valueOf(2);
        BigInteger q = valueOf(7919);
        BigInteger p = q.multiply(valueOf(2)).add(valueOf(1));
        BigInteger j = valueOf(1);

        BigInteger[] polynomial = new BigInteger[]{valueOf(11), valueOf(2), valueOf(2)};
        BigInteger[] coefficientCommitments = FeldmanVSSUtils.computeCoefficientCommitments(g, p, polynomial);

        BigInteger actualResult = FeldmanVSSUtils.combineCoefficientCommitments(coefficientCommitments, j, p, q);

        BigInteger jExp = j.modPow(valueOf(2), q);
        BigInteger expectedResult = coefficientCommitments[0]
                .multiply(coefficientCommitments[1].modPow(j, p))
                .multiply(coefficientCommitments[2].modPow(jExp, p)).mod(p);

        assertEquals("Combining coefficient commitments failed", expectedResult, actualResult);
    }

    @Test
    public void testCombineCoefficientCommitmentsBig() {
        BigInteger g = valueOf(2);
        BigInteger q = valueOf(7919);
        BigInteger p = q.multiply(valueOf(2)).add(valueOf(1));
        BigInteger j = valueOf(3);

        BigInteger[] polynomial = new BigInteger[]{valueOf(3000), valueOf(100), valueOf(2)};
        BigInteger[] coefficientCommitments = FeldmanVSSUtils.computeCoefficientCommitments(g, p, polynomial);

        BigInteger actualResult = FeldmanVSSUtils.combineCoefficientCommitments(coefficientCommitments, j, p, q);

        BigInteger jExp = j.modPow(valueOf(2), q);
        BigInteger expectedResult = coefficientCommitments[0]
                .multiply(coefficientCommitments[1].modPow(j, p))
                .multiply(coefficientCommitments[2].modPow(jExp, p)).mod(p);

        assertEquals("Combining coefficient commitments failed", expectedResult, actualResult);
    }


    @Test
    public void testVerifyCommitmentRespected() {
        BigInteger g = valueOf(2);
        BigInteger q = valueOf(7919);
        BigInteger p = q.multiply(valueOf(2)).add(valueOf(1));
        BigInteger j = valueOf(1);

        BigInteger[] polynomial = new BigInteger[]{valueOf(11), valueOf(2), valueOf(2)};
        BigInteger[] coefficientCommitments = FeldmanVSSUtils.computeCoefficientCommitments(g, p, polynomial);
        BigInteger u = SecurityUtils.evaluatePolynomial(polynomial, j.intValue());

        boolean actualResult = FeldmanVSSUtils.verifyCommitmentRespected(g, u, coefficientCommitments, j, p, q);

        assertTrue(actualResult);
    }

    @Test
    public void testVerifyCommitmentRespectedBig() {
        BigInteger g = valueOf(2);
        BigInteger q = valueOf(7919);
        BigInteger p = q.multiply(valueOf(2)).add(valueOf(1));
        BigInteger j = valueOf(3);

        BigInteger[] polynomial = new BigInteger[]{valueOf(3000), valueOf(100), valueOf(2)};
        BigInteger[] coefficientCommitments = FeldmanVSSUtils.computeCoefficientCommitments(g, p, polynomial);
        BigInteger u = SecurityUtils.evaluatePolynomial(polynomial, j.intValue());

        boolean actualResult = FeldmanVSSUtils.verifyCommitmentRespected(g, u, coefficientCommitments, j, p, q);

        assertTrue(actualResult);
    }

    @Test
    public void testGenerateKeyPair() {
        BigInteger g = valueOf(2);
        BigInteger q = valueOf(7919);
        BigInteger p = q.multiply(valueOf(2)).add(valueOf(1));
        BigInteger j = valueOf(1);

        BigInteger[][] polynomials = new BigInteger[][]{
                {valueOf(11), valueOf(1), valueOf(2)},
                {valueOf(11), valueOf(1), valueOf(2)},
                {valueOf(11), valueOf(1), valueOf(2)}};

        BigInteger[] u = new BigInteger[]{
                SecurityUtils.evaluatePolynomial(polynomials[0], j.intValue()),
                SecurityUtils.evaluatePolynomial(polynomials[1], j.intValue()),
                SecurityUtils.evaluatePolynomial(polynomials[2], j.intValue())
        };
        BigInteger[] v = new BigInteger[]{polynomials[0][0], polynomials[1][0], polynomials[2][0]};
        BigInteger[] gV = Arrays.stream(v).map(x -> g.modPow(x, p)).toArray(BigInteger[]::new);

        BigInteger expectedH = Arrays.stream(gV).reduce(BigInteger::multiply).orElse(BigInteger.ZERO).mod(p);
        BigInteger expectedPartialSecretKey = Arrays.stream(u).reduce(BigInteger::add).orElse(BigInteger.ZERO).mod(q);
        BigInteger expectedPartialPublicKey = g.modPow(expectedPartialSecretKey, p);

        PartialKeyPair actualPartialKeyPair = FeldmanVSSUtils.generateKeyPair(g, q, u, gV);

        assertEquals("Public keys h are not equal", expectedH, actualPartialKeyPair.getPublicKey().getH());
        assertEquals("Partial secret keys s_j are not equal", expectedPartialSecretKey, actualPartialKeyPair.getPartialSecretKey().getSecretValue());
        assertEquals("Partial public keys h_j are not equal", expectedPartialPublicKey, actualPartialKeyPair.getPartialPublicKey());
    }

    @Test
    public void testGenerateKeyPairBig() {
        BigInteger g = valueOf(2);
        BigInteger q = valueOf(7919);
        BigInteger p = q.multiply(valueOf(2)).add(valueOf(1));
        BigInteger j = valueOf(3);

        BigInteger[][] polynomials = new BigInteger[][]{
                {valueOf(3000), valueOf(100), valueOf(2)},
                {valueOf(3000), valueOf(100), valueOf(2)},
                {valueOf(3000), valueOf(100), valueOf(2)}};

        BigInteger[] u = new BigInteger[]{
                SecurityUtils.evaluatePolynomial(polynomials[0], j.intValue()),
                SecurityUtils.evaluatePolynomial(polynomials[1], j.intValue()),
                SecurityUtils.evaluatePolynomial(polynomials[2], j.intValue())
        };
        BigInteger[] v = new BigInteger[]{polynomials[0][0], polynomials[1][0], polynomials[2][0]};
        BigInteger[] gV = Arrays.stream(v).map(x -> g.modPow(x, p)).toArray(BigInteger[]::new);

        BigInteger expectedH = Arrays.stream(gV).reduce(BigInteger::multiply).orElse(BigInteger.ZERO).mod(p);
        BigInteger expectedPartialSecretKey = Arrays.stream(u).reduce(BigInteger::add).orElse(BigInteger.ZERO).mod(q);
        BigInteger expectedPartialPublicKey = g.modPow(expectedPartialSecretKey, p);

        PartialKeyPair actualPartialKeyPair = FeldmanVSSUtils.generateKeyPair(g, q, u, gV);

        assertEquals("Public keys h are not equal", expectedH, actualPartialKeyPair.getPublicKey().getH());
        assertEquals("Partial secret keys s_j are not equal", expectedPartialSecretKey, actualPartialKeyPair.getPartialSecretKey().getSecretValue());
        assertEquals("Partial public keys h_j are not equal", expectedPartialPublicKey, actualPartialKeyPair.getPartialPublicKey());
    }
}
