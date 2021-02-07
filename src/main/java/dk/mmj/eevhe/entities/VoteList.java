package dk.mmj.eevhe.entities;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("JavaDocs, unused")
@Deprecated
public class VoteList {

    private List<PersistedVote> votes;

    public VoteList(List<PersistedVote> votes) {
        this.votes = votes;
    }

    public VoteList() {
    }

    public List<PersistedVote> getVotes() {
        return votes;
    }

    public void setVotes(List<PersistedVote> votes) {
        this.votes = votes;
    }

    @Override
    public String toString() {
        return "VoteList{" +
                "votes=" + votes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoteList voteList = (VoteList) o;
        return Objects.equals(votes, voteList.votes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(votes);
    }
}
