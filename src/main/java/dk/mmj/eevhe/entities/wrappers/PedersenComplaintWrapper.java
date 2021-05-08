package dk.mmj.eevhe.entities.wrappers;

import dk.mmj.eevhe.entities.PedersenComplaintDTO;
import dk.mmj.eevhe.entities.SignedEntity;

import java.util.List;
import java.util.Objects;

public class PedersenComplaintWrapper implements Wrapper<List<SignedEntity<PedersenComplaintDTO>>> {
    private List<SignedEntity<PedersenComplaintDTO>> content;

    @SuppressWarnings("unused")
    public PedersenComplaintWrapper() {
    }

    public PedersenComplaintWrapper(List<SignedEntity<PedersenComplaintDTO>> content) {
        this.content = content;
    }

    public List<SignedEntity<PedersenComplaintDTO>> getContent() {
        return content;
    }

    public void setContent(List<SignedEntity<PedersenComplaintDTO>> content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PedersenComplaintWrapper that = (PedersenComplaintWrapper) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "PedersenComplaintWrapper{" +
                "content=" + content +
                '}';
    }
}
