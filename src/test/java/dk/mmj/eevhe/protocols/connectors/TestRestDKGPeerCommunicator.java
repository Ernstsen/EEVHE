package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.entities.SignedEntity;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestRestDKGPeerCommunicator {
    private WebTarget peer;
    private WebTarget target;
    private RestDKGPeerCommunicator communicator;

    @Before
    public void setUp() throws Exception {
        peer = mock(WebTarget.class);
        AsymmetricKeyParameter sk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
        communicator = new RestDKGPeerCommunicator(peer, sk);
        target = mock(WebTarget.class);

        when(peer.path("partialSecret")).thenReturn(target);
    }

    @Test
    public void sendSecret() {
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(target.request()).thenReturn(commitBuilder);

        final List<SignedEntity<PartialSecretMessageDTO>> secrets = new ArrayList<>();
        when(peer.path("partialSecret").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<SignedEntity<PartialSecretMessageDTO>> argument = invocationOnMock.getArgument(0);
                    secrets.add(argument.getEntity());
                    final Response resp = mock(Response.class);
                    when(resp.getStatus()).thenReturn(204);
                    return resp;
                });

        final PartialSecretMessageDTO s1 = new PartialSecretMessageDTO(valueOf(15616), valueOf(1234), 57, 65);
        final PartialSecretMessageDTO s2 = new PartialSecretMessageDTO(valueOf(1566), valueOf(123), 257, 66);

        communicator.sendSecret(s1);
        communicator.sendSecret(s2);
        List<PartialSecretMessageDTO> sent = Arrays.asList(s1, s2);
        assertEquals("Only two secrets has been sent", secrets.size(), 2);
        assertTrue("First secret not properly handled", sent.contains(secrets.get(0).getEntity()));
        assertTrue("Second secret not properly handled", sent.contains(secrets.get(1).getEntity()));

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

        communicator.sendSecret(new PartialSecretMessageDTO(valueOf(15616), valueOf(1234), 57, 65));
        communicator.sendSecret(new PartialSecretMessageDTO(valueOf(1566), valueOf(123), 257, 66));
    }
}
