package dk.mmj.eevhe.entities;

import dk.mmj.eevhe.server.bulletinboard.BulletinBoardState;

import java.util.Date;
import java.util.Objects;

public class PersistedBallot extends BallotDTO implements BulletinBoardUpdatable {
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

    @Override
    public String toString() {
        return "PersistedBallot{" +
                "ts=" + ts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PersistedBallot that = (PersistedBallot) o;
        return Objects.equals(ts, that.ts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ts);
    }

    @Override
    public void update(BulletinBoardState bb) {
        boolean exists = bb.getBallots().stream().anyMatch(this::isSameBallot);
        if(!exists){
            bb.addBallot(this);
        }
    }

    private boolean isSameBallot(BallotDTO that){
        return Objects.equals(this.getCandidateVotes(), that.getCandidateVotes()) &&
                Objects.equals(this.getId(), that.getId()) &&
                Objects.equals(this.getSumIsOneProof(), that.getSumIsOneProof());
    }
}
