package dk.mmj.eevhe.protocols.mvba;

public interface Incoming<T> {

    /**
     * @return the incoming content
     */
    T getContent();

    /**
     * @return identifier for the sender
     */
    String getIdentifier();

    /**
     * Asserts whether the content is valid, and originates from the identifier it says it does.
     * <br>
     * Example is verifying cryptographic signatures
     *
     * @return whether the incoming message is valid on all fields
     */
    boolean isValid();
}
