package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class PartialPublicInfo {
    private int senderId;
    private PublicKey publicKey;
    private BigInteger partialPublicKey;
    private List<Candidate> candidates;
    private long endTime;
    private String certificate;

    public PartialPublicInfo() {
    }

    public PartialPublicInfo(int senderId, PublicKey publicKey, BigInteger partialPublicKey, List<Candidate> candidates, long endTime, String certificate) {
        this.senderId = senderId;
        this.publicKey = publicKey;
        this.partialPublicKey = partialPublicKey;
        this.candidates = candidates;
        this.endTime = endTime;
        this.certificate = certificate;
    }

    public int getSenderId() {
        return senderId;
    }

    protected void setSenderId(int senderId) {
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

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialPublicInfo that = (PartialPublicInfo) o;
        return senderId == that.senderId && endTime == that.endTime && Objects.equals(publicKey, that.publicKey) && Objects.equals(partialPublicKey, that.partialPublicKey) && Objects.equals(candidates, that.candidates) && Objects.equals(certificate, that.certificate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderId, publicKey, partialPublicKey, candidates, endTime, certificate);
    }

    @Override
    public String toString() {
        return "PartialPublicInfo{" +
                "senderId=" + senderId +
                ", publicKey=" + publicKey +
                ", partialPublicKey=" + partialPublicKey +
                ", candidates=" + candidates +
                ", endTime=" + endTime +
                ", certificate='" + certificate + '\'' +
                '}';
    }
}
