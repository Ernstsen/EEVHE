package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigInteger;
import java.util.Objects;

@SuppressWarnings("unused")
public class PublicKey {
    private BigInteger h, g, q;

    /**
     * Unused object mapper constructor
     */
    @SuppressWarnings("unused")
    private PublicKey() {
    }

    public PublicKey(BigInteger h, BigInteger g, BigInteger q) {
        this.g = g;
        this.q = q;
        this.h = h;
    }

    public BigInteger getH() {
        return h;
    }

    public void setH(BigInteger h) {
        this.h = h;
    }

    public BigInteger getG() {
        return g;
    }

    public void setG(BigInteger g) {
        this.g = g;
    }

    public BigInteger getQ() {
        return q;
    }

    public void setQ(BigInteger q) {
        this.q = q;
    }

    @JsonIgnore
    public BigInteger getP() {
        return q.multiply(BigInteger.valueOf(2)).add(BigInteger.ONE);
    }

    @Override
    public String toString() {
        return "PublicKey{" +
                "h=" + h +
                ", g=" + g +
                ", q=" + q +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicKey publicKey = (PublicKey) o;
        return Objects.equals(h, publicKey.h) &&
                Objects.equals(g, publicKey.g) &&
                Objects.equals(q, publicKey.q);
    }

    @Override
    public int hashCode() {
        return Objects.hash(h, g, q);
    }
}
