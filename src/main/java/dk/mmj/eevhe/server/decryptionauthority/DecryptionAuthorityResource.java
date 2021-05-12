package dk.mmj.eevhe.server.decryptionauthority;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/")
public class DecryptionAuthorityResource {
    private static final Logger logger = LogManager.getLogger(DecryptionAuthorityResource.class);

    @POST
    @Path("partialSecret")
    public void postPartialSecret(SignedEntity<PartialSecretMessageDTO> partialSecret) {
        logger.debug("Received DA partial secret: " + partialSecret.toString());
        ServerState state = ServerState.getInstance();
        state.put(partialSecretKey(partialSecret), partialSecret);
    }

    private String partialSecretKey(SignedEntity<PartialSecretMessageDTO> message) {
        PartialSecretMessageDTO entity = message.getEntity();
        return entity.getTarget() + "secret:" + entity.getSender();
    }
}
