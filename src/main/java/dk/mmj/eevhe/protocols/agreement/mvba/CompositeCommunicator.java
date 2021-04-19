package dk.mmj.eevhe.protocols.agreement.mvba;

import java.util.function.BiConsumer;

public class CompositeCommunicator implements Communicator {
    private final BiConsumer<String, String> onStringSend;
    private final BiConsumer<String, Boolean> onBoolSend;
    private BiConsumer<String, String> onReceivedString;
    private BiConsumer<String, Boolean> onReceivedBoolean;

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
    public void receive(String BAId, String msg) {
        onReceivedString.accept(BAId, msg);
    }

    @Override
    public void receive(String BAId, Boolean msg) {
        onReceivedBoolean.accept(BAId, msg);
    }

    @Override
    public void registerOnReceivedString(BiConsumer<String, String> onReceived) {
        this.onReceivedString = onReceived;
    }

    @Override
    public void registerOnReceivedBoolean(BiConsumer<String, Boolean> onReceived) {
        this.onReceivedBoolean = onReceived;
    }
}
