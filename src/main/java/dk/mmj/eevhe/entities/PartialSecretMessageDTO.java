package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

@SuppressWarnings("unused")
public class PartialSecretMessageDTO {
    private BigInteger partialSecret;
    private Integer target;
    private Integer sender;

    public PartialSecretMessageDTO(BigInteger partialSecret, Integer target, Integer sender) {
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

    public Integer getTarget() {
        return target;
    }

    public void setTarget(Integer target) {
        this.target = target;
    }

    public Integer getSender() {
        return sender;
    }

    public void setSender(Integer sender) {
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
