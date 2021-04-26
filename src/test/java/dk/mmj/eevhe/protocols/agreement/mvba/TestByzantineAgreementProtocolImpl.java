package dk.mmj.eevhe.protocols.agreement.mvba;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@SuppressWarnings("ConstantConditions")
public class TestByzantineAgreementProtocolImpl {
    private Map<String, Boolean> bools;
    private CompositeCommunicator communicator;

    @Before
    public void setupCommunicator() {
        bools = new HashMap<>();

        communicator = new CompositeCommunicator (
                new HashMap<>()::put,
                bools::put
        );
    }

    @Test
    public void shouldReachAgreementNoCorrupt(){
        Boolean testBool = true;

        ByzantineAgreementProtocolImpl baProtocol = new ByzantineAgreementProtocolImpl(communicator, 3, 0, "p1");
        ByzantineAgreementCommunicator.BANotifyItem<Boolean> baAgreement = baProtocol.agree(testBool);

        assertEquals(1, bools.entrySet().size());

        String id = bools.keySet().toArray(new String[0])[0];

        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, testBool), "p2", true ));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, testBool), "p3", true ));

        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertTrue("Majority >= t did not agree on all values sent.", baAgreement.getAgreement());
    }

    @Test
    public void shouldReachAgreementSomeDisagree() {
        Boolean testBool = true;

        ByzantineAgreementProtocolImpl baProtocol = new ByzantineAgreementProtocolImpl(communicator, 4, 1, "p1");
        ByzantineAgreementCommunicator.BANotifyItem<Boolean> baAgreement = baProtocol.agree(testBool);

        assertEquals(1, bools.entrySet().size());

        String id = bools.keySet().toArray(new String[0])[0];

        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, testBool), "p2", true));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, testBool), "p3", true));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p4", true));

        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertTrue("Majority >= t did not agree on all values sent.", baAgreement.getAgreement());
    }

    @Test
    public void shouldReachAgreementSomeDisagreeSpamming() {
        Boolean testBool = true;

        ByzantineAgreementProtocolImpl baProtocol = new ByzantineAgreementProtocolImpl(communicator, 4, 1, "p1");
        ByzantineAgreementCommunicator.BANotifyItem<Boolean> baAgreement = baProtocol.agree(testBool);

        assertEquals(1, bools.entrySet().size());

        String id = bools.keySet().toArray(new String[0])[0];

        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, testBool), "p2", true));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p4", true));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p2", false));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p2", false));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p3", false));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p3", false));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p4", true));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p4", true));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p4", true));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p4", true));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, testBool), "p3", true));

        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertTrue("Majority >= t did not agree on all values sent.", baAgreement.getAgreement());
    }

    @Test
    public void shouldBeUndecided() {
        Boolean testBool = true;

        ByzantineAgreementProtocolImpl baProtocol = new ByzantineAgreementProtocolImpl(communicator, 3, 0, "p1");
        ByzantineAgreementCommunicator.BANotifyItem<Boolean> baAgreement = baProtocol.agree(testBool);

        assertEquals(1, bools.entrySet().size());

        String id = bools.keySet().toArray(new String[0])[0];

        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, testBool), "p2", true));
        new Thread(() -> communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, !testBool), "p3", true))).start();

        baAgreement.waitForFinish();

        assertNull("BA Agreement should be null as the message should be undecided.", baAgreement.getAgreement());
    }
}
