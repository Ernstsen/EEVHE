package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestRestPeerCommunicator {
    private WebTarget peer;
    private WebTarget target;
    private RestPeerCommunicator communicator;

    @Before
    public void setUp() throws Exception {
        peer = mock(WebTarget.class);
        communicator = new RestPeerCommunicator(peer);
        target = mock(WebTarget.class);

        when(peer.path("partialSecret")).thenReturn(target);
    }

    @Test
    public void sendSecret() {
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(target.request()).thenReturn(commitBuilder);

        final List<PartialSecretMessageDTO> secrets = new ArrayList<>();
        when(peer.path("partialSecret").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<PartialSecretMessageDTO> argument = invocationOnMock.getArgument(0);
                    secrets.add(argument.getEntity());
                    final Response resp = mock(Response.class);
                    when(resp.getStatus()).thenReturn(204);
                    return resp;
                });

        final PartialSecretMessageDTO s1 = new PartialSecretMessageDTO(valueOf(15616), 57, 65);
        final PartialSecretMessageDTO s2 = new PartialSecretMessageDTO(valueOf(1566), 257, 66);

        communicator.sendSecret(s1);
        communicator.sendSecret(s2);
        assertTrue("First secret not properly handled", secrets.contains(s1));
        assertTrue("Second secret not properly handled", secrets.contains(s2));

    }

    /**
     * Asserts that an error will not cause a crash
     */
    @Test
    public void sendSecretWithError() {
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(target.request()).thenReturn(commitBuilder);

        final Response errorResponse = mock(Response.class);
        when(errorResponse.getStatus()).thenReturn(500);

        when(peer.path("partialSecret").request().post(any()))
                .thenReturn(errorResponse);

        communicator.sendSecret(new PartialSecretMessageDTO(valueOf(15616), 57, 65));
        communicator.sendSecret(new PartialSecretMessageDTO(valueOf(1566), 257, 66));
    }
}