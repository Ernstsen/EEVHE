package dk.mmj.eevhe.entities.wrappers;

import dk.mmj.eevhe.entities.PersistedBallot;

import java.util.List;
import java.util.Objects;

public class BallotWrapper implements Wrapper<List<PersistedBallot>> {
    private List<PersistedBallot> content;

    public BallotWrapper() {
    }

    public BallotWrapper(List<PersistedBallot> content) {
        this.content = content;
    }

    public List<PersistedBallot> getContent() {
        return content;
    }

    public void setContent(List<PersistedBallot> content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BallotWrapper that = (BallotWrapper) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "BallotWrapper{" +
                "content=" + content +
                '}';
    }
}
