package dk.mmj.eevhe.entities.wrappers;

import dk.mmj.eevhe.entities.PartialPublicInfo;
import dk.mmj.eevhe.entities.SignedEntity;

import java.util.List;
import java.util.Objects;

public class PublicInfoWrapper implements Wrapper<List<SignedEntity<PartialPublicInfo>>> {
    private List<SignedEntity<PartialPublicInfo>> content;

    @SuppressWarnings("unused")
    public PublicInfoWrapper() {
    }

    public PublicInfoWrapper(List<SignedEntity<PartialPublicInfo>> content) {
        this.content = content;
    }

    public List<SignedEntity<PartialPublicInfo>> getContent() {
        return content;
    }

    public void setContent(List<SignedEntity<PartialPublicInfo>> content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "PublicInfoReturnWrap{" +
                "state=" + content +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicInfoWrapper that = (PublicInfoWrapper) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
}
