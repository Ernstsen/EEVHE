package dk.mmj.eevhe.entities;

import java.math.BigInteger;

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
}
