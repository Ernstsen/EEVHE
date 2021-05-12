package dk.mmj.eevhe.protocols.agreement;

import dk.mmj.eevhe.protocols.agreement.broadcast.TestBrachaBroadcastManager;
import dk.mmj.eevhe.protocols.agreement.broadcast.TestDummyBroadcastManager;
import dk.mmj.eevhe.protocols.agreement.mvba.TestByzantineAgreementProtocolImpl;
import dk.mmj.eevhe.protocols.agreement.mvba.TestCompositeCommunicator;
import dk.mmj.eevhe.protocols.agreement.mvba.TestMultiValuedByzantineAgreementProtocolImpl;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestAgreementHelper.class,
        TestBrachaBroadcastManager.class,
        TestDummyBroadcastManager.class,
        TestByzantineAgreementProtocolImpl.class,
        TestCompositeCommunicator.class,
        TestMultiValuedByzantineAgreementProtocolImpl.class,
        TestNotifyItem.class,
        TestTimeoutMap.class
})
public class AgreementTestSuite {
}
