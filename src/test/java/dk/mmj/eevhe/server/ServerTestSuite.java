package dk.mmj.eevhe.server;

import dk.mmj.eevhe.server.bulletinboard.*;
import dk.mmj.eevhe.server.decryptionauthority.TestDecrypterImpl;
import dk.mmj.eevhe.server.decryptionauthority.TestDecryptionAuthorityConfigBuilder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestServerState.class,
        TestBulletinBoardConfigBuilder.class,
        TestDecrypterImpl.class,
        TestDecryptionAuthorityConfigBuilder.class,
        TestBulletinBoardState.class,
        TestBulletinBoardPeer.class,
        TestBulletinBoardPeerCommunication.class,
        TestBulletinBoardEdge.class,
        TestBulletinBoardEdgeCommunication.class
})
public class ServerTestSuite {
}

