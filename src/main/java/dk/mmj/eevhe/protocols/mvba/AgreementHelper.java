package dk.mmj.eevhe.protocols.mvba;

import java.util.UUID;
import java.util.function.Consumer;

public class AgreementHelper {
    private final BroadcastManager broadcastManager;
    private final ByzantineAgreementCommunicator<String> mvba;
    private final Consumer<String> onComplete;

    public AgreementHelper(BroadcastManager broadcastManager,
                           ByzantineAgreementCommunicator<String> mvba,
                           Consumer<String> onComplete) {
        this.broadcastManager = broadcastManager;
        broadcastManager.registerOnReceived(this::executeMVBA);
        this.mvba = mvba;
        this.onComplete = onComplete;
    }

    /**
     * Non-blocking call enabling a caller to broadcast a message, and then agree on it afterwards.
     *
     * @param message message to broadcast, and then agree on
     */
    public void agree(String message) {
        String id = UUID.randomUUID().toString(); //TODO Determine in deterministic way
        broadcastManager.broadcast(id, message);
    }

    private void executeMVBA(String message) {
        ByzantineAgreementCommunicator.BANotifyItem<String> agree = mvba.agree(message);

        new Thread(() -> {
            agree.waitForFinish();
            if (agree.getAgreement() != null && agree.getAgreement()) {
                onComplete.accept(message);
            }
        }).start();
    }
}
