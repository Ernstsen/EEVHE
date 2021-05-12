package dk.mmj.eevhe.entities.wrappers;

import dk.mmj.eevhe.entities.PartialResultList;
import dk.mmj.eevhe.entities.SignedEntity;

import java.util.List;
import java.util.Objects;

public class PartialResultWrapper implements Wrapper<List<SignedEntity<PartialResultList>>> {
    private List<SignedEntity<PartialResultList>> content;

    @SuppressWarnings("unused")
    public PartialResultWrapper() {
    }

    public PartialResultWrapper(List<SignedEntity<PartialResultList>> content) {
        this.content = content;
    }

    public List<SignedEntity<PartialResultList>> getContent() {
        return content;
    }

    public void setContent(List<SignedEntity<PartialResultList>> content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialResultWrapper that = (PartialResultWrapper) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "PartialResultWrapper{" +
                "content=" + content +
                '}';
    }
}
