package dk.mmj.eevhe.protocols.agreement.mvba;

import java.util.*;

public class ByzantineAgreementProtocolImpl implements ByzantineAgreementCommunicator<Boolean> {

    private final Communicator communicator;
    private final Map<String, List<Boolean>> received = new HashMap<>();
    private final Map<String, BANotifyItem<Boolean>> notifyItems = new HashMap<>();
    private final int peers;
    private final int t;

    public ByzantineAgreementProtocolImpl(Communicator communicator, int peers, int t) {
        this.communicator = communicator;
        communicator.registerOnReceivedBoolean(this::handleReceived);
        this.peers = peers;
        this.t = t;
    }

    @Override
    public BANotifyItem<Boolean> agree(Boolean msg) {
        String id = UUID.randomUUID().toString();
        communicator.send(id, msg);

        List<Boolean> conversation = received.computeIfAbsent(id, i -> new ArrayList<>());
        conversation.add(msg);

        BANotifyItem<Boolean> notifyItem = new BANotifyItem<>();
        notifyItems.put(id, notifyItem);
        return notifyItem;
    }

    private synchronized void handleReceived(String id, Boolean msg) {
        //        TODO: Handle adversaries messages repeatedly
        List<Boolean> conversation = received.computeIfAbsent(id, i -> new ArrayList<>());
        conversation.add(msg);

        if (conversation.size() >= peers - t) {
            int[] cnt = {0, 0};
            for (Boolean val : conversation) {
                cnt[val ? 1 : 0] += 1;
            }

            int idx = cnt[0] >= peers - (2 * t) ? 0 : 1;

            //Find d s.t. d_i = d for n-2t of values then v = true, else v=false
            Boolean d = cnt[idx] >= peers - (2 * t) ? idx == 1 : null;

            BANotifyItem<Boolean> conclusion = notifyItems.get(id);
            conclusion.setAgreement(d);
            conclusion.setMessage(d);
            conclusion.finish();
        }
    }
}
