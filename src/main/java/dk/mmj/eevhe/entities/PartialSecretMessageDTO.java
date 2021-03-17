package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

@SuppressWarnings("unused")
public class PartialSecretMessageDTO {
    private BigInteger partialSecret1;
    private BigInteger partialSecret2;
    private int target;
    private int sender;

    public PartialSecretMessageDTO(BigInteger val1, BigInteger val2, int target, int sender) {
        this.partialSecret1 = val1;
        this.partialSecret2 = val2;
        this.target = target;
        this.sender = sender;
    }

    public PartialSecretMessageDTO() {
    }

    public BigInteger getPartialSecret1() {
        return partialSecret1;
    }

    public void setPartialSecret1(BigInteger partialSecret) {
        this.partialSecret1 = partialSecret;
    }

    public BigInteger getPartialSecret2() {
        return partialSecret2;
    }

    public void setPartialSecret2(BigInteger partialSecret) {
        this.partialSecret2 = partialSecret;
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
        return Objects.equals(partialSecret1, that.partialSecret1) &&
                Objects.equals(partialSecret2, that.partialSecret2) &&
                Objects.equals(target, that.target) &&
                Objects.equals(sender, that.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partialSecret1, partialSecret2, target, sender);
    }

    @Override
    public String toString() {
        return "PartialSecretMessageDTO{" +
                "partialSecret1=" + partialSecret1 +
                ", partialSecret2=" + partialSecret2 +
                ", target=" + target +
                ", sender=" + sender +
                '}';
    }
}
