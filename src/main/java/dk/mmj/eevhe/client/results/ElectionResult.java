package dk.mmj.eevhe.client.results;

import java.util.List;
import java.util.Objects;

public class ElectionResult {

    private List<Integer> candidateVotes;
    private int votesTotal;

    public ElectionResult(List<Integer> candidateVotes, int votesTotal) {
        this.candidateVotes = candidateVotes;
        this.votesTotal = votesTotal;
    }

    public ElectionResult() {
    }

    public List<Integer> getCandidateVotes() {
        return candidateVotes;
    }

    public void setCandidateVotes(List<Integer> candidateVotes) {
        this.candidateVotes = candidateVotes;
    }

    public int getVotesTotal() {
        return votesTotal;
    }

    public void setVotesTotal(int votesTotal) {
        this.votesTotal = votesTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElectionResult that = (ElectionResult) o;
        return votesTotal == that.votesTotal && Objects.equals(candidateVotes, that.candidateVotes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidateVotes, votesTotal);
    }

    @Override
    public String toString() {
        return "ElectionResult{" +
                "candidateVotes=" + candidateVotes +
                ", votesTotal=" + votesTotal +
                '}';
    }
}
