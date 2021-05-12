package dk.mmj.eevhe.protocols.agreement.broadcast;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * BroadcastManager that does not broadcast, but still calls registered handlers.
 * <br>
 * Used when there are no peers in the system, to avoid architectural changes in the system
 */
public class DummyBroadcastManager implements BroadcastManager {
    private final List<Consumer<String>> onReceivedList = new ArrayList<>();

    @Override
    public void broadcast(String broadcastId, String message) {
        onReceivedList.forEach(
                c -> c.accept(message)
        );
    }

    @Override
    public void registerOnReceived(Consumer<String> onReceived) {
        onReceivedList.add(onReceived);
    }
}
