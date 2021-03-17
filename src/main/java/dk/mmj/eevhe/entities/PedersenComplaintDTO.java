package dk.mmj.eevhe.entities;

import java.util.Objects;

@SuppressWarnings("unused")
public class PedersenComplaintDTO {
    private int senderId;
    private int targetId;

    public PedersenComplaintDTO() {
    }

    public PedersenComplaintDTO(int senderId, int targetId) {
        this.senderId = senderId;
        this.targetId = targetId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PedersenComplaintDTO that = (PedersenComplaintDTO) o;
        return senderId == that.senderId && targetId == that.targetId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderId, targetId);
    }

    @Override
    public String toString() {
        return "ComplaintDTO{" +
                "senderId=" + senderId +
                ", targetId=" + targetId +
                '}';
    }
}
