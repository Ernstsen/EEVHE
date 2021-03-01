package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.exceptions.UnableToDecryptException;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedPersistedKeyParameters;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.interfaces.PeerCommunicator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class TestPedersenVSS {
    private ExtendedPersistedKeyParameters params;

    @Before
    public void setUp() throws Exception {
        params = new ExtendedPersistedKeyParameters(
                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1 29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245 E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F 83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D 670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9 DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510 15728E5A 8AACAA68 FFFFFFFF FFFFFFFF",
                "2"
        );
    }

    /**
     * Tests PedersenVSS with three non-corrupt participants
     */
    @Test
    public void testProtocolRunNoCorruption() {
        //Modelling communications channels
        final TestBroadcaster testBroadcaster = new TestBroadcaster();
        final PrivateCommunicationChannel channel1 = new PrivateCommunicationChannel();
        final PrivateCommunicationChannel channel2 = new PrivateCommunicationChannel();
        final PrivateCommunicationChannel channel3 = new PrivateCommunicationChannel();

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
        final PedersenVSS player1 = new PedersenVSS(testBroadcaster, channel1, commMap1, 1, params, "ID=" + 1, null, null);
        final PedersenVSS player2 = new PedersenVSS(testBroadcaster, channel2, commMap2, 2, params, "ID=" + 2, null, null);
        final PedersenVSS player3 = new PedersenVSS(testBroadcaster, channel3, commMap3, 3, params, "ID=" + 3, null, null);

        final List<PedersenVSS> players = Arrays.asList(player1, player2, player3);

        players.forEach(VSS::startProtocol);

        assertEquals("All players should have broadcasted their commitments", 3, testBroadcaster.commitments.size());

        players.forEach(VSS::handleReceivedValues);

        assertEquals("No players should have lodged a complaint", 0, testBroadcaster.complaints.size());

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
        final TestBroadcaster testBroadcaster = new TestBroadcaster();
        final PrivateCommunicationChannel channel1 = new PrivateCommunicationChannel();
        final PrivateCommunicationChannel channel2 = new PrivateCommunicationChannel();
        final PrivateCommunicationChannel channel3 = new PrivateCommunicationChannel();

        final HashMap<Integer, PeerCommunicator> commMap1 = new HashMap<>();
        final HashMap<Integer, PeerCommunicator> commMap3 = new HashMap<>();

        commMap1.put(2, channel2);
        commMap1.put(3, channel3);

        commMap3.put(1, channel1);
        commMap3.put(2, channel2);

        //Creating players TODO: use Gennaro DKG instead
        final PedersenVSS player1 = new PedersenVSS(testBroadcaster, channel1, commMap1, 1, params, "ID=" + 1, null, null);
        final PedersenVSS player3 = new PedersenVSS(testBroadcaster, channel3, commMap3, 3, params, "ID=" + 3, null, null);

        final List<PedersenVSS> players = Arrays.asList(player1, player3);

        players.forEach(VSS::startProtocol);

        assertEquals("All players should have broadcasted their commitments", 2, testBroadcaster.commitments.size());

        players.forEach(VSS::handleReceivedValues);

        assertEquals("No players should have lodged a complaint", 0, testBroadcaster.complaints.size());

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
     * Runs PedersenVSS with no private channels. All secret values should be made public in this case
     */
    @Test
    public void testProtocolWithComplaints() {
        //Modelling communications channels
        final TestBroadcaster testBroadcaster = new TestBroadcaster();
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
        final PedersenVSS player1 = new PedersenVSS(testBroadcaster, brokenIncoming1, commMap1, 1, params, "ID=" + 1, null, null);
        final PedersenVSS player2 = new PedersenVSS(testBroadcaster, brokenIncoming2, commMap2, 2, params, "ID=" + 2, null, null);
        final PedersenVSS player3 = new PedersenVSS(testBroadcaster, brokenIncoming3, commMap3, 3, params, "ID=" + 3, null, null);

        final List<PedersenVSS> players = Arrays.asList(player1, player2, player3);

        players.forEach(VSS::startProtocol);

        assertEquals("All players should have broadcasted their commitments", 3, testBroadcaster.commitments.size());

        players.forEach(VSS::handleReceivedValues);

        assertEquals("All players should have lodged 2 complaints", 6, testBroadcaster.complaints.size());

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

    static class TestBroadcaster implements Broadcaster {
        final List<CommitmentDTO> commitments = new ArrayList<>();
        final List<ComplaintDTO> complaints = new ArrayList<>();
        final List<ComplaintResolveDTO> resolves = new ArrayList<>();


        @Override
        public void commit(CommitmentDTO commitmentDTO) {
            commitments.add(commitmentDTO);
        }

        @Override
        public List<CommitmentDTO> getCommitments() {
            return commitments;
        }

        @Override
        public void complain(ComplaintDTO complaint) {
            complaints.add(complaint);
        }

        @Override
        public List<ComplaintDTO> getComplaints() {
            return complaints;
        }

        @Override
        public void resolveComplaint(ComplaintResolveDTO complaintResolveDTO) {
            resolves.add(complaintResolveDTO);
        }

        @Override
        public List<ComplaintResolveDTO> getResolves() {
            return resolves;
        }
    }

    static class PrivateCommunicationChannel implements IncomingChannel, PeerCommunicator {
        private final List<PartialSecretMessageDTO> messages = new ArrayList<>();

        @Override
        public List<PartialSecretMessageDTO> receiveSecrets() {
            return messages;
        }

        @Override
        public void sendSecret(PartialSecretMessageDTO value) {
            messages.add(value);
        }
    }
}
