package dk.mmj.eevhe.protocols.connectors.interfaces;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;


/**
 * Enables sending messages to a single peer, in a DKG protocol
 */
public interface PeerCommunicator {

    /**
     * Sends a secret value to the communicator
     *
     * @param message secret to be sent to peer
     */
    void sendSecret(PartialSecretMessageDTO message);

}
