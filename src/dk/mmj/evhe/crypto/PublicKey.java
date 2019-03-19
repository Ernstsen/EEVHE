package dk.mmj.evhe.crypto;

import java.math.BigInteger;

public class PublicKey {
    private BigInteger h, g, q;

    /**
     * Unused object mapper constructor
     */
    @SuppressWarnings("unused")
    private PublicKey() {
    }

    PublicKey(BigInteger h, BigInteger g, BigInteger q) {
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
}