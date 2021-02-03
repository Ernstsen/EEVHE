package dk.mmj.eevhe.entities;

import java.util.List;

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
}
