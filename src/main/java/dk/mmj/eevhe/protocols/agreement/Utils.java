package dk.mmj.eevhe.protocols.agreement;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Utils {
    private static final Logger logger = LogManager.getLogger(Utils.class);

    public static boolean validate(SignedEntity<?> se, String senderId) {
//        TODO: prettify:
        String pkString = (String) ServerState.getInstance().get("peerCertificates", Map.class).get(senderId);
        try {
            AsymmetricKeyParameter pk = CertificateHelper.getPublicKeyFromCertificate(pkString.getBytes(StandardCharsets.UTF_8));
            return se.verifySignature(pk);
        } catch (IOException e) {
            logger.warn("Failed to validate signature on incoming message", e);
            return false;
        }
    }
}
