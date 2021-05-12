package dk.mmj.eevhe.protocols.agreement;

import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.protocols.agreement.broadcast.BroadcastManager;
import dk.mmj.eevhe.protocols.agreement.mvba.ByzantineAgreementCommunicator;

import java.util.function.Consumer;

/**
 * Helper class for reaching agreement between a number of peers, and executing some functionality only when this is the
 * case
 */
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
        String id = new String(SecurityUtils.hash(message.getBytes()));
        broadcastManager.broadcast(id, message);
    }

    private void executeMVBA(String message) {
        String id = new String(SecurityUtils.hash(message.getBytes()));
        ByzantineAgreementCommunicator.BANotifyItem<String> agree = mvba.agree(message, id);

        new Thread(() -> {
            agree.waitForFinish();
            if (agree.getAgreement() != null && agree.getAgreement()) {
                onComplete.accept(message);
            }
        }).start();
    }
}
