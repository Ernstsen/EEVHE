package dk.mmj.eevhe.protocols.agreement.mvba;

import dk.mmj.eevhe.protocols.agreement.TimeoutMap;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ByzantineAgreementProtocolImpl implements ByzantineAgreementCommunicator<Boolean> {

    private final Communicator communicator;
    private final Map<String, Map<String, Boolean>> received = new TimeoutMap<>(5, TimeUnit.MINUTES);
    private final Map<String, BANotifyItem<Boolean>> notifyItems = new HashMap<>();
    private final int peers;
    private final int t;
    private final String identity;

    /**
     * @param communicator handle for sending/receiving messages
     * @param peers        #peers
     * @param t            t value for the protocol - requirement for safety: t<n/5
     * @param identity     identity of this entity
     */
    public ByzantineAgreementProtocolImpl(Communicator communicator, int peers, int t, String identity) {
        this.communicator = communicator;
        communicator.registerOnReceivedBoolean(this::handleReceived);
        this.peers = peers;
        this.t = t;
        this.identity = identity;
    }

    @Override
    public BANotifyItem<Boolean> agree(Boolean msg) {
        String id = UUID.randomUUID().toString();
        communicator.send(id, msg);

        BANotifyItem<Boolean> notifyItem = new BANotifyItem<>();
        notifyItems.put(id, notifyItem);

        handleReceived(new CompositeIncoming<>(new Communicator.Message<>(id, msg), identity, () -> true));

        return notifyItem;
    }

    private synchronized void handleReceived(Incoming<Communicator.Message<Boolean>> incoming) {
        if (!incoming.isValid()) {
            return;
        }

        String id = incoming.getContent().getBaId();
        Boolean msg = incoming.getContent().getMessage();

        Map<String, Boolean> conversation = received.computeIfAbsent(id, i -> new HashMap<>());

        String sender = incoming.getIdentifier();
        if (conversation.containsKey(sender)) {
            return;
        } else {
            conversation.put(sender, msg);
        }


        if (conversation.size() >= peers - t) {
            int[] cnt = {0, 0};
            for (Boolean val : conversation.values()) {
                cnt[val ? 1 : 0] += 1;
            }

            int idx = cnt[1] >= peers - (2 * t) ? 1 : 0;

            //Find d s.t. d_i = d for n-2t of values then v = true, else v=false
            Boolean d = cnt[idx] >= peers - (2 * t) ? idx == 1 : null;

            BANotifyItem<Boolean> conclusion = notifyItems.get(id);
            conclusion.setAgreement(d);
            conclusion.setMessage(d);
            conclusion.finish();
        }
    }
}
