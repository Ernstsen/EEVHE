package dk.mmj.eevhe.entities;

import java.util.Date;

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
}
