package dk.mmj.eevhe.crypto;

import org.junit.Test;

import java.math.BigInteger;

import static dk.mmj.eevhe.crypto.PedersenVSSUtils.*;
import static java.math.BigInteger.valueOf;
import static org.junit.Assert.*;

public class TestPedersenVSSUtils {
    @Test
    public void testComputeCoefficientCommitments() {
        BigInteger[] polynomial1 = new BigInteger[]{valueOf(11), valueOf(2), valueOf(2)};
        BigInteger[] polynomial2 = new BigInteger[]{valueOf(8), valueOf(4), valueOf(3)};
        BigInteger g = valueOf(2);
        BigInteger p = valueOf(7919);
        BigInteger e = generateElementInSubgroup(g, p);
        BigInteger[] actualResult = computeCoefficientCommitments(g, e, p, polynomial1, polynomial2);

        BigInteger[] expectedResult = new BigInteger[3];
        expectedResult[0] = g.modPow(polynomial1[0], p).multiply(e.modPow(polynomial2[0], p)).mod(p);
        expectedResult[1] = g.modPow(polynomial1[1], p).multiply(e.modPow(polynomial2[1], p)).mod(p);
        expectedResult[2] = g.modPow(polynomial1[2], p).multiply(e.modPow(polynomial2[2], p)).mod(p);

        assertArrayEquals("Computation of coefficient commitments failed", expectedResult, actualResult);
    }

    @Test
    public void testComputeCoefficientCommitmentsBig() {
        BigInteger[] polynomial1 = new BigInteger[]{valueOf(3000), valueOf(100), valueOf(2)};
        BigInteger[] polynomial2 = new BigInteger[]{valueOf(1773), valueOf(147), valueOf(82)};
        BigInteger g = valueOf(2);
        BigInteger p = valueOf(7919);
        BigInteger e = generateElementInSubgroup(g, p);
        BigInteger[] actualResult = computeCoefficientCommitments(g, e, p, polynomial1, polynomial2);

        BigInteger[] expectedResult = new BigInteger[3];
        expectedResult[0] = g.modPow(polynomial1[0], p).multiply(e.modPow(polynomial2[0], p)).mod(p);
        expectedResult[1] = g.modPow(polynomial1[1], p).multiply(e.modPow(polynomial2[1], p)).mod(p);
        expectedResult[2] = g.modPow(polynomial1[2], p).multiply(e.modPow(polynomial2[2], p)).mod(p);

        assertArrayEquals("Computation of coefficient commitments failed", expectedResult, actualResult);
    }

    @Test
    public void testCombineCoefficientCommitments() {
        BigInteger[] polynomial1 = new BigInteger[]{valueOf(11), valueOf(2), valueOf(2)};
        BigInteger[] polynomial2 = new BigInteger[]{valueOf(8), valueOf(4), valueOf(3)};

        BigInteger g = valueOf(2);
        BigInteger q = valueOf(7919);
        BigInteger p = q.multiply(valueOf(2)).add(valueOf(1));
        BigInteger e = generateElementInSubgroup(g, p);
        BigInteger j = valueOf(1);

        BigInteger[] coefficientCommitments = computeCoefficientCommitments(g, e, p, polynomial1, polynomial2);

        BigInteger actualResult = combineCoefficientCommitments(coefficientCommitments, j, p, q);

        BigInteger jExp = j.modPow(valueOf(2), q);
        BigInteger expectedResult = coefficientCommitments[0]
                .multiply(coefficientCommitments[1].modPow(j, p))
                .multiply(coefficientCommitments[2].modPow(jExp, p)).mod(p);

        assertEquals("Combining coefficient commitments failed", expectedResult, actualResult);
    }

    @Test
    public void testCombineCoefficientCommitmentsBig() {
        BigInteger[] polynomial1 = new BigInteger[]{valueOf(3000), valueOf(100), valueOf(2)};
        BigInteger[] polynomial2 = new BigInteger[]{valueOf(1773), valueOf(147), valueOf(82)};

        BigInteger g = valueOf(2);
        BigInteger q = valueOf(7919);
        BigInteger p = q.multiply(valueOf(2)).add(valueOf(1));
        BigInteger e = generateElementInSubgroup(g, p);
        BigInteger j = valueOf(1);

        BigInteger[] coefficientCommitments = computeCoefficientCommitments(g, e, p, polynomial1, polynomial2);

        BigInteger actualResult = combineCoefficientCommitments(coefficientCommitments, j, p, q);

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
        BigInteger e = generateElementInSubgroup(g, p);

        BigInteger[] polynomial1 = new BigInteger[]{valueOf(11), valueOf(2), valueOf(2)};
        BigInteger[] polynomial2 = new BigInteger[]{valueOf(8), valueOf(4), valueOf(3)};
        BigInteger[] coefficientCommitments = computeCoefficientCommitments(g, e, p, polynomial1, polynomial2);

        for (long i = 0; i < 20; i++) {
            BigInteger j = valueOf(i);
            BigInteger u1 = SecurityUtils.evaluatePolynomial(polynomial1, j.intValue());
            BigInteger u2 = SecurityUtils.evaluatePolynomial(polynomial2, j.intValue());
            assertTrue("Commitments should be respected ",verifyCommitmentRespected(g, e, u1, u2, coefficientCommitments, j, p, q));
        }
    }

    @Test
    public void testVerifyCommitmentRespectedBig() {
        BigInteger g = valueOf(2);
        BigInteger q = valueOf(7919);
        BigInteger p = q.multiply(valueOf(2)).add(valueOf(1));
        BigInteger e = generateElementInSubgroup(g, p);

        BigInteger[] polynomial1 = new BigInteger[]{valueOf(3000), valueOf(100), valueOf(2)};
        BigInteger[] polynomial2 = new BigInteger[]{valueOf(1773), valueOf(147), valueOf(82)};
        BigInteger[] coefficientCommitments = computeCoefficientCommitments(g, e, p, polynomial1, polynomial2);


        for (int i = 0; i < 3000; i++) {

            BigInteger j = valueOf(i);
            BigInteger u1 = SecurityUtils.evaluatePolynomial(polynomial1, j.intValue());
            BigInteger u2 = SecurityUtils.evaluatePolynomial(polynomial2, j.intValue());

            boolean actualResult = verifyCommitmentRespected(g, e, u1, u2, coefficientCommitments, j, p, q);

            assertTrue(actualResult);
        }
    }
}
