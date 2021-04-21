package dk.mmj.eevhe.protocols.connectors.interfaces;

import dk.mmj.eevhe.server.bulletinboard.BulletinBoardState;
import dk.mmj.eevhe.entities.SignedEntity;

public interface BBPeerCommunicator {
    void sendMessage(String baId, String message);
    void sendMessage(String baId, Boolean message);
}
