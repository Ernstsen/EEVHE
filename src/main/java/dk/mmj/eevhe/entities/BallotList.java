package dk.mmj.eevhe.entities;

import java.util.List;

@SuppressWarnings("JavaDocs, unused")
public class BallotList {

    private List<PersistedBallot> ballots;

    public BallotList(List<PersistedBallot> ballots) {
        this.ballots = ballots;
    }

    public BallotList() {
    }

    public List<PersistedBallot> getBallots() {
        return ballots;
    }

    public void setBallots(List<PersistedBallot> ballots) {
        this.ballots = ballots;
    }
}
