package dk.mmj.eevhe.protocols.agreement.broadcast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.protocols.agreement.TimeoutMap;
import dk.mmj.eevhe.protocols.agreement.mvba.Incoming;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * BroadcastManager implementing Bracha Broadcast
 * <br>
 * Note that conversations expire after ten minutes
 */
public class BrachaBroadcastManager implements BroadcastManager {
    private static final int TIMEOUT_MINUTES = 10;

    private static final ObjectMapper mapper = new ObjectMapper();
    private final Map<Type, Set<String>> handledMessages = Collections.synchronizedMap(new TimeoutMap<>(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    private final Map<Integer, Consumer<String>> peerMap;
    private final Map<String, Integer> totalReceived = Collections.synchronizedMap(new TimeoutMap<>(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    private final Map<String, Map<String, Set<Type>>> recordedParticipants = Collections.synchronizedMap(new TimeoutMap<>(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    private final List<Consumer<String>> listeners = new ArrayList<>();
    private final int t;

    public BrachaBroadcastManager(Map<Integer, Consumer<String>> peerMap, int t) {
        this.peerMap = peerMap;
        this.t = t;
    }

    @Override
    public void broadcast(String broadcastId, String msg) {
        sendMessage(new Message(Type.SEND, broadcastId, msg));
    }

    @Override
    public void registerOnReceived(Consumer<String> onReceived) {
        listeners.add(onReceived);
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
     * @param incoming message to be received.
     * @throws JsonProcessingException if the {@link ObjectMapper} fails to deserialize the incoming message
     */
    public void receive(Incoming<String> incoming) throws JsonProcessingException {
        if (!incoming.isValid()) {
            return;
        }

        String msg = incoming.getContent();
        Message received = mapper.readValue(msg, Message.class);
        String broadcastId = received.getBroadcastId();

        Map<String, Set<Type>> convParticipants = recordedParticipants.computeIfAbsent(broadcastId, k -> new HashMap<>());
        Set<Type> types = convParticipants.computeIfAbsent(incoming.getIdentifier(), k -> new HashSet<>());
        if (types.contains(received.type)) {
            return;
        } else {
            types.add(received.type);
        }

        Set<String> readiedMessages = handledMessages.computeIfAbsent(Type.ECHO, k -> new HashSet<>());
        switch (received.type) {
            case SEND:
                Set<String> echoedMessages = handledMessages.computeIfAbsent(Type.SEND, k -> new HashSet<>());
                if (!echoedMessages.contains(broadcastId)) {
                    echoedMessages.add(broadcastId);
                    received.setType(Type.ECHO);
                    sendMessage(received);
                }
                break;
            case ECHO:
                totalReceived.compute(msg, (msgStr, cnt) -> cnt != null ? cnt + 1 : 1);
                if (totalReceived.get(msg) >= peerMap.size() - t && !readiedMessages.contains(broadcastId)) {
                    readiedMessages.add(broadcastId);
                    received.setType(Type.READY);
                    sendMessage(received);
                }
                break;
            case READY:
                totalReceived.compute(msg, (msgStr, cnt) -> cnt != null ? cnt + 1 : 1);
                if (totalReceived.get(msg) >= t + 1 && !readiedMessages.contains(broadcastId)) {
                    readiedMessages.add(broadcastId);
                    received.setType(Type.READY);
                    sendMessage(received);
                }
                Set<String> terminated = handledMessages.computeIfAbsent(Type.READY, k -> new HashSet<>());
                if (totalReceived.get(msg) >= peerMap.size() - t && !terminated.contains(broadcastId)) {
                    listeners.forEach(l -> l.accept(received.getMessage()));
                    terminated.add(broadcastId);
                }
                break;
        }
    }

    enum Type {
        SEND,
        ECHO,
        READY
    }

    @SuppressWarnings("unused")
    static class Message {
        Type type;
        String broadcastId;
        String message;

        public Message(Type type, String broadcastId, String message) {
            this.type = type;
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
            return
                    type == message1.type &&
                            Objects.equals(broadcastId, message1.broadcastId) &&
                            Objects.equals(message, message1.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, broadcastId, message);
        }
    }
}
