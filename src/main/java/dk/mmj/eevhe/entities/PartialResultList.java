package dk.mmj.eevhe.entities;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class PartialResultList {
    private List<PartialResult> results;
    private int voteCount;
    private int daId;

    public PartialResultList(List<PartialResult> results, int voteCount, int daId) {
        this.results = results;
        this.voteCount = voteCount;
        this.daId = daId;
    }

    public PartialResultList() {
    }

    public List<PartialResult> getResults() {
        return results;
    }

    public void setResults(List<PartialResult> results) {
        this.results = results;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public int getDaId() {
        return daId;
    }

    protected void setDaId(int daId) {
        this.daId = daId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialResultList that = (PartialResultList) o;
        return voteCount == that.voteCount && daId == that.daId && Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(results, voteCount, daId);
    }

    @Override
    public String toString() {
        return "PartialResultList{" +
                "results=" + results +
                ", voteCount=" + voteCount +
                ", daId=" + daId +
                '}';
    }
}
