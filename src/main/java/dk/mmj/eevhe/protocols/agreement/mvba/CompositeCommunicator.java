package dk.mmj.eevhe.protocols.agreement.mvba;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CompositeCommunicator implements Communicator {
    private final BiConsumer<String, String> onStringSend;
    private final BiConsumer<String, Boolean> onBoolSend;
    private Consumer<Incoming<Message<String>>> onReceivedString;
    private Consumer<Incoming<Message<Boolean>>> onReceivedBoolean;

    /**
     * Constructor taking functional interfaces for each supported functionality
     *
     * @param onStringSend consumer for sending string
     * @param onBoolSend   consumer for sending boolean
     */
    public CompositeCommunicator(
            BiConsumer<String, String> onStringSend,
            BiConsumer<String, Boolean> onBoolSend
    ) {
        this.onStringSend = onStringSend;
        this.onBoolSend = onBoolSend;
    }

    @Override
    public void send(String BAId, String msg) {
        onStringSend.accept(BAId, msg);
    }

    @Override
    public void send(String BAId, Boolean msg) {
        onBoolSend.accept(BAId, msg);
    }

    @Override
    public void receiveString(Incoming<Message<String>> incoming) {
        onReceivedString.accept(incoming);
    }

    @Override
    public void receiveBool(Incoming<Message<Boolean>> incoming) {
        onReceivedBoolean.accept(incoming);
    }

    @Override
    public void registerOnReceivedString(Consumer<Incoming<Message<String>>> onReceived) {
        this.onReceivedString = onReceived;
    }

    @Override
    public void registerOnReceivedBoolean(Consumer<Incoming<Message<Boolean>>> onReceived) {
        this.onReceivedBoolean = onReceived;
    }
}
