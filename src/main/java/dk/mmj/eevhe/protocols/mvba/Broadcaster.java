package dk.mmj.eevhe.protocols.mvba;

public interface Broadcaster {
    /**
     * Broadcasts a given message for a given broadcast specified by its unique id.
     *
     * @param broadcastId id of the broadcast, must be unique
     * @param message message to be broadcast
     */
    void broadcast(String broadcastId, String message);
}
