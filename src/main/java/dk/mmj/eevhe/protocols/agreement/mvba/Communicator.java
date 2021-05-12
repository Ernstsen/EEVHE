package dk.mmj.eevhe.protocols.agreement.mvba;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Communicator used in Byzantine Agreement Protocols
 */
public interface Communicator {

    /**
     * Transmits string for BA
     *
     * @param BAId BA id
     * @param msg  message
     */
    void send(String BAId, String msg);

    /**
     * Transmits boolean for BA
     *
     * @param BAId BA id
     * @param msg  message
     */
    void send(String BAId, Boolean msg);


    /**
     * Communicator receives string msg for BA with ID
     *
     * @param incoming incoming message
     */
    void receiveString(Incoming<Message<String>> incoming);

    /**
     * Communicator receives boolean msg for BA with ID
     *
     * @param incoming incoming message
     */
    void receiveBool(Incoming<Message<Boolean>> incoming);

    /**
     * Registers a receiving handler for Strings.
     * <br>
     * Depending on implementation, only one, or multiple, can be registered at a time
     *
     * @param onReceived handler for received string BAs
     */
    void registerOnReceivedString(Consumer<Incoming<Message<String>>> onReceived);

    /**
     * Registers a receiving handler for booleans.
     * <br>
     * Depending on implementation, only one, or multiple, can be registered at a time
     *
     * @param onReceived handler for received boolean BAs
     */
    void registerOnReceivedBoolean(Consumer<Incoming<Message<Boolean>>> onReceived);

    class Message<T> {
        private String baId;
        private T message;

        public Message() {
        }

        public Message(String baId, T message) {
            this.baId = baId;
            this.message = message;
        }

        public String getBaId() {
            return baId;
        }

        public void setBaId(String baId) {
            this.baId = baId;
        }

        public T getMessage() {
            return message;
        }

        public void setMessage(T message) {
            this.message = message;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Message<?> message1 = (Message<?>) o;
            return Objects.equals(baId, message1.baId) && Objects.equals(message, message1.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(baId, message);
        }

        @Override
        public String toString() {
            return "Message{" +
                    "baId='" + baId + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

}
