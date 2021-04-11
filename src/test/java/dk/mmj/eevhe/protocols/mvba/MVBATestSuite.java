package dk.mmj.eevhe.protocols.mvba;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestByzantineAgreementProtocolImpl.class,
        TestMultiValuedByzantineAgreementProtocolImpl.class,
        TestNotifyItem.class
})
public class MVBATestSuite {
}
