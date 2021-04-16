package dk.mmj.eevhe.protocols.mvba;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestBrachaBroadcastManager {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String goToNextMessage(String msg) {
        try {
            BrachaBroadcastManager.Message message = mapper.readValue(msg, BrachaBroadcastManager.Message.class);

            switch (message.type) {
                case SEND:
                    message.setType(BrachaBroadcastManager.Type.ECHO);
                    break;
                case ECHO:
                    message.setType(BrachaBroadcastManager.Type.READY);
                    break;
                case READY:
                    break;
            }

            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
            return null;
        }
    }

    private static void autoAnswer(BrachaBroadcastManager manager, String s) {
        try {
            manager.receive(goToNextMessage(s));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void shouldBroadcastSuccessfully() {
        String message = "This is the message that is broadcasted";

        Map<Integer, Consumer<String>> peers = new HashMap<>();


        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 1, 0);
        manager.registerOnReceived(spy);

        peers.put(2, s -> autoAnswer(manager, s));
        peers.put(3, s -> autoAnswer(manager, s));

        String broadcastId = "BID";
        manager.broadcast(broadcastId, message);


        assertEquals("Did not call listener with proper output!", message, spy.val);
    }

    @Test
    public void shouldHandleReceivedBroadcastSuccessfully() throws JsonProcessingException {
        String message = "This is the message that is broadcasted";
        BrachaBroadcastManager.Message broadcast = new BrachaBroadcastManager.Message(
                BrachaBroadcastManager.Type.SEND, 2, "BID", message);

        Map<Integer, Consumer<String>> peers = new HashMap<>();

        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 1, 0);
        manager.registerOnReceived(spy);

        peers.put(2, s -> autoAnswer(manager, s));
        peers.put(3, s -> autoAnswer(manager, s));

        manager.receive(mapper.writeValueAsString(broadcast));

        assertEquals("Did not call listener with proper output!", message, spy.val);
    }

    @Test
    public void testMessageEqualsAndSerialization() throws JsonProcessingException {
        BrachaBroadcastManager.Message msg = new BrachaBroadcastManager.Message(BrachaBroadcastManager.Type.READY, 234, "weuigfwnjefw", "!iowuuengfwefg");

        BrachaBroadcastManager.Message deserialized = mapper.readValue(mapper.writeValueAsString(msg), BrachaBroadcastManager.Message.class);

        assertEquals("Hashes did not match for serialized/deserialized and original", msg.hashCode(), deserialized.hashCode());
        assertEquals("Serialized/deserialized did not match original", msg, deserialized);
    }

    private static class DummyConsumer implements Consumer<String> {
        private String val;

        @Override
        public void accept(String s) {
            val = s;
        }
    }
}
