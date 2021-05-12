package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

/**
 * DTO for ciphertext
 */
@SuppressWarnings("JavaDocs, unused")
public class CipherText {
    private BigInteger c;
    private BigInteger d;

    /**
     * Empty {@link com.fasterxml.jackson.databind.ObjectMapper} constructor
     */
    public CipherText() {
    }

    public CipherText(BigInteger c, BigInteger d) {
        this.c = c;
        this.d = d;
    }

    public BigInteger getC() {
        return c;
    }

    public void setC(BigInteger c) {
        this.c = c;
    }

    public BigInteger getD() {
        return d;
    }

    public void setD(BigInteger d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return "CipherText{" +
                "c=" + c +
                ", d=" + d +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CipherText that = (CipherText) o;
        return Objects.equals(c, that.c) && Objects.equals(d, that.d);
    }

    @Override
    public int hashCode() {
        return Objects.hash(c, d);
    }
}
