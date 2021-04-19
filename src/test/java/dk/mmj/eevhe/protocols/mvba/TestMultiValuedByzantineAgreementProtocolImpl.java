package dk.mmj.eevhe.protocols.mvba;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@SuppressWarnings("ConstantConditions")
public class TestMultiValuedByzantineAgreementProtocolImpl {
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
    public void shouldReachAgreementNoCorrupt() {
        String testStr = "true";
        Boolean testBool = true;

        MultiValuedByzantineAgreementProtocolImpl baProtocol =
                new MultiValuedByzantineAgreementProtocolImpl(communicator, 3, 0);
        ByzantineAgreementCommunicator.BANotifyItem<String> baAgreement = baProtocol.agree(testStr);

        assertEquals(1, strings.entrySet().size());

        String stringId = strings.keySet().toArray(new String[0])[0];

        communicator.receive(stringId, testStr);
        communicator.receive(stringId, testStr);

        assertEquals(1, bools.entrySet().size());

        String boolId = bools.keySet().toArray(new String[0])[0];

        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);

        baAgreement.waitForFinish();
        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertTrue("BA Agreement should be true.", baAgreement.getAgreement());
        assertEquals("Majority >= t did not agree on all values sent.", testStr, baAgreement.getMessage());
    }

    @Test
    public void shouldReachAgreementSomeDisagreeString() {
        String testStr = "true";
        String testStr2 = "True";
        Boolean testBool = true;

        MultiValuedByzantineAgreementProtocolImpl baProtocol =
                new MultiValuedByzantineAgreementProtocolImpl(communicator, 4, 1);
        ByzantineAgreementCommunicator.BANotifyItem<String> baAgreement = baProtocol.agree(testStr);

        assertEquals(1, strings.entrySet().size());

        String stringId = strings.keySet().toArray(new String[0])[0];

        communicator.receive(stringId, testStr2);
        communicator.receive(stringId, testStr);
        communicator.receive(stringId, testStr);

        assertEquals(1, bools.entrySet().size());

        String boolId = bools.keySet().toArray(new String[0])[0];

        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);

        baAgreement.waitForFinish();
        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertTrue("BA Agreement should be true.", baAgreement.getAgreement());
        assertEquals("Majority >= t did not agree on all values sent.", testStr, baAgreement.getMessage());
    }

    @Test
    public void shouldReachAgreementSomeDisagreeBoolean() {
        String testStr = "true";
        String testStr2 = "True";
        Boolean testBool = true;

        MultiValuedByzantineAgreementProtocolImpl baProtocol =
                new MultiValuedByzantineAgreementProtocolImpl(communicator, 4, 1);
        ByzantineAgreementCommunicator.BANotifyItem<String> baAgreement = baProtocol.agree(testStr);

        assertEquals(1, strings.entrySet().size());

        String stringId = strings.keySet().toArray(new String[0])[0];

        communicator.receive(stringId, testStr2);
        communicator.receive(stringId, testStr2);
        communicator.receive(stringId, testStr);

        assertEquals(1, bools.entrySet().size());

        String boolId = bools.keySet().toArray(new String[0])[0];

        communicator.receive(boolId, !testBool);
        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);

        baAgreement.waitForFinish();
        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertTrue("BA Agreement should be true.", baAgreement.getAgreement());
        assertEquals("Majority >= t did not agree on all values sent.", testStr2, baAgreement.getMessage());
    }

    @Test
    public void shouldReachAgreementForString() {
        String testStr = "true";
        String testStr2 = "True";
        Boolean testBool = true;

        MultiValuedByzantineAgreementProtocolImpl baProtocol =
                new MultiValuedByzantineAgreementProtocolImpl(communicator, 4, 1);
        ByzantineAgreementCommunicator.BANotifyItem<String> baAgreement = baProtocol.agree(testStr);

        assertEquals(1, strings.entrySet().size());

        String stringId = strings.keySet().toArray(new String[0])[0];

        communicator.receive(stringId, testStr2);
        communicator.receive(stringId, testStr2);
        communicator.receive(stringId, testStr);

        assertEquals(1, bools.entrySet().size());

        String boolId = bools.keySet().toArray(new String[0])[0];

        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);

        baAgreement.waitForFinish();
        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertTrue("BA Agreement should be true.", baAgreement.getAgreement());
        assertEquals("Majority >= t did not agree on all values sent.", testStr2, baAgreement.getMessage());
    }

    @Test
    public void shouldDisagree() {
        String testStr = "true";
        String testStr2 = "True";
        Boolean testBool = false;

        MultiValuedByzantineAgreementProtocolImpl baProtocol =
                new MultiValuedByzantineAgreementProtocolImpl(communicator, 4, 1);
        ByzantineAgreementCommunicator.BANotifyItem<String> baAgreement = baProtocol.agree(testStr);

        assertEquals(1, strings.entrySet().size());

        String stringId = strings.keySet().toArray(new String[0])[0];

        communicator.receive(stringId, testStr2);
        communicator.receive(stringId, testStr2);
        communicator.receive(stringId, testStr);

        assertEquals(1, bools.entrySet().size());

        String boolId = bools.keySet().toArray(new String[0])[0];

        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);

        baAgreement.waitForFinish();
        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertFalse("BA Agreement should be false.", baAgreement.getAgreement());
        assertNull("Majority >= t should not agree on values sent, thus msg should be null.", baAgreement.getMessage());
    }
}
