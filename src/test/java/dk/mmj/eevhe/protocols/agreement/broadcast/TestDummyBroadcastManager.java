package dk.mmj.eevhe.protocols.agreement.broadcast;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class TestDummyBroadcastManager {

    @Test
    public void testCallsRegisteredHandler(){
        DummyBroadcastManager manager = new DummyBroadcastManager();

        ArrayList<String> strings = new ArrayList<>();

        manager.registerOnReceived(strings::add);

        manager.broadcast("", "message");

        assertEquals("Should have called onReceived once",1,strings.size());
        assertEquals("Unexpected message in list", "message", strings.get(0));


    }

}
