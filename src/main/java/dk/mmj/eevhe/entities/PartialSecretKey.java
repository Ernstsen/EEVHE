package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Entity containing a partial secret key, kept by a {@link dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthority}
 */
public class PartialSecretKey {
    private BigInteger secretValue;
    private BigInteger p;

    public PartialSecretKey() {
    }

    public PartialSecretKey(BigInteger secretValue, BigInteger p) {
        this.secretValue = secretValue;
        this.p = p;
    }

    public BigInteger getSecretValue() {
        return secretValue;
    }

    public BigInteger getP() {
        return p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialSecretKey that = (PartialSecretKey) o;
        return Objects.equals(secretValue, that.secretValue) &&
                Objects.equals(p, that.p);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretValue, p);
    }

    @Override
    public String toString() {
        return "PartialSecretKey{" +
                "secretValue=" + secretValue +
                ", p=" + p +
                '}';
    }
}
