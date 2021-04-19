package dk.mmj.eevhe.protocols.mvba;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class TestNotifyItem {

    @Test
    public void testWaitingWorks() throws InterruptedException {

        ArrayList<Integer> ints = new ArrayList<>();

        NotifyItem notifyItem = new NotifyItem();


        Thread thread = new Thread(() -> {
            notifyItem.waitForFinish();
            ints.add(2);
        });
        thread.start();

        ints.add(1);
        notifyItem.finish();

        thread.join();

        assertEquals("Did not wait for the notifyItem properly",
                Arrays.asList(1, 2),
                ints
        );
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsExceptionInsteadOfReturnOnInterrupt(){
        NotifyItem notifyItem = new NotifyItem();
        Thread main = Thread.currentThread();

        new Thread(() -> {
            try {
                Thread.sleep(500);
                main.interrupt();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        notifyItem.waitForFinish();
    }
}
