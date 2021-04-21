package dk.mmj.eevhe.protocols.agreement.mvba;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.util.Map;

public class IncomingImpl<T extends IdentityHaving> implements Incoming<T> {
    private static final Logger logger = LogManager.getLogger(IncomingImpl.class);
    private SignedEntity<T> signedEntity;

    public IncomingImpl(SignedEntity<T> signedEntity) {
        this.signedEntity = signedEntity;
    }

    @Override
    public T getContent() {
        return signedEntity.getEntity();
    }

    @Override
    public String getIdentifier() {
        return getId().toString();
    }

    private Integer getId() {
        return signedEntity.getEntity().getId();
    }

    @Override
    public boolean isValid() {
//        TODO: prettify:
        AsymmetricKeyParameter pk = (AsymmetricKeyParameter) ServerState.getInstance().get("peerCertificates", Map.class).get(getId());
        try {
            return signedEntity.verifySignature(pk);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to validate signature on incoming message", e);
            return false;
        }
    }
}
