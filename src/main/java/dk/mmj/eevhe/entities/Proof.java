package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

/**
 * DTO class for proof that votes is either 0 or 1
 */
public class Proof {
    private BigInteger e0;
    private BigInteger e1;
    private BigInteger z0;
    private BigInteger z1;

    public Proof() {
    }

    public Proof(BigInteger e0, BigInteger e1, BigInteger z0, BigInteger z1) {
        this.e0 = e0;
        this.e1 = e1;
        this.z0 = z0;
        this.z1 = z1;
    }

    public BigInteger getE0() {
        return e0;
    }

    public void setE0(BigInteger e0) {
        this.e0 = e0;
    }

    public BigInteger getE1() {
        return e1;
    }

    public void setE1(BigInteger e1) {
        this.e1 = e1;
    }

    public BigInteger getZ0() {
        return z0;
    }

    public void setZ0(BigInteger z0) {
        this.z0 = z0;
    }

    public BigInteger getZ1() {
        return z1;
    }

    public void setZ1(BigInteger z1) {
        this.z1 = z1;
    }

    @Override
    public String toString() {
        return "Proof{" +
                "e0=" + e0 +
                ", e1=" + e1 +
                ", z0=" + z0 +
                ", z1=" + z1 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proof proof = (Proof) o;
        return Objects.equals(e0, proof.e0) &&
                Objects.equals(e1, proof.e1) &&
                Objects.equals(z0, proof.z0) &&
                Objects.equals(z1, proof.z1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(e0, e1, z0, z1);
    }
}
