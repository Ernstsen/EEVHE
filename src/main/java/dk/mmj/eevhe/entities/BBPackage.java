package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Deprecated
/**
 * @deprecated use SignedEntity stuff instead
 */
public class BBPackage<T> {
    private static final ObjectMapper mapper = new ObjectMapper();
    private T content;

    public BBPackage(T content) {
        this.content = content;
    }

    public BBPackage(){}

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    /**
     * Returns a JSON string representation of the Content object
     *
     * @return Json string representation
     */
    public String getContentAsString() {
        try {
            return mapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize content object.", e);
        }
    }
}
