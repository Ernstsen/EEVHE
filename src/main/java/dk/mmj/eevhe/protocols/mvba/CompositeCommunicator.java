package dk.mmj.eevhe.protocols.mvba;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CompositeCommunicator implements Communicator {
    private final BiConsumer<String, String> onStringSend;
    private final BiConsumer<String, Boolean> onBoolSend;
    private final Consumer<BiConsumer<String, String>> onReceivedString;
    private final Consumer<BiConsumer<String, Boolean>> onReceivedBool;

    /**
     * Constructor taking functional interfaces for each supported functionality
     *
     * @param onStringSend     consumer for sending string
     * @param onBoolSend       consumer for sending boolean
     * @param onReceivedString handler for registering received strings
     * @param onReceivedBool   handler for registering received boolean
     */
    public CompositeCommunicator(
            BiConsumer<String, String> onStringSend,
            BiConsumer<String, Boolean> onBoolSend,
            Consumer<BiConsumer<String, String>> onReceivedString,
            Consumer<BiConsumer<String, Boolean>> onReceivedBool
    ) {
        this.onStringSend = onStringSend;
        this.onBoolSend = onBoolSend;
        this.onReceivedString = onReceivedString;
        this.onReceivedBool = onReceivedBool;
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
    public void registerOnReceivedString(BiConsumer<String, String> onReceived) {
        onReceivedString.accept(onReceived);
    }

    @Override
    public void registerOnReceivedBoolean(BiConsumer<String, Boolean> onReceived) {
        onReceivedBool.accept(onReceived);
    }
}
