package dk.mmj.eevhe.protocols.connectors.interfaces;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;

import java.util.List;

/**
 * Models messages sent to a peer
 * <br>
 * This interface allows the DKG implementation to access information sent directly to it.
 * <br>
 * Broadcasts must be read through {@link DKGBroadcaster}
 */
public interface DKGIncomingChannel {


    /**
     * Delivers all messages sent to this instance.
     *
     * @return all secrets received
     */
    List<PartialSecretMessageDTO> receiveSecrets();
}
