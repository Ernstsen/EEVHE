package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.BAMessage;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.protocols.connectors.interfaces.BBPeerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class RestBBPeerCommunicator implements BBPeerCommunicator {
    private static final Logger logger = LogManager.getLogger(RestBBPeerCommunicator.class);
    private final WebTarget target;
    private final AsymmetricKeyParameter sk;
    private final String id;

    /**
     * Creates a peerCommunicator communicating through a webtarget
     *
     * @param target webTarget for communication
     * @param sk     secretKey used in signing outgoing messages
     * @param id     identifier for sender
     */
    public RestBBPeerCommunicator(WebTarget target, AsymmetricKeyParameter sk, String id) {
        this.target = target;
        this.sk = sk;
        this.id = id;
    }

    @Override
    public void sendMessageBA(String baId, String message) {
        Entity<SignedEntity<BAMessage>> entity = Entity.entity(new SignedEntity<>(new BAMessage(baId, message, null, id), sk), MediaType.APPLICATION_JSON);

        Response resp = target.path("BAMessage").request().post(entity);
        if (!(resp.getStatus() == 204)) {
            logger.error("Failed to post secret to with status= " + resp.getStatus() + " webtarget: " + target);
        }
    }

    @Override
    public void sendMessageBA(String baId, Boolean message) {
        Entity<SignedEntity<BAMessage>> entity = Entity.entity(new SignedEntity<>(new BAMessage(baId, null, message, id), sk), MediaType.APPLICATION_JSON);

        Response resp = target.path("BAMessage").request().post(entity);
        if (!(resp.getStatus() == 204)) {
            logger.error("Failed to post secret to with status= " + resp.getStatus() + " webtarget: " + target);
        }
    }

    @Override
    public void sendMessageBroadcast(String message) {
        Entity<SignedEntity<String>> entity = Entity.entity(new SignedEntity<>(message, sk), MediaType.APPLICATION_JSON);

        Response resp = target.path("BroadcastMessage").path(id).request().post(entity);
        if (!(resp.getStatus() == 204)) {
            logger.error("Failed to post secret to with status= " + resp.getStatus() + " webtarget: " + target);
        }
    }
}
