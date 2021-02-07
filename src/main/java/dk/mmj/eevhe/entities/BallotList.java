package dk.mmj.eevhe.entities;

import java.util.List;
import java.util.Objects;

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

    @Override
    public String toString() {
        return "BallotList{" +
                "ballots=" + ballots +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BallotList that = (BallotList) o;
        return Objects.equals(ballots, that.ballots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ballots);
    }
}
