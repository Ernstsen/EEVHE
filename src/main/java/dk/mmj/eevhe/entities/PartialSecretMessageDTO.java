package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

@SuppressWarnings("unused")
public class PartialSecretMessageDTO {
    private BigInteger partialSecret;
    private int target;
    private int sender;

    public PartialSecretMessageDTO(BigInteger partialSecret, int target, int sender) {
        this.partialSecret = partialSecret;
        this.target = target;
        this.sender = sender;
    }

    public PartialSecretMessageDTO() {
    }

    public BigInteger getPartialSecret() {
        return partialSecret;
    }

    public void setPartialSecret(BigInteger partialSecret) {
        this.partialSecret = partialSecret;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialSecretMessageDTO that = (PartialSecretMessageDTO) o;
        return Objects.equals(partialSecret, that.partialSecret) &&
                Objects.equals(target, that.target) &&
                Objects.equals(sender, that.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partialSecret, target, sender);
    }

    @Override
    public String toString() {
        return "PartialSecretMessageDTO{" +
                "partialSecret=" + partialSecret +
                ", target=" + target +
                ", sender=" + sender +
                '}';
    }
}
