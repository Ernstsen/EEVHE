package dk.mmj.eevhe.protocols.agreement;

/**
 * Item used to wait for some job to terminate
 */
public class NotifyItem {
    private boolean complete = false;

    /**
     * Waits for the agreement to terminate
     */
    public void waitForFinish() {
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
    public void finish() {
        synchronized (this) {
            complete = true;
            notifyAll();
        }
    }
}
