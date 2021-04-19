package dk.mmj.eevhe.protocols.agreement.mvba;

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
     * Communicator receives string msg for BA with ID
     *
     * @param BAId BA id
     * @param msg message
     */
    void receive(String BAId, String msg);

    /**
     * Communicator receives boolean msg for BA with ID
     *
     * @param BAId BA id
     * @param msg message
     */
    void receive(String BAId, Boolean msg);

    /**
     * Registers a receiving handler for Strings.
     * <br>
     * Depending on implementation, only one, or multiple, can be registered at a time
     * @param onReceived handler for received string BAs
     */
    void registerOnReceivedString(BiConsumer<String, String> onReceived);

    /**
     * Registers a receiving handler for booleans.
     * <br>
     * Depending on implementation, only one, or multiple, can be registered at a time
     *
     * @param onReceived handler for received boolean BAs
     */
    void registerOnReceivedBoolean(BiConsumer<String, Boolean> onReceived);

}
