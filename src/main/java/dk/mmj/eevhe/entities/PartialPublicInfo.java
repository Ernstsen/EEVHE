package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

public class PartialPublicInfo {
    private int senderId;
    private PublicKey publicKey;
    private BigInteger partialPublicKey;
    private List<Candidate> candidates;
    private long endTime;

    public PartialPublicInfo() {
    }

    public PartialPublicInfo(int senderId, PublicKey publicKey, BigInteger partialPublicKey, List<Candidate> candidates, long endTime) {
        this.senderId = senderId;
        this.publicKey = publicKey;
        this.partialPublicKey = partialPublicKey;
        this.candidates = candidates;
        this.endTime = endTime;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public BigInteger getPartialPublicKey() {
        return partialPublicKey;
    }

    public void setPartialPublicKey(BigInteger partialPublicKey) {
        this.partialPublicKey = partialPublicKey;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialPublicInfo that = (PartialPublicInfo) o;
        return senderId == that.senderId && endTime == that.endTime && Objects.equals(publicKey, that.publicKey) && Objects.equals(partialPublicKey, that.partialPublicKey) && Objects.equals(candidates, that.candidates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderId, publicKey, partialPublicKey, candidates, endTime);
    }
}
