package dk.mmj.eevhe.protocols;


import dk.mmj.eevhe.entities.PartialKeyPair;

/**
 * Interface for proper encapsulation of distributed key-generation behaviour
 * <br>
 * All methods must be called in the proper order,
 * however they are split into different steps as to allow waiting time, and let other players act/react between steps.
 */
public interface VSS {

    /**
     * Initializes protocol
     * <br>
     * Will often create the secret, and initialize any private state.
     * <br>
     * This method will dispatch the first messages, such as broadcasting commitments, or send secret values to peers
     */
    void startProtocol();

    /**
     * Second step in the protocol
     * <br>
     * Handles values received from peers, and which has been broadcasted.
     * <br>
     * Will often verify received values, using commitments made by other participants
     *
     * @return true if method was run to completion, false to signal retry, as not enough data was received
     */
    boolean handleReceivedValues();

    /**
     * Third step in the protocol
     * <br>
     * Handles complaints. Will often be responding to complaints about self.
     */
    void handleComplaints();

    /**
     * Fourth step in the protocol
     * <br>
     * Applies other peers complaint-resolves, or disqualifies them if the resolve is invalid
     */
    void applyResolves();

    /**
     * Fifth and final step in the protocol
     * <br>
     * Combines all relevant data and outputs keys
     *
     * @return partial secret-key, partial public-key and public key
     */
    PartialKeyPair createKeys();

}
