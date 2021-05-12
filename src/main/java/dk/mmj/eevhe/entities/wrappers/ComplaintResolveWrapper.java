package dk.mmj.eevhe.entities.wrappers;

import dk.mmj.eevhe.entities.ComplaintResolveDTO;
import dk.mmj.eevhe.entities.SignedEntity;

import java.util.List;
import java.util.Objects;

public class ComplaintResolveWrapper implements Wrapper<List<SignedEntity<ComplaintResolveDTO>>> {
    private List<SignedEntity<ComplaintResolveDTO>> content;

    @SuppressWarnings("unused")
    public ComplaintResolveWrapper() {
    }

    public ComplaintResolveWrapper(List<SignedEntity<ComplaintResolveDTO>> content) {
        this.content = content;
    }

    public List<SignedEntity<ComplaintResolveDTO>> getContent() {
        return content;
    }

    public void setContent(List<SignedEntity<ComplaintResolveDTO>> content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplaintResolveWrapper that = (ComplaintResolveWrapper) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "ComplaintResolveWrapper{" +
                "content=" + content +
                '}';
    }
}
