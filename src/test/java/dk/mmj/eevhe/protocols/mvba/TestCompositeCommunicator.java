package dk.mmj.eevhe.protocols.mvba;

import org.junit.Test;

import java.util.*;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TestCompositeCommunicator {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void allLambdasRespected() {
        Map<String, String> strings = new HashMap<>();
        Map<String, Boolean> bools = new HashMap<>();
        List<BiConsumer<String, String>> onStrings = new ArrayList<>();
        List<BiConsumer<String, Boolean>> onBools = new ArrayList<>();

        String id = UUID.randomUUID().toString();

        CompositeCommunicator communicator = new CompositeCommunicator(
                strings::put,
                bools::put,
                onStrings::add,
                onBools::add
        );

        String strVal = "someString";
        Boolean boolVal = true;
        Boolean boolVal2 = false;

        communicator.send(id, strVal);
        communicator.send(id, boolVal);
        communicator.send("id", boolVal2);

        BiConsumer<String, String> onString = (s, s2) -> {
        };
        BiConsumer<String, Boolean> onBool = (s, b) -> {
        };

        communicator.registerOnReceivedString(onString);
        communicator.registerOnReceivedBoolean(onBool);

        assertSame("Failed to register onString handler", onString, onStrings.get(0));
        assertSame("Failed to register onBool handler", onBool, onBools.get(0));
        assertEquals("String was not sent properly", strVal, strings.get(id));
        assertEquals("Bool was not sent properly", boolVal, bools.get(id));
        assertEquals("Bool was not sent properly", boolVal2, bools.get("id"));
    }
}
