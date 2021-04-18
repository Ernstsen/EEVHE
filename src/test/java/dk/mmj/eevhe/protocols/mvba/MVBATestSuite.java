package dk.mmj.eevhe.protocols.mvba;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestBrachaBroadcastManager.class,
        TestByzantineAgreementProtocolImpl.class,
        TestCompositeCommunicator.class,
        TestMultiValuedByzantineAgreementProtocolImpl.class,
        TestNotifyItem.class,
        TestTimeoutMap.class
})
public class MVBATestSuite {
}
