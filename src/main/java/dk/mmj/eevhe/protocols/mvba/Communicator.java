package dk.mmj.eevhe.protocols.mvba;

import java.util.function.BiConsumer;

/**
 * Communicator used in Byzantine Agreement Protocols
 */
public interface Communicator {

    /**
     * Transmits string for BA
     *
     * @param BAId BA id
     * @param msg  message
     */
    void send(String BAId, String msg);

    /**
     * Transmits boolean for BA
     *
     * @param BAId BA id
     * @param msg  message
     */
    void send(String BAId, Boolean msg);

    /**
     * @param onReceived handler for received string BAs
     */
    void registerOnReceivedString(BiConsumer<String, String> onReceived);

    /**
     * @param onReceived handler for received boolean BAs
     */
    void registerOnReceivedBoolean(BiConsumer<String, Boolean> onReceived);

}
