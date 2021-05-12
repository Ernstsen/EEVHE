package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Class containing primes p and q
 * where p is a so-called safe prime due to its construction: p = 2q + 1
 */
@SuppressWarnings("unused")
public class PrimePair {
    private BigInteger p;
    private BigInteger q;

    public PrimePair() {
    }

    public PrimePair(BigInteger p, BigInteger q) {
        this.p = p;
        this.q = q;
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getQ() {
        return q;
    }

    protected PrimePair setP(BigInteger p) {
        this.p = p;
        return this;
    }

    protected PrimePair setQ(BigInteger q) {
        this.q = q;
        return this;
    }

    @Override
    public String toString() {
        return "PrimePair{" +
                "p=" + p +
                ", q=" + q +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimePair primePair = (PrimePair) o;
        return Objects.equals(p, primePair.p) &&
                Objects.equals(q, primePair.q);
    }

    @Override
    public int hashCode() {
        return Objects.hash(p, q);
    }
}
