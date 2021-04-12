package dk.mmj.eevhe.protocols.mvba;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

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

        communicator.receive(stringId, testStr);
        communicator.receive(stringId, testStr);
        communicator.receive(stringId, testStr2);

        assertEquals(1, bools.entrySet().size());

        String boolId = bools.keySet().toArray(new String[0])[0];

        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);
        communicator.receive(boolId, testBool);

        baAgreement.waitForFinish();
        assertNotNull("BA Agreement was null, not yet terminated.", baAgreement.getAgreement());
        assertEquals("Majority >= t did not agree on all values sent.", testStr, baAgreement.getMessage());
    }

    @Test
    public void shouldReachAgreementSomeDisagreeBoolean() {
        fail("Not implemented");
    }

    @Test
    public void shouldBeUndecided() {
        fail("Not implemented");
    }

    @Test
    public void shouldDisagree() {
        fail("Not implemented");
    }

}
