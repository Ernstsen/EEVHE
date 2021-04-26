package dk.mmj.eevhe.protocols.agreement.mvba;

import java.util.function.BooleanSupplier;

/**
 * Composite incoming, meaning that content, identity and validity-checking does not need to come from same object
 * @param <T> type parameter for contained content
 */
public class CompositeIncoming<T> implements Incoming<T> {
    private final T content;
    private final String identifier;
    private final BooleanSupplier validityChecker;

    public CompositeIncoming(T content, String identifier, BooleanSupplier validityChecker) {
        this.content = content;
        this.identifier = identifier;
        this.validityChecker = validityChecker;
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
        return validityChecker.getAsBoolean();
    }
}
