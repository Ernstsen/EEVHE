package dk.mmj.eevhe.protocols.agreement.mvba;

public class IncomingTestImpl<T> implements Incoming<T> {

    private final T content;
    private final String identifier;
    private final boolean isValid;

    public IncomingTestImpl(T content, String identifier, boolean isValid) {
        this.content = content;
        this.identifier = identifier;
        this.isValid = isValid;
    }

    @Override
    public T getContent() {
        return content;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }
}
