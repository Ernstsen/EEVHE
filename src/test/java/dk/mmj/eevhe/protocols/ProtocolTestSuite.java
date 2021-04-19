package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.protocols.connectors.TestBulletinBoardDKGBroadcaster;
import dk.mmj.eevhe.protocols.connectors.TestRestDKGPeerCommunicator;
import dk.mmj.eevhe.protocols.connectors.TestServerStateDKGIncomingChannel;
import dk.mmj.eevhe.protocols.mvba.MVBATestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestGennaroFeldmanVSS.class,
        TestGennaroDKG.class,
        TestPedersenVSS.class,
        TestBulletinBoardDKGBroadcaster.class,
        TestRestDKGPeerCommunicator.class,
        TestServerStateDKGIncomingChannel.class,
        MVBATestSuite.class
})
public class ProtocolTestSuite {
}
