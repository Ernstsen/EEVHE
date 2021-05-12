package dk.mmj.eevhe.server;

import dk.mmj.eevhe.server.bulletinboard.*;
import dk.mmj.eevhe.server.decryptionauthority.TestDecrypterImpl;
import dk.mmj.eevhe.server.decryptionauthority.TestDecryptionAuthorityConfigBuilder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestBulletinBoardEdge.class,
        TestBulletinBoardEdgeCommunication.class,
        TestBulletinBoardPeer.class,
        TestBulletinBoardPeerCommunication.class,
        TestBulletinBoardPeerConfigBuilder.class,
        TestBulletinBoardState.class,
        TestDecrypterImpl.class,
        TestDecryptionAuthorityConfigBuilder.class,
        TestServerState.class,
})
public class ServerTestSuite {
}

