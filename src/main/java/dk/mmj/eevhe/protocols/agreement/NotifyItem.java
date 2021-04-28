package dk.mmj.eevhe.protocols.agreement;

/**
 * Item used to wait for agreement
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
                    System.out.println(this.toString() + ". In waitForFinish() - waiting for wait()");
                    wait();
                    System.out.println(this.toString() + ". In waitForFinish() - DONE waiting for wait()");
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
            System.out.println(this.toString() + ". In finish()");
            complete = true;
            notifyAll();
        }
    }
}
