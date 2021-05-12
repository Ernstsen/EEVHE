package dk.mmj.eevhe.protocols.connectors.interfaces;

public interface BBPeerCommunicator {
    /**
     * Sends message using Byzantine agreement protocol
     *
     * @param baId    Identifier for Byzantine Agreement protocol
     * @param message Message to be sent
     */
    void sendMessageBA(String baId, String message);

    /**
     * Sends message using Byzantine agreement protocol
     *
     * @param baId    Identifier for Byzantine Agreement protocol
     * @param message Message to be sent
     */
    void sendMessageBA(String baId, Boolean message);

    /**
     * Sends message to individual peers as part of a broadcast protocol
     *
     * @param message Message to be sent
     */
    void sendMessageBroadcast(String message);
}
