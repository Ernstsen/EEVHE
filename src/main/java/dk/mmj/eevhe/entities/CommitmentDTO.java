package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * DTO object used in committing to polynomials in both {@link dk.mmj.eevhe.protocols.PedersenVSS} and
 * {@link dk.mmj.eevhe.protocols.GennaroFeldmanVSS}
 */
@SuppressWarnings("unused")
public class CommitmentDTO {
    private BigInteger[] commitment;
    private int id;
    private String protocol;

    public CommitmentDTO(BigInteger[] commitment, int id, String protocol) {
        this.commitment = commitment;
        this.id = id;
        this.protocol = protocol;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommitmentDTO that = (CommitmentDTO) o;
        return id == that.id && Arrays.equals(commitment, that.commitment) && Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, protocol);
        result = 31 * result + Arrays.hashCode(commitment);
        return result;
    }

    @Override
    public String toString() {
        return "CommitmentDTO{" +
                "commitment=" + Arrays.toString(commitment) +
                ", id=" + id +
                ", protocol='" + protocol + '\'' +
                '}';
    }
}
