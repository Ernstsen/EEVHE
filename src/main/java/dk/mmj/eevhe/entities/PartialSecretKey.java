package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Entity containing a partial secret key, kept by a {@link dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthority}
 */
public class PartialSecretKey {
    private BigInteger secretValue;
    private BigInteger dLogPublicValue;
    private BigInteger p;

    public PartialSecretKey() {
    }

    /**
     * @param secretValue     secret value, output of the DKG protocol
     * @param dLogPublicValue discrete log of the public value. Must be the case that partialPublicKey = g^dLogPublicValue
     * @param p               prime p
     */
    public PartialSecretKey(BigInteger secretValue, BigInteger dLogPublicValue, BigInteger p) {
        this.secretValue = secretValue;
        this.dLogPublicValue = dLogPublicValue;
        this.p = p;
    }

    public BigInteger getSecretValue() {
        return secretValue;
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getdLogPublicValue() {
        return dLogPublicValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialSecretKey that = (PartialSecretKey) o;
        return Objects.equals(secretValue, that.secretValue) && Objects.equals(dLogPublicValue, that.dLogPublicValue) && Objects.equals(p, that.p);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretValue, dLogPublicValue, p);
    }

    @Override
    public String toString() {
        return "PartialSecretKey{" +
                "secretValue=" + secretValue +
                ", dLogPublicValue=" + dLogPublicValue +
                ", p=" + p +
                '}';
    }
}
