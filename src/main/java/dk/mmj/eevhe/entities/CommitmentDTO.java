package dk.mmj.eevhe.entities;

import java.math.BigInteger;

public class CommitmentDTO {
    private BigInteger[] commitment;
    private int id;

    public CommitmentDTO(BigInteger[] commitment, int id) {
        this.commitment = commitment;
        this.id = id;
    }

    public CommitmentDTO() {
    }

    public BigInteger[] getCommitment() {
        return commitment;
    }

    public void setCommitment(BigInteger[] commitment) {
        this.commitment = commitment;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
