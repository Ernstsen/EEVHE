package dk.mmj.eevhe.protocols.agreement;

import dk.mmj.eevhe.protocols.agreement.broadcast.BrachaBroadcastManager;
import dk.mmj.eevhe.protocols.agreement.mvba.ByzantineAgreementCommunicator;
import dk.mmj.eevhe.protocols.agreement.mvba.MultiValuedByzantineAgreementProtocolImpl;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class TestAgreementHelper {

    @Test
    public void testRegistersWithBroadcaster() {
        BrachaBroadcastManager broadcast = mock(BrachaBroadcastManager.class);
        ByzantineAgreementCommunicator<String> mvba = mock(MultiValuedByzantineAgreementProtocolImpl.class);
        new AgreementHelper(broadcast, mvba, null);
        verify(broadcast, times(1)).registerOnReceived(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStartsByBroadcasting() throws InterruptedException {
        IsCompleteListener isCompleteListener = new IsCompleteListener();

        BrachaBroadcastManager broadcast = mock(BrachaBroadcastManager.class);
        ArgumentCaptor<Consumer<String>> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        doNothing().when(broadcast).registerOnReceived(consumerCaptor.capture());

        ByzantineAgreementCommunicator<String> mvba = mock(MultiValuedByzantineAgreementProtocolImpl.class);
        ByzantineAgreementCommunicator.BANotifyItem<String> notifyItem = mock(ByzantineAgreementCommunicator.BANotifyItem.class);
        when(mvba.agree(any(), any())).thenReturn(notifyItem);
        when(notifyItem.getAgreement()).thenReturn(true);


        AgreementHelper helper = new AgreementHelper(broadcast, mvba, isCompleteListener::setMessage);
        Consumer<String> onReceive = consumerCaptor.getValue();
        assertNotNull("Failed to extract onReceived hook", onReceive);

        String message = "This is the message in question";
        helper.agree(message);

        verify(broadcast, times(1)).broadcast(any(), eq(message));

        //Broadcast terminates and passes message to MVBA
        onReceive.accept(message);

        Thread.sleep(50);
        assertEquals("OnComplete was not called, as expected", message, isCompleteListener.message);
    }


    private static class IsCompleteListener {
        String message;

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
