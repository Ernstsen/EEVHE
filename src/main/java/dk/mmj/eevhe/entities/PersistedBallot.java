package dk.mmj.eevhe.entities;

import java.util.Date;

public class PersistedBallot extends BallotDTO {
    private Date ts;

    public PersistedBallot() {
    }

    public PersistedBallot(BallotDTO ballotDTO) {
        super(ballotDTO.getCandidateVotes(), ballotDTO.getId(), ballotDTO.getSumIsOneProof());
        this.ts = new Date();
    }

    public Date getTs() {
        return ts;
    }

    public PersistedBallot setTs(Date ts) {
        this.ts = ts;
        return this;
    }
}
