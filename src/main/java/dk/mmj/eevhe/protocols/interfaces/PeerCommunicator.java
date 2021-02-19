package dk.mmj.eevhe.protocols.interfaces;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;


/**
 * Enables sending messages to a single peer, in a DKG protocol
 */
public interface PeerCommunicator {

    /**
     * Sends a secret value to the communicator
     */
    void sendSecret(PartialSecretMessageDTO message);

}
