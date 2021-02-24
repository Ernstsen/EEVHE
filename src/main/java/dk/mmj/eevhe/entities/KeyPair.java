package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

/**
 * "dumb" object for keeping a keypair
 */
public class KeyPair {
    private BigInteger secretKey;
    private PublicKey publicKey;

    public KeyPair() {
    }

    public KeyPair(BigInteger secretKey, PublicKey publicKey) {
        this.secretKey = secretKey;
        this.publicKey = publicKey;
    }

    public BigInteger getSecretKey() {
        return secretKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyPair keyPair = (KeyPair) o;
        return Objects.equals(secretKey, keyPair.secretKey) &&
                Objects.equals(publicKey, keyPair.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretKey, publicKey);
    }

    @Override
    public String toString() {
        return "KeyPair{" +
                "secretKey=" + secretKey +
                ", publicKey=" + publicKey +
                '}';
    }
}
