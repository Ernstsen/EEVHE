package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGIncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGPeerCommunicator;

import java.util.ArrayList;
import java.util.List;

public class PrivateCommunicationChannelDKG implements DKGIncomingChannel, DKGPeerCommunicator {
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
