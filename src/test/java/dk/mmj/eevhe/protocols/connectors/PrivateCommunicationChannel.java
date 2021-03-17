package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;

import java.util.ArrayList;
import java.util.List;

public class PrivateCommunicationChannel implements IncomingChannel, PeerCommunicator {
    private final List<PartialSecretMessageDTO> messages = new ArrayList<>();

    @Override
    public List<PartialSecretMessageDTO> receiveSecrets() {
        return messages;
    }

    @Override
    public void sendSecret(PartialSecretMessageDTO value) {
        messages.add(value);
    }
}
