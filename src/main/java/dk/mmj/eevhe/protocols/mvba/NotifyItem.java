package dk.mmj.eevhe.protocols.mvba;

/**
 * Item used to wait for agreement
 */
public class NotifyItem {
    private boolean complete = false;

    /**
     * Waits for the agreement to terminate
     */
    void waitForFinish() {
        synchronized (this) {
            if (!complete) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread was interrupted", e);
                }
            }
        }
    }

    /**
     * Mark the item as finished, and notify all waiting threads
     */
    void finish() {
        synchronized (this) {
            complete = true;
            notifyAll();
        }
    }
}
