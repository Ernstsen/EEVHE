package dk.mmj.eevhe.protocols.agreement.mvba;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class TestCompositeCommunicator {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void allLambdasRespected() {
        Map<String, String> strings = new HashMap<>();
        Map<String, Boolean> bools = new HashMap<>();
        List<Incoming<Communicator.Message<String>>> strings2 = new ArrayList<>();
        List<Incoming<Communicator.Message<Boolean>>> bools2 = new ArrayList<>();


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

        communicator.registerOnReceivedString(strings2::add);
        communicator.registerOnReceivedBoolean(bools2::add);

        communicator.receiveString(new IncomingTestImpl<>(new Communicator.Message<>(id, strVal2), "id", true));
        communicator.receiveBool(new IncomingTestImpl<>(new Communicator.Message<>(id, boolVal2), "id", true));

        assertEquals("String was not sent properly", strVal, strings.get(id));
        assertEquals("Bool was not sent properly", boolVal, bools.get(id));
        assertEquals("Bool was not sent properly", boolVal2, bools.get("id"));
        assertEquals("String was not received properly", strVal2, strings2.get(0).getContent().getMessage());
        assertEquals("Bool was not received properly", boolVal2, bools2.get(0).getContent().getMessage());
    }
}
