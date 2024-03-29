package dk.mmj.eevhe.protocols.interfaces;

import java.math.BigInteger;

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
     */
    void handleReceivedValues();

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
     * Combines all relevant data and outputs partial secret
     *
     * @return partial secret
     */
    BigInteger output();

}
