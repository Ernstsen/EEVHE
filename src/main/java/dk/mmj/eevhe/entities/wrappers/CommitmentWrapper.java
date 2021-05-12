package dk.mmj.eevhe.entities.wrappers;

import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.SignedEntity;

import java.util.List;
import java.util.Objects;

public class CommitmentWrapper implements Wrapper<List<SignedEntity<CommitmentDTO>>> {

    private List<SignedEntity<CommitmentDTO>> content;

    @SuppressWarnings("unused")
    public CommitmentWrapper() {
    }

    public CommitmentWrapper(List<SignedEntity<CommitmentDTO>> content) {
        this.content = content;
    }

    public List<SignedEntity<CommitmentDTO>> getContent() {
        return content;
    }

    public void setContent(List<SignedEntity<CommitmentDTO>> content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommitmentWrapper that = (CommitmentWrapper) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "CommitmentWrapper{" +
                "content=" + content +
                '}';
    }
}
