package dk.mmj.eevhe.entities;

import java.util.Objects;

/**
 * Simple DTO object for casting votes
 */
@SuppressWarnings("JavaDocs, unused")
public class CandidateVoteDTO {
    private CipherText cipherText;
    private String id;
    private Proof proof;

    CandidateVoteDTO() {
    }

    public CandidateVoteDTO(CipherText cipherText, String id, Proof proof) {
        this.cipherText = cipherText;
        this.id = id;
        this.proof = proof;
    }

    public CipherText getCipherText() {
        return cipherText;
    }

    public void setCipherText(CipherText cipherText) {
        this.cipherText = cipherText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Proof getProof() {
        return proof;
    }

    public void setProof(Proof proof) {
        this.proof = proof;
    }

    @Override
    public String toString() {
        return "CandidateVoteDTO{" +
                "cipherText=" + cipherText +
                ", id='" + id + '\'' +
                ", proof=" + proof +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateVoteDTO that = (CandidateVoteDTO) o;
        return Objects.equals(cipherText, that.cipherText) &&
                Objects.equals(id, that.id) &&
                Objects.equals(proof, that.proof);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cipherText, id, proof);
    }
}
