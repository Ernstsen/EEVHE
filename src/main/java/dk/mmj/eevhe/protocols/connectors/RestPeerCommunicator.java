package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

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
    private final AsymmetricKeyParameter sk;

    /**
     * Creates a peerCommunicator communicating through a webtarget
     *
     * @param target webTarget for communication
     * @param sk     secretKey used in signing outgoing messages
     */
    public RestPeerCommunicator(WebTarget target, AsymmetricKeyParameter sk) {
        this.target = target;
        this.sk = sk;
    }

    @Override
    public void sendSecret(PartialSecretMessageDTO message) {
        Entity<SignedEntity<PartialSecretMessageDTO>> partialSecretEntity = Entity.entity(new SignedEntity<>(message, sk), MediaType.APPLICATION_JSON);

        Response resp = target.path("partialSecret").request().post(partialSecretEntity);
        if (!(resp.getStatus() == 204)) {
            logger.error("Failed to post secret to with status= " + resp.getStatus() + " webtarget: " + target);
        }
    }
}
