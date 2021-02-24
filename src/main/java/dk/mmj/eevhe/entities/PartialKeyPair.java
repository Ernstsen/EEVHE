package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

public class PartialKeyPair {
    private BigInteger partialSecretKey;
    private BigInteger partialPublicKey;
    private PublicKey publicKey;

    public PartialKeyPair(BigInteger partialSecretKey, BigInteger partialPublicKey, PublicKey publicKey) {
        this.partialSecretKey = partialSecretKey;
        this.partialPublicKey = partialPublicKey;
        this.publicKey = publicKey;
    }

    public PartialKeyPair() {
    }

    public BigInteger getPartialSecretKey() {
        return partialSecretKey;
    }

    public void setPartialSecretKey(BigInteger partialSecretKey) {
        this.partialSecretKey = partialSecretKey;
    }

    public BigInteger getPartialPublicKey() {
        return partialPublicKey;
    }

    public void setPartialPublicKey(BigInteger partialPublicKey) {
        this.partialPublicKey = partialPublicKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialKeyPair that = (PartialKeyPair) o;
        return Objects.equals(partialSecretKey, that.partialSecretKey) && Objects.equals(partialPublicKey, that.partialPublicKey) && Objects.equals(publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partialSecretKey, partialPublicKey, publicKey);
    }

    @Override
    public String toString() {
        return "PartialKeyPair{" +
                "partialSecretKey=" + partialSecretKey +
                ", partialPublicKey=" + partialPublicKey +
                ", publicKey=" + publicKey +
                '}';
    }
}
