package dk.mmj.eevhe.protocols.mvba;

import java.util.UUID;

public class AgreementHelper {
    private final Broadcaster broadcaster;
    private final ByzantineAgreementCommunicator<String> mvba;

    public AgreementHelper(Broadcaster broadcaster, ByzantineAgreementCommunicator<String> mvba) {
        this.broadcaster = broadcaster;
        this.mvba = mvba;
    }

    /**
     * Non-blocking call enabling a caller to broadcast a message, and then agree on it afterwards.
     *
     * @param message    message to broadcast, and then agree on
     * @param onComplete runnable to be executed upon reaching agreement
     */
    public void agree(String message, Runnable onComplete) {
        String id = UUID.randomUUID().toString(); //TODO Determine in deterministic way
        broadcaster.broadcast(id, message);

        ByzantineAgreementCommunicator.BANotifyItem<String> agree = mvba.agree(message);

        new Thread(() -> {
            agree.waitForFinish();
            if (agree.getAgreement() != null && agree.getAgreement()) {
                onComplete.run();
            }
        }).start();

    }
}
