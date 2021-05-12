package dk.mmj.eevhe.protocols.agreement.broadcast;

import java.util.function.Consumer;

public interface BroadcastManager {
    /**
     * Broadcasts a given message for a given broadcast specified by its unique id.
     *
     * @param broadcastId id of the broadcast, must be unique
     * @param message     message to be broadcast
     */
    void broadcast(String broadcastId, String message);

    /**
     * Registers a function to be executed when this manager receives a broadcast from its peers
     *
     * @param onReceived handle to inform system that a message has been received
     */
    void registerOnReceived(Consumer<String> onReceived);
}
