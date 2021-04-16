package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.protocols.connectors.TestBulletinBoardBroadcastManager;
import dk.mmj.eevhe.protocols.connectors.TestRestPeerCommunicator;
import dk.mmj.eevhe.protocols.connectors.TestServerStateIncomingChannel;
import dk.mmj.eevhe.protocols.mvba.MVBATestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestGennaroFeldmanVSS.class,
        TestGennaroDKG.class,
        TestPedersenVSS.class,
        TestBulletinBoardBroadcastManager.class,
        TestRestPeerCommunicator.class,
        TestServerStateIncomingChannel.class,
        MVBATestSuite.class
})
public class ProtocolTestSuite {
}
