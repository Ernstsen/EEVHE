package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.protocols.connectors.TestBulletinBoardBroadcaster;
import dk.mmj.eevhe.protocols.connectors.TestRestPeerCommunicator;
import dk.mmj.eevhe.protocols.connectors.TestServerStateIncomingChannel;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestGennaroDKG.class,
        TestBulletinBoardBroadcaster.class,
        TestRestPeerCommunicator.class,
        TestServerStateIncomingChannel.class
})
public class ProtocolTestSuite {
}
