package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

/**
 * DTO for a complaint in the {@link dk.mmj.eevhe.protocols.GennaroFeldmanVSS} protocol
 */
@SuppressWarnings("unused")
public class FeldmanComplaintDTO {
    private int senderId;
    private int targetId;
    private BigInteger val1;
    private BigInteger val2;

    public FeldmanComplaintDTO() {
    }

    public FeldmanComplaintDTO(int senderId, int targetId, BigInteger val1, BigInteger val2) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.val1 = val1;
        this.val2 = val2;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public BigInteger getVal1() {
        return val1;
    }

    public BigInteger getVal2() {
        return val2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeldmanComplaintDTO that = (FeldmanComplaintDTO) o;
        return senderId == that.senderId &&
                targetId == that.targetId &&
                Objects.equals(val1, that.val1) &&
                Objects.equals(val2, that.val2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderId, targetId, val1, val2);
    }

    @Override
    public String toString() {
        return "FeldmanComplaintDTO{" +
                "senderId=" + senderId +
                ", targetId=" + targetId +
                ", val1=" + val1 +
                ", val2=" + val2 +
                '}';
    }
}
