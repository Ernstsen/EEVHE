package dk.mmj.eevhe.server.decryptionauthority;

import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
public class DecryptionAuthorityResource {
    private static Logger logger = LogManager.getLogger(DecryptionAuthorityResource.class);
    private Integer id;

    /**
     * Endpoint to be used in integrationTest to separate the state of the DA instances. Does not affect behaviour
     *
     * @param id id of server
     */
    @POST
    @Path("identity")
    public void identity(Integer id) {
        if (id != null) {
            this.id = id;
        }
    }

    @GET
    @Path("type")
    @Produces(MediaType.TEXT_HTML)
    public String test() {
        logger.info("Received request for server type");

        return "<b>ServerType:</b> Decryption Authority";
    }

    @POST
    @Path("partialSecret")
    public void postPartialSecret(DecryptionAuthority.PartialSecretMessage partialSecret) {
        ServerState state = ServerState.getInstance();
        state.put(partialSecretKey(partialSecret.getId()), partialSecret);
    }

    private String partialSecretKey(Integer id) {
        return statePrefix(this.id) + "secret:" + id;
    }

    public static String statePrefix(Integer id) {
        if (id != null) {
            return id.toString() + ":";
        } else {
            return "";
        }
    }
}
