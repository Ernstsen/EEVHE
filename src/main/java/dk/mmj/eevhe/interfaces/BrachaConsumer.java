package dk.mmj.eevhe.interfaces;

import dk.mmj.eevhe.entities.SignedEntity;

import java.util.function.BiConsumer;

/**
 * Functional interface for passing around the bracha broadcast pointer
 */
public interface BrachaConsumer extends BiConsumer<SignedEntity<String>, String> {
}
