package dk.mmj.eevhe.entities;

import java.util.Date;
import java.util.Objects;

/**
 * {@link CandidateVoteDTO} with timestamp
 * @deprecated use {@link PersistedBallot} instead
 */
@SuppressWarnings("unused, JavaDocs")
@Deprecated
public class PersistedVote extends CandidateVoteDTO {

    private Date ts;

    public PersistedVote() {
    }

    public PersistedVote(CipherText cipherText, String id, Proof proof, Date ts) {
        super(cipherText, id, proof);
        this.ts = ts;
    }

    public PersistedVote(CandidateVoteDTO candidateVoteDTO) {
        super(candidateVoteDTO.getCipherText(), candidateVoteDTO.getId(), candidateVoteDTO.getProof());
        this.ts = new Date();
    }

    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

    @Override
    public String toString() {
        return "PersistedVote{" +
                "ts=" + ts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PersistedVote that = (PersistedVote) o;
        return Objects.equals(ts, that.ts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ts);
    }
}
