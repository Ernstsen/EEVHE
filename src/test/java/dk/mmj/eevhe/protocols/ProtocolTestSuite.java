package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.protocols.connectors.TestBulletinBoardBroadcaster;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestPedersenDKG.class,
        TestBulletinBoardBroadcaster.class
})
public class ProtocolTestSuite {
}
