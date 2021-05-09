package dk.mmj.eevhe.entities.wrappers;

import java.util.List;
import java.util.Objects;

public class StringListWrapper {
    private List<String> content;

    @SuppressWarnings("unused")
    public StringListWrapper() {
    }

    public StringListWrapper(List<String> content) {
        this.content = content;
    }

    public List<String> getContent() {
        return content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringListWrapper that = (StringListWrapper) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "StringListWrapper{" +
                "content=" + content +
                '}';
    }
}
