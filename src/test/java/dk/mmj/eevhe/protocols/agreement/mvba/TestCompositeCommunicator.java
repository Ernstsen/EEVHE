package dk.mmj.eevhe.protocols.agreement.mvba;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class TestCompositeCommunicator {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void allLambdasRespected() {
        Map<String, String> strings = new HashMap<>();
        Map<String, Boolean> bools = new HashMap<>();
        Map<String, String> strings2 = new HashMap<>();
        Map<String, Boolean> bools2 = new HashMap<>();


        String id = UUID.randomUUID().toString();

        CompositeCommunicator communicator = new CompositeCommunicator(
                strings::put,
                bools::put
        );

        String strVal = "someString";
        String strVal2 = "someString2";
        Boolean boolVal = true;
        Boolean boolVal2 = false;

        communicator.send(id, strVal);
        communicator.send(id, boolVal);
        communicator.send("id", boolVal2);

        communicator.registerOnReceivedString(strings2::put);
        communicator.registerOnReceivedBoolean(bools2::put);

        communicator.receive(id, strVal2);
        communicator.receive(id, boolVal2);

        assertEquals("String was not sent properly", strVal, strings.get(id));
        assertEquals("Bool was not sent properly", boolVal, bools.get(id));
        assertEquals("Bool was not sent properly", boolVal2, bools.get("id"));
        assertEquals("String was not received properly", strVal2, strings2.get(id));
        assertEquals("Bool was not received properly", boolVal2, bools2.get(id));
    }
}
