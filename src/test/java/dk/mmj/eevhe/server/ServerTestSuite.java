package dk.mmj.eevhe.server;

import dk.mmj.eevhe.server.bulletinboard.TestBulletinBoard;
import dk.mmj.eevhe.server.bulletinboard.TestBulletinBoardConfigBuilder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestServerState.class,
        TestBulletinBoardConfigBuilder.class,
        TestBulletinBoard.class
})
public class ServerTestSuite {
}
