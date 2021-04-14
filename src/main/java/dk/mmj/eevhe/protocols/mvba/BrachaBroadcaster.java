package dk.mmj.eevhe.protocols.mvba;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Consumer;

public class BrachaBroadcaster implements Broadcaster {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final HashSet<String> echoedMessages = new HashSet<>();
    private final HashSet<String> readiedMessages = new HashSet<>();
    private final Map<Integer, Consumer<String>> peerMap;
    private final List<Consumer<String>> listeners = new ArrayList<>();
    private final int peerId;
    private final int t;

    public BrachaBroadcaster(Map<Integer, Consumer<String>> peerMap, int peerId, int t) {
        this.peerMap = peerMap;
        this.peerId = peerId;
        this.t = t;
    }

    @Override
    public void broadcast(String broadcasterId, String msg) {
        sendMessage(new Message(Type.SEND, peerId, broadcasterId, msg));
    }

    /**
     * Serializes a message object into a String and send it to all peers in peerMap.
     *
     * @param message message object to be sent.
     */
    private void sendMessage(Message message) {
        try {
            String messageString = mapper.writeValueAsString(message);
            peerMap.values().forEach(c -> c.accept(messageString));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message object.", e);
        }
    }

    /**
     * Receives a message, and handles it according to its type.
     *
     * @param msg message to be received.
     */
    public void receive(String msg) throws JsonProcessingException {
        Message received = mapper.readValue(msg, Message.class);
        Map<String, Integer> totalReceived = new HashMap<>();
        String broadcastId = received.getBroadcastId();

        switch (received.type) {
            case SEND:
                if (!echoedMessages.contains(broadcastId)) {
                    echoedMessages.add(broadcastId);
                    received.setType(Type.ECHO);
                    sendMessage(received);
                }
                break;
            case ECHO:
                totalReceived.compute(msg, (msgStr, cnt) -> cnt != null ? cnt + 1 : 1);
                if (totalReceived.get(msg) >= peerMap.size() - t) {
                    readiedMessages.add(broadcastId);
                    received.setType(Type.READY);
                    sendMessage(received);
                }
                break;
            case READY:
                totalReceived.compute(msg, (msgStr, cnt) -> cnt != null ? cnt + 1 : 1);
                if (totalReceived.get(msg) >= t + 1 && !readiedMessages.contains(broadcastId)) {
                    received.setType(Type.READY);
                    sendMessage(received);
                }
                if (totalReceived.get(msg) >= peerMap.size() - t) {
                    listeners.forEach(l -> l.accept(received.getMessage()));
                }
                break;
        }
    }

    public void registerListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    enum Type {
        SEND,
        ECHO,
        READY
    }

    private static class Message {
        Type type;
        int senderId;
        String broadcastId;
        String message;

        public Message(Type type, int senderId, String broadcastId, String message) {
            this.type = type;
            this.senderId = senderId;
            this.broadcastId = broadcastId;
            this.message = message;
        }

        public Message() {

        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public int getSenderId() {
            return senderId;
        }

        public void setSenderId(int senderId) {
            this.senderId = senderId;
        }

        public String getBroadcastId() {
            return broadcastId;
        }

        public void setBroadcastId(String broadcastId) {
            this.broadcastId = broadcastId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Message message1 = (Message) o;
            return senderId == message1.senderId &&
                    type == message1.type &&
                    Objects.equals(broadcastId, message1.broadcastId) &&
                    Objects.equals(message, message1.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, senderId, broadcastId, message);
        }
    }
}
