package dk.mmj.eevhe.protocols.connectors.interfaces;

import dk.mmj.eevhe.entities.BBState;
import dk.mmj.eevhe.entities.SignedEntity;

public interface BBPeerCommunicator {
    /**
     * Sends content signed using the BB-Peer's secret key
     * Corresponds to "Partial BB Contents"
     *
     * @param content Signed content
     */
    void sendContent(SignedEntity<BBState> content);
}
