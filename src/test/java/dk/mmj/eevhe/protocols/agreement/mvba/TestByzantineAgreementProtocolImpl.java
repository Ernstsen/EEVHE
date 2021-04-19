package dk.mmj.eevhe.protocols.agreement.mvba;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@SuppressWarnings("ConstantConditions")
public class TestByzantineAgreementProtocolImpl {
    private Map<String, String> strings;
    private Map<String, Boolean> bools;
    private CompositeCommunicator communicator;

    @Before
    public void setupCommunicator() {
        strings = new HashMap<>();
        bools = new HashMap<>();

        communicator = new CompositeCommunicator(
                strings::put,
                bools::put
        );
    }

    @Test
    public void shouldReachAgreementNoCorrupt() throws InterruptedException {
        Boolean testBool = true;

        ByzantineAgreementProtocolImpl baProtocol = new ByzantineAgreementProtocolImpl(communicator, 3, 0);
        ByzantineAgreementCommunicator.BANotifyItem<Boolean> baAgreement = baProtocol.agree(testBool);

        assertEquals(1, bools.entrySet().size());

        String id = bools.keySet().toArray(new String[0])[0];

        communicator.receive(id, testBool);
        communicator.receive(id, testBool);

        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertTrue("Majority >= t did not agree on all values sent.", baAgreement.getAgreement());
    }

    @Test
    public void shouldReachAgreementSomeDisagree() {
        Boolean testBool = true;

        ByzantineAgreementProtocolImpl baProtocol = new ByzantineAgreementProtocolImpl(communicator, 4, 1);
        ByzantineAgreementCommunicator.BANotifyItem<Boolean> baAgreement = baProtocol.agree(testBool);

        assertEquals(1, bools.entrySet().size());

        String id = bools.keySet().toArray(new String[0])[0];

        communicator.receive(id, testBool);
        communicator.receive(id, testBool);
        communicator.receive(id, !testBool);

        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertTrue("Majority >= t did not agree on all values sent.", baAgreement.getAgreement());
    }

    @Test
    public void shouldBeUndecided() {
        Boolean testBool = true;

        ByzantineAgreementProtocolImpl baProtocol = new ByzantineAgreementProtocolImpl(communicator, 3, 0);
        ByzantineAgreementCommunicator.BANotifyItem<Boolean> baAgreement = baProtocol.agree(testBool);

        assertEquals(1, bools.entrySet().size());

        String id = bools.keySet().toArray(new String[0])[0];

        communicator.receive(id, testBool);
        new Thread(() -> communicator.receive(id, !testBool)).start();

        baAgreement.waitForFinish();

        assertNull("BA Agreement should be null as the message should be undecided.", baAgreement.getAgreement());
    }
}
