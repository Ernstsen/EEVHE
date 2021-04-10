package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.protocols.connectors.TestBulletinBoardBroadcaster;
import dk.mmj.eevhe.protocols.connectors.TestRestPeerCommunicator;
import dk.mmj.eevhe.protocols.connectors.TestServerStateIncomingChannel;
import dk.mmj.eevhe.protocols.mvba.TestNotifyItem;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestGennaroFeldmanVSS.class,
        TestGennaroDKG.class,
        TestPedersenVSS.class,
        TestBulletinBoardBroadcaster.class,
        TestRestPeerCommunicator.class,
        TestServerStateIncomingChannel.class,
        TestNotifyItem.class
})
public class ProtocolTestSuite {
}
