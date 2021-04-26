package dk.mmj.eevhe.protocols.agreement.broadcast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.protocols.agreement.mvba.Incoming;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /**
     * Wraps sending to manager so no exception needs to be handled
     *
     * @param manager         manager to receive message
     * @param s               message to send
     * @param id              senderId
     * @param failOnException whether test fails when exception is thrown - often false for adversaries
     * @param valid           whether incomings should be valid
     */
    private static void sendWrap(BrachaBroadcastManager manager, String s, int id, boolean failOnException, boolean valid) {
        try {
            manager.receive(new SimpleIncoming(s, id, valid));
        } catch (JsonProcessingException e) {
            if (failOnException) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * Repeats a runnable a number of times
     *
     * @param runnable runnable to execute
     * @param times    number of times
     */
    @SuppressWarnings("SameParameterValue")
    private static void repeat(Runnable runnable, int times) {
        for (int i = 0; i < times; i++) {
            runnable.run();
        }
    }

    /**
     * Wraps sending to manager so no exception needs to be handled
     *
     * @param manager manager to receive message
     * @param s       message to send
     * @param id      senderId
     */
    private static void sendWrap(BrachaBroadcastManager manager, String s, int id) {
        sendWrap(manager, s, id, true, true);
    }

    /**
     * Replaces containing message with new one
     *
     * @param body       serialized message object
     * @param newMessage new message to write to object
     * @return serialized message object with only change being new contained message
     */
    private static String replaceMessage(String body, String newMessage) {
        try {
            BrachaBroadcastManager.Message msg = mapper.readValue(body, BrachaBroadcastManager.Message.class);

            msg.setMessage(newMessage);

            return mapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldBroadcastSuccessfully() {
        String message = "This is the message that is broadcasted";

        Map<Integer, Consumer<String>> peers = new HashMap<>();


        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 1);
        manager.registerOnReceived(spy);

        peers.put(2, s -> sendWrap(manager, goToNextMessage(s), 2));
        peers.put(3, s -> sendWrap(manager, goToNextMessage(s), 3));

        String broadcastId = "BID";
        manager.broadcast(broadcastId, message);

        assertEquals("Expected exactly one output val", 1, spy.vals.size());
        assertEquals("Did not call listener with proper output!", message, spy.vals.get(0));
    }

    @Test
    public void shouldNotBroadcastWhenInputIsInvalid() {
        String message = "This is the message that is broadcasted";

        Map<Integer, Consumer<String>> peers = new HashMap<>();


        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers,  0);
        manager.registerOnReceived(spy);

        peers.put(2, s -> sendWrap(manager, goToNextMessage(s), 2, true, false));
        peers.put(3, s -> sendWrap(manager, goToNextMessage(s), 3, true, false));

        String broadcastId = "BID";
        manager.broadcast(broadcastId, message);

        assertEquals("Did not expect any output", 0, spy.vals.size());
    }

    @Test
    public void shouldBroadcastSuccessfullyOneAdversaryIncoherentMessage() {
        String message = "This is the message that is broadcasted";

        Map<Integer, Consumer<String>> peers = new HashMap<>();


        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 1);
        manager.registerOnReceived(spy);

        peers.put(2, s -> sendWrap(manager, goToNextMessage(s), 2));
        peers.put(3, s -> sendWrap(manager, "This is some faulty string", 3, false, true));
        peers.put(4, s -> sendWrap(manager, goToNextMessage(s), 4));

        String broadcastId = "BID";
        manager.broadcast(broadcastId, message);

        assertEquals("Expected exactly one output val", 1, spy.vals.size());
        assertEquals("Did not call listener with proper output!", message, spy.vals.get(0));
    }

    @Test
    public void shouldBroadcastSuccessfullyOneAdversaryInactive() {
        String message = "This is the message that is broadcasted";

        Map<Integer, Consumer<String>> peers = new HashMap<>();


        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 1);
        manager.registerOnReceived(spy);

        peers.put(2, s -> sendWrap(manager, goToNextMessage(s), 2));
        peers.put(3, s -> {
        });
        peers.put(4, s -> sendWrap(manager, goToNextMessage(s), 4));

        String broadcastId = "BID";
        manager.broadcast(broadcastId, message);

        assertEquals("Expected exactly one output val", 1, spy.vals.size());
        assertEquals("Did not call listener with proper output!", message, spy.vals.get(0));
    }

    @Test
    public void shouldBroadcastSuccessfullyOneAdversarySpamming() throws JsonProcessingException {
        String message = "This is the message that is broadcasted";

        Map<Integer, Consumer<String>> peers = new HashMap<>();


        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 1);
        manager.registerOnReceived(spy);

        peers.put(2, s -> repeat(() -> sendWrap(manager, replaceMessage(s, "msg"), 2, false, true), 8));
        peers.put(3, s -> {
        });
        peers.put(4, s -> {
        });

        String broadcastId = "BID";
        BrachaBroadcastManager.Message echo = new BrachaBroadcastManager.Message(BrachaBroadcastManager.Type.ECHO, broadcastId, message);
        BrachaBroadcastManager.Message ready = new BrachaBroadcastManager.Message(BrachaBroadcastManager.Type.READY, broadcastId, message);

        manager.broadcast(broadcastId, message);

        sendWrap(manager, mapper.writeValueAsString(echo), 2, true, true);
        sendWrap(manager, mapper.writeValueAsString(echo), 4, true, true);

        sendWrap(manager, mapper.writeValueAsString(ready), 2, true, true);
        sendWrap(manager, mapper.writeValueAsString(ready), 4, true, true);

        assertEquals("Expected exactly one output val", 1, spy.vals.size());
        assertEquals("Did not call listener with proper output!", message, spy.vals.get(0));
    }

    @Test
    public void shouldBroadcastEvenThoughPeersAttemptsToChangeValue() {
        String message = "This is the message that is broadcasted";

        Map<Integer, Consumer<String>> peers = new HashMap<>();

        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 1);
        manager.registerOnReceived(spy);

        peers.put(2, s -> {
            sendWrap(manager, goToNextMessage(s), 2);
            sendWrap(manager, replaceMessage(goToNextMessage(s), "msg"), 2);
        });
        peers.put(3, s -> {
            sendWrap(manager, goToNextMessage(s), 3);
            sendWrap(manager, replaceMessage(goToNextMessage(s), "msg"), 3);
        });
        peers.put(4, s -> {
            sendWrap(manager, goToNextMessage(s), 4);
            sendWrap(manager, replaceMessage(goToNextMessage(s), "msg"), 4);
        });

        String broadcastId = "BID";
        manager.broadcast(broadcastId, message);

        assertEquals("Expected exactly one output val", 1, spy.vals.size());
        assertEquals("Did not call listener with proper output!", message, spy.vals.get(0));
    }

    @Test
    public void twoAdversariesControlsOutput() {
        String message = "This is the message that is broadcasted";
        String adversaryMessage = "some message";

        Map<Integer, Consumer<String>> peers = new HashMap<>();


        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 1);
        manager.registerOnReceived(spy);

        peers.put(2, s -> sendWrap(manager, goToNextMessage(s), 2));
        peers.put(3, s -> sendWrap(manager, replaceMessage(s, adversaryMessage), 3, false, true));
        peers.put(4, s -> sendWrap(manager, replaceMessage(s, adversaryMessage), 4, false, true));

        String broadcastId = "BID";
        manager.broadcast(broadcastId, message);

        assertEquals("Expected exactly one output val", 1, spy.vals.size());
        assertEquals("Broadcast output should be controlled by adversaries", adversaryMessage, spy.vals.get(0));
    }


    @Test
    public void shouldBroadcastSuccessfullyOneAdversaryWrongMessage() throws JsonProcessingException {
        String message = "This is the message that is broadcasted";

        Map<Integer, Consumer<String>> peers = new HashMap<>();


        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 1);
        manager.registerOnReceived(spy);

        peers.put(2, s -> {
        });
        peers.put(3, s -> sendWrap(manager, replaceMessage(s, "newMessage"), 3, false, true));
        peers.put(4, s -> {
        });

        String broadcastId = "BID";
        BrachaBroadcastManager.Message echo = new BrachaBroadcastManager.Message(BrachaBroadcastManager.Type.ECHO,  broadcastId, message);
        BrachaBroadcastManager.Message ready = new BrachaBroadcastManager.Message(BrachaBroadcastManager.Type.READY, broadcastId, message);

        manager.broadcast(broadcastId, message);

        sendWrap(manager, mapper.writeValueAsString(echo), 2, true, true);
        sendWrap(manager, mapper.writeValueAsString(echo), 4, true, true);

        sendWrap(manager, mapper.writeValueAsString(ready), 2, true, true);
        sendWrap(manager, mapper.writeValueAsString(ready), 4, true, true);
        assertEquals("Expected exactly one output val", 1, spy.vals.size());
        assertEquals("Did not call listener with proper output!", message, spy.vals.get(0));
    }


    @Test
    public void shouldHandleReceivedBroadcastSuccessfully() throws JsonProcessingException {
        String message = "This is the message that is broadcasted";
        BrachaBroadcastManager.Message broadcast = new BrachaBroadcastManager.Message(
                BrachaBroadcastManager.Type.SEND,  "BID", message);

        Map<Integer, Consumer<String>> peers = new HashMap<>();

        DummyConsumer spy = new DummyConsumer();
        BrachaBroadcastManager manager = new BrachaBroadcastManager(peers, 0);
        manager.registerOnReceived(spy);

        peers.put(2, s -> sendWrap(manager, s, 2));
        peers.put(3, s -> sendWrap(manager, s, 3));

        manager.receive(new SimpleIncoming(mapper.writeValueAsString(broadcast), 2));

        assertEquals("Expected exactly one output val", 1, spy.vals.size());
        assertEquals("Did not call listener with proper output!", message, spy.vals.get(0));
    }

    @Test
    public void testMessageEqualsAndSerialization() throws JsonProcessingException {
        BrachaBroadcastManager.Message msg = new BrachaBroadcastManager.Message(BrachaBroadcastManager.Type.READY,  "weuigfwnjefw", "!iowuuengfwefg");

        BrachaBroadcastManager.Message deserialized = mapper.readValue(mapper.writeValueAsString(msg), BrachaBroadcastManager.Message.class);

        assertEquals("Hashes did not match for serialized/deserialized and original", msg.hashCode(), deserialized.hashCode());
        assertEquals("Serialized/deserialized did not match original", msg, deserialized);
    }

    private static class DummyConsumer implements Consumer<String> {
        private final List<String> vals = new ArrayList<>();

        @Override
        public void accept(String s) {
            vals.add(s);
        }
    }

    private static class SimpleIncoming implements Incoming<String> {
        private final String content;
        private final int id;
        private boolean valid = true;

        public SimpleIncoming(String content, int id) {
            this.content = content;
            this.id = id;
        }

        public SimpleIncoming(String content, int id, boolean valid) {
            this.content = content;
            this.id = id;
            this.valid = valid;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public String getIdentifier() {
            return Integer.toString(id);
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }
}
