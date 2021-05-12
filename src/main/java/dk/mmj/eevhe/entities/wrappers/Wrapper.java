package dk.mmj.eevhe.entities.wrappers;

/**
 * Interface for all wrappers.
 * <br>
 * Wrappers are of no apparent use, except avoiding confusing the jackson
 * {@link com.fasterxml.jackson.databind.ObjectMapper} deserializer
 *
 * @param <Type> type of content that is wrapped
 */
public interface Wrapper<Type> {

    /**
     * @return the wrapped content
     */
    Type getContent();
}
