package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

@SuppressWarnings("unused")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommitmentDTO that = (CommitmentDTO) o;
        return id == that.id && Arrays.equals(commitment, that.commitment);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id);
        result = 31 * result + Arrays.hashCode(commitment);
        return result;
    }
}
