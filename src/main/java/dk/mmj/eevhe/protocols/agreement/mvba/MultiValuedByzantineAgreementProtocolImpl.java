package dk.mmj.eevhe.protocols.agreement.mvba;

import dk.mmj.eevhe.protocols.agreement.TimeoutMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MultiValuedByzantineAgreementProtocolImpl implements ByzantineAgreementCommunicator<String> {

    private final Communicator communicator;
    private final ByzantineAgreementCommunicator<Boolean> singleValueBA;
    private final Map<String, Map<String, String>> received = new TimeoutMap<>(5, TimeUnit.MINUTES);
    private final Map<String, BANotifyItem<String>> notifyItems = new HashMap<>();
    private final int peers;
    private final int t;
    private final String identity;

    /**
     * @param communicator handle for sending/receiving messages
     * @param peers        #peers
     * @param t            t value for the protocol - requirement for safety: t<n/5
     * @param identity     identity of this entity
     */
    public MultiValuedByzantineAgreementProtocolImpl(Communicator communicator, int peers, int t, String identity) {
        this.communicator = communicator;
        communicator.registerOnReceivedString(this::handleReceived);
        this.peers = peers;
        singleValueBA = new ByzantineAgreementProtocolImpl(communicator, peers, t, identity);
        this.t = t;
        this.identity = identity;
    }

    @Override
    public BANotifyItem<String> agree(String msg, String id) {
        communicator.send(id, msg);

        BANotifyItem<String> notifyItem = notifyItems.computeIfAbsent(id, k -> new BANotifyItem<>());

        handleReceived(new CompositeIncoming<>(new Communicator.Message<>(id, msg), identity, () -> true));

        return notifyItem;
    }

    private synchronized void handleReceived(Incoming<Communicator.Message<String>> incoming) {
        if (!incoming.isValid()) {
            return;
        }

        String id = incoming.getContent().getBaId();
        String msg = incoming.getContent().getMessage();

        Map<String, String> conversation = received.computeIfAbsent(id, i -> new HashMap<>());

        String sender = incoming.getIdentifier();
        if (conversation.containsKey(sender)) {
            return;
        } else {
            conversation.put(sender, msg);
        }

        if (conversation.size() >= peers - t) {
            received.remove(id);
            HashMap<String, Integer> countMap = new HashMap<>();
            for (String s : conversation.values()) {
                countMap.compute(s, (k, v) -> v != null ? v + 1 : 1);
            }

            //Find d s.t. d_i = d for n-2t of values then v = true, else v=false
            String d = countMap.entrySet().stream()
                    .filter(e -> e.getValue() >= peers - (2 * t))
                    .map(Map.Entry::getKey)
                    .findAny().orElse(null);

            BANotifyItem<Boolean> agree = singleValueBA.agree(d != null, id);

            new Thread(() -> {
                agree.waitForFinish();
                boolean e = Boolean.TRUE.equals(agree.getAgreement());//Undecided defaults to false

                BANotifyItem<String> conclusion = notifyItems.computeIfAbsent(id, k -> new BANotifyItem<>());
                if (e) {
                    //succeed
                    conclusion.setAgreement(true);
                    conclusion.setMessage(d);
                } else {
                    //fail
                    conclusion.setAgreement(false);
                }
                conclusion.finish();
            }).start();
        }
    }
}
