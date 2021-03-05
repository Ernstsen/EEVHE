package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.keygeneration.ExtendedPersistedKeyParameters;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.VSS;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestGennaroFeldmanVSS {
    private ExtendedPersistedKeyParameters params;

    @Before
    public void setUp() throws Exception {
        params = new ExtendedPersistedKeyParameters(
                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1 29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245 E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F 83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D 670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9 DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510 15728E5A 8AACAA68 FFFFFFFF FFFFFFFF",
                "2"
        );
    }

    /**
     * Tests FeldmanVSS with three non-corrupt participants
     */
    @Test
    public void testProtocolRunNoCorruption() {
        //Modelling communications channels
        final TestPedersenVSS.TestBroadcaster testBroadcaster = new TestPedersenVSS.TestBroadcaster();
        final TestPedersenVSS.PrivateCommunicationChannel channel1 = new TestPedersenVSS.PrivateCommunicationChannel();
        final TestPedersenVSS.PrivateCommunicationChannel channel2 = new TestPedersenVSS.PrivateCommunicationChannel();
        final TestPedersenVSS.PrivateCommunicationChannel channel3 = new TestPedersenVSS.PrivateCommunicationChannel();

        final HashMap<Integer, PeerCommunicator> commMap1 = new HashMap<>();
        final HashMap<Integer, PeerCommunicator> commMap2 = new HashMap<>();
        final HashMap<Integer, PeerCommunicator> commMap3 = new HashMap<>();

        commMap1.put(2, channel2);
        commMap1.put(3, channel3);

        commMap2.put(1, channel1);
        commMap2.put(3, channel3);

        commMap3.put(1, channel1);
        commMap3.put(2, channel2);

        //Creating players TODO: use GennaroDKG instead
        final GennaroFeldmanVSS player1 = new GennaroFeldmanVSS(testBroadcaster, channel1, commMap1, 1, params, "ID=" + 1, null, null);
        final GennaroFeldmanVSS player2 = new GennaroFeldmanVSS(testBroadcaster, channel2, commMap2, 2, params, "ID=" + 2, null, null);
        final GennaroFeldmanVSS player3 = new GennaroFeldmanVSS(testBroadcaster, channel3, commMap3, 3, params, "ID=" + 3, null, null);

        final List<GennaroFeldmanVSS> players = Arrays.asList(player1, player2, player3);

        players.forEach(VSS::startProtocol);

        assertEquals("All players should have broadcasted their commitments", 3, testBroadcaster.commitments.size());

        players.forEach(VSS::handleReceivedValues);

        assertEquals("No players should have lodged a complaint", 0, testBroadcaster.pedersenComplaints.size());

        players.forEach(VSS::handleComplaints);

        //No change in state, as no complaints has been registered

        players.forEach(VSS::applyResolves);

        //No change in state, as there were no complaints to be resolved

        final BigInteger partialSecret1 = player1.output();
        final BigInteger partialSecret2 = player2.output();
        final BigInteger partialSecret3 = player3.output();

        assertNotNull("Partial secret was null", partialSecret1);
        assertNotNull("Partial secret was null", partialSecret2);
        assertNotNull("Partial secret was null", partialSecret3);
    }

    @Test
    public void testProtocolWith1NonParticipant() {
        //Modelling communications channels
        final TestPedersenVSS.TestBroadcaster testBroadcaster = new TestPedersenVSS.TestBroadcaster();
        final TestPedersenVSS.PrivateCommunicationChannel channel1 = new TestPedersenVSS.PrivateCommunicationChannel();
        final TestPedersenVSS.PrivateCommunicationChannel channel2 = new TestPedersenVSS.PrivateCommunicationChannel();
        final TestPedersenVSS.PrivateCommunicationChannel channel3 = new TestPedersenVSS.PrivateCommunicationChannel();

        final HashMap<Integer, PeerCommunicator> commMap1 = new HashMap<>();
        final HashMap<Integer, PeerCommunicator> commMap3 = new HashMap<>();

        commMap1.put(2, channel2);
        commMap1.put(3, channel3);

        commMap3.put(1, channel1);
        commMap3.put(2, channel2);

        //Creating players TODO: use Gennaro DKG instead
        final GennaroFeldmanVSS player1 = new GennaroFeldmanVSS(testBroadcaster, channel1, commMap1, 1, params, "ID=" + 1, null, null);
        final GennaroFeldmanVSS player3 = new GennaroFeldmanVSS(testBroadcaster, channel3, commMap3, 3, params, "ID=" + 3, null, null);

        final List<GennaroFeldmanVSS> players = Arrays.asList(player1, player3);

        players.forEach(VSS::startProtocol);

        assertEquals("All players should have broadcasted their commitments", 2, testBroadcaster.commitments.size());

        players.forEach(VSS::handleReceivedValues);

        assertEquals("No players should have lodged a complaint", 0, testBroadcaster.feldmanComplaints.size());

        players.forEach(VSS::handleComplaints);

        //No change in state, as no complaints has been registered

        players.forEach(VSS::applyResolves);

        //No change in state, as there were no complaints to be resolved

        final BigInteger partialSecret1 = player1.output();
        final BigInteger partialSecret3 = player3.output();

        assertNotNull("Partial secret was null", partialSecret1);
        assertNotNull("Partial secret was null", partialSecret3);
    }

    /**
     * Runs FeldmanVSS with no private channels. All secret values should be made public in this case
     */
    @Test
    public void testProtocolWithComplaints() {
        //Modelling communications channels
        final TestPedersenVSS.TestBroadcaster testBroadcaster = new TestPedersenVSS.TestBroadcaster();
        final PeerCommunicator brokenChannel = (m) -> {
        };

        List<PartialSecretMessageDTO> receivedPeer1 = Arrays.asList(
                new PartialSecretMessageDTO(BigInteger.ONE, BigInteger.ONE, 1, 2),
                new PartialSecretMessageDTO(BigInteger.ONE, BigInteger.ONE, 1, 3));
        List<PartialSecretMessageDTO> receivedPeer2 = Arrays.asList(
                new PartialSecretMessageDTO(BigInteger.ONE, BigInteger.ONE, 2, 1),
                new PartialSecretMessageDTO(BigInteger.ONE, BigInteger.ONE, 2, 3));
        List<PartialSecretMessageDTO> receivedPeer3 = Arrays.asList(
                new PartialSecretMessageDTO(BigInteger.ONE, BigInteger.ONE, 3, 1),
                new PartialSecretMessageDTO(BigInteger.ONE, BigInteger.ONE, 3, 2));

        final IncomingChannel brokenIncoming1 = () -> receivedPeer1;
        final IncomingChannel brokenIncoming2 = () -> receivedPeer2;
        final IncomingChannel brokenIncoming3 = () -> receivedPeer3;

        final HashMap<Integer, PeerCommunicator> commMap1 = new HashMap<>();
        final HashMap<Integer, PeerCommunicator> commMap2 = new HashMap<>();
        final HashMap<Integer, PeerCommunicator> commMap3 = new HashMap<>();

        commMap1.put(2, brokenChannel);
        commMap1.put(3, brokenChannel);

        commMap2.put(1, brokenChannel);
        commMap2.put(3, brokenChannel);

        commMap3.put(1, brokenChannel);
        commMap3.put(2, brokenChannel);

        //Creating players
        final GennaroFeldmanVSS player1 = new GennaroFeldmanVSS(testBroadcaster, brokenIncoming1, commMap1, 1, params, "ID=" + 1, null, null);
        final GennaroFeldmanVSS player2 = new GennaroFeldmanVSS(testBroadcaster, brokenIncoming2, commMap2, 2, params, "ID=" + 2, null, null);
        final GennaroFeldmanVSS player3 = new GennaroFeldmanVSS(testBroadcaster, brokenIncoming3, commMap3, 3, params, "ID=" + 3, null, null);

        final List<GennaroFeldmanVSS> players = Arrays.asList(player1, player2, player3);

        players.forEach(VSS::startProtocol);

        assertEquals("All players should have broadcasted their commitments", 3, testBroadcaster.commitments.size());

        players.forEach(VSS::handleReceivedValues);

        assertEquals("All players should have lodged 2 complaints", 6, testBroadcaster.feldmanComplaints.size());

        players.forEach(VSS::handleComplaints);

        //No change in state, as no complaints has been registered

        players.forEach(VSS::applyResolves);

        //No change in state, as there were no complaints to be resolved

        final BigInteger partialSecret1 = player1.output();
        final BigInteger partialSecret2 = player2.output();
        final BigInteger partialSecret3 = player3.output();

        assertNotNull("Partial secret was null", partialSecret1);
        assertNotNull("Partial secret was null", partialSecret2);
        assertNotNull("Partial secret was null", partialSecret3);
    }

    @Ignore
    @Test
    public void testProtocol1Corrupt() {
        //TODO!
    }
}