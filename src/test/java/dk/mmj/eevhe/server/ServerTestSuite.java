package dk.mmj.eevhe.server;

import dk.mmj.eevhe.server.bulletinboard.TestBulletinBoardConfigBuilder;
import dk.mmj.eevhe.server.bulletinboard.TestBulletinBoardPeer;
import dk.mmj.eevhe.server.bulletinboard.TestBulletinBoardState;
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
        TestBulletinBoardPeer.class,
        TestBulletinBoardState.class
})
public class ServerTestSuite {
}
