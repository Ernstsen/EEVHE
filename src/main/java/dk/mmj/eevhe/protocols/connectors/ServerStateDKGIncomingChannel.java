package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.interfaces.CertificateProvider;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGIncomingChannel;
import dk.mmj.eevhe.server.ServerState;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads {@link ServerState} to determine incoming messages
 */
public class ServerStateDKGIncomingChannel implements DKGIncomingChannel {
    private final List<String> ids;
    private final CertificateProvider certProvider;

    /**
     * @param ids          list of ids that the messages might be saved under
     * @param certProvider provider for certificates used in verifying incoming messages
     */
    public ServerStateDKGIncomingChannel(List<String> ids, CertificateProvider certProvider) {
        this.ids = ids;
        this.certProvider = certProvider;
    }

    @Override
    public List<PartialSecretMessageDTO> receiveSecrets() {
        final ServerState instance = ServerState.getInstance();
        final ArrayList<PartialSecretMessageDTO> res = new ArrayList<>();

        Map<Integer, String> certs = certProvider.generateCertMap();

        List<SignedEntity<PartialSecretMessageDTO>> intermediate = new ArrayList<>();
        for (String id : ids) {
            //noinspection unchecked
            intermediate.add(instance.get(id, SignedEntity.class));
        }

        intermediate.stream()
                .filter(Objects::nonNull)
                .filter(se -> verifySecret(se, certs))
                .map(SignedEntity::getEntity)
                .forEach(res::add);

        return res;
    }

    /**
     * Verifies signature of partial secret object
     *
     * @param e     signed entity of PartialSecretMessageDTO
     * @param certs map of retrieved certificates
     * @return whether the signature is valid
     */
    private boolean verifySecret(SignedEntity<PartialSecretMessageDTO> e, Map<Integer, String> certs) {
        try {
            String cert = certs.get(e.getEntity().getSender());
            byte[] certBytes = cert.getBytes(StandardCharsets.UTF_8);
            return e.verifySignature(CertificateHelper.getPublicKeyFromCertificate(certBytes));
        } catch (Exception exception) {
            return false;
        }
    }
}
