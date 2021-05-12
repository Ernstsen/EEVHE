package dk.mmj.eevhe.entities.wrappers;

import dk.mmj.eevhe.entities.FeldmanComplaintDTO;
import dk.mmj.eevhe.entities.SignedEntity;

import java.util.List;
import java.util.Objects;

public class FeldmanComplaintWrapper implements Wrapper<List<SignedEntity<FeldmanComplaintDTO>>> {
    private List<SignedEntity<FeldmanComplaintDTO>> content;

    @SuppressWarnings("unused")
    public FeldmanComplaintWrapper() {
    }

    public FeldmanComplaintWrapper(List<SignedEntity<FeldmanComplaintDTO>> content) {
        this.content = content;
    }

    public List<SignedEntity<FeldmanComplaintDTO>> getContent() {
        return content;
    }

    public void setContent(List<SignedEntity<FeldmanComplaintDTO>> content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeldmanComplaintWrapper that = (FeldmanComplaintWrapper) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "FeldmanComplaintWrapper{" +
                "content=" + content +
                '}';
    }
}
