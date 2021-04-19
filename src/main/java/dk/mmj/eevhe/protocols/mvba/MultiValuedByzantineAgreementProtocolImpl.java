package dk.mmj.eevhe.protocols.mvba;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MultiValuedByzantineAgreementProtocolImpl implements ByzantineAgreementCommunicator<String> {

    private final Communicator communicator;
    private final ByzantineAgreementCommunicator<Boolean> singleValueBA;
    private final Map<String, List<String>> received = new TimeoutMap<>(5, TimeUnit.MINUTES);
    private final Map<String, BANotifyItem<String>> notifyItems = new HashMap<>();
    private final int peers;
    private final int t;

    public MultiValuedByzantineAgreementProtocolImpl(Communicator communicator, int peers, int t) {
        this.communicator = communicator;
        communicator.registerOnReceivedString(this::handleReceived);
        this.peers = peers;
        singleValueBA = new ByzantineAgreementProtocolImpl(communicator, peers, t);
        this.t = t;
    }

    @Override
    public BANotifyItem<String> agree(String msg) {
        String id = UUID.randomUUID().toString();
        communicator.send(id, msg);

        List<String> conversation = received.computeIfAbsent(id, i -> new ArrayList<>());
        conversation.add(msg);

        BANotifyItem<String> notifyItem = new BANotifyItem<>();
        notifyItems.put(id, notifyItem);
        return notifyItem;
    }

    private synchronized void handleReceived(String id, String msg) {
        List<String> conversation = received.computeIfAbsent(id, i -> new ArrayList<>());
        conversation.add(msg);

        if (conversation.size() >= peers - t) {
            received.remove(id);
            HashMap<String, Integer> countMap = new HashMap<>();
            for (String s : conversation) {
                countMap.compute(s, (k, v) -> v != null ? v + 1 : 1);
            }

            //Find d s.t. d_i = d for n-2t of values then v = true, else v=false
            String d = countMap.entrySet().stream()
                    .filter(e -> e.getValue() >= peers - (2 * t))
                    .map(Map.Entry::getKey)
                    .findAny().orElse(null);

            BANotifyItem<Boolean> agree = singleValueBA.agree(d != null);

            new Thread(() -> {
                agree.waitForFinish();
                boolean e = Boolean.TRUE.equals(agree.getAgreement());//Undecided defaults to false

                BANotifyItem<String> conclusion = notifyItems.get(id);
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
