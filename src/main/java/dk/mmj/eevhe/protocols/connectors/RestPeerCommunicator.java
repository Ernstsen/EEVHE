package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Rest implementation of PeerCommunicator
 */
public class RestPeerCommunicator implements PeerCommunicator {
    private final Logger logger = LogManager.getLogger(RestPeerCommunicator.class);
    private final WebTarget target;

    /**
     * Creates a peerCommunicator communicating through a webtarget
     *
     * @param target webTarget for communication
     */
    public RestPeerCommunicator(WebTarget target) {
        this.target = target;
    }

    @Override
    public void sendSecret(PartialSecretMessageDTO message) {
        Entity<PartialSecretMessageDTO> partialSecretEntity = Entity.entity(message, MediaType.APPLICATION_JSON);

        Response resp = target.path("partialSecret").request().post(partialSecretEntity);
        if (!(resp.getStatus() == 204)) {
            logger.error("Failed to post secret to with status= " + resp.getStatus() + " webtarget: " + target);
        }

    }
}
