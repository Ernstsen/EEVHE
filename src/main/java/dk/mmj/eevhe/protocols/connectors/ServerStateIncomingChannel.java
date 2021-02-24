package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.interfaces.IncomingChannel;
import dk.mmj.eevhe.server.ServerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads {@link ServerState} to determine incoming messages
 */
public class ServerStateIncomingChannel implements IncomingChannel {
    private final List<String> ids;

    /**
     * @param ids list of ids that the messages might be saved under
     */
    public ServerStateIncomingChannel(List<String> ids) {
        this.ids = ids;
    }

    @Override
    public List<PartialSecretMessageDTO> receiveSecrets() {
        final ServerState instance = ServerState.getInstance();
        final ArrayList<PartialSecretMessageDTO> res = new ArrayList<>();
        ids.stream()
                .map(id -> instance.get(id, PartialSecretMessageDTO.class))
                .filter(Objects::nonNull)
                .forEach(res::add);
        return res;
    }
}
