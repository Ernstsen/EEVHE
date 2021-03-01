package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.server.ServerState;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.assertEquals;

public class TestServerStateIncomingChannel {
    private IncomingChannel channel;
    private ArrayList<PartialSecretMessageDTO> expected;

    @Before
    public void setUp() {

        ArrayList<String> ids = new ArrayList<>();
        ids.add("1");
        ids.add("2");

        PartialSecretMessageDTO s1 = new PartialSecretMessageDTO(valueOf(15616), valueOf(1414), 57, 65);
        PartialSecretMessageDTO s2 = new PartialSecretMessageDTO(valueOf(1566), valueOf(141), 257, 66);
        PartialSecretMessageDTO s3 = new PartialSecretMessageDTO(valueOf(156), valueOf(14), 25, 6);

        final ServerState inst = ServerState.getInstance();
        inst.put("1", s1);
        inst.put("2", s2);
        inst.put("3", s3);

        expected = new ArrayList<>();
        expected.add(s1);
        expected.add(s2);

        channel = new ServerStateIncomingChannel(ids);

    }

    @Test
    public void receiveSecrets() {
        final List<PartialSecretMessageDTO> received = channel.receiveSecrets();
        assertEquals("Unexpected list of received secrets", expected, received);
    }
}
