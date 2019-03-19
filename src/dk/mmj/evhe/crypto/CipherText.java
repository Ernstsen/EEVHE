package dk.mmj.evhe.crypto;

import java.math.BigInteger;

@SuppressWarnings({"JavaDocs, unused, WeakerAccess"})
public class CipherText {
    private BigInteger c;
    private BigInteger d;

    /**
     * Empty objectmapper constructor
     */
    public CipherText() {
    }

    CipherText(BigInteger c, BigInteger d) {
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
}