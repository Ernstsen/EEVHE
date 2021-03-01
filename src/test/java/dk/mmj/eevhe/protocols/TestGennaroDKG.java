package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.keygeneration.ExtendedPersistedKeyParameters;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.connectors.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestGennaroDKG {
    private ExtendedPersistedKeyParameters params;

    @Before
    public void setUp() {
        params = new ExtendedPersistedKeyParameters(
                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1 29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245 E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F 83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D 670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9 DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510 15728E5A 8AACAA68 FFFFFFFF FFFFFFFF",
                "2"
        );
    }

    /**
     * Tests GennaroDKG with three non-corrupt participants
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

        //Creating players
        final GennaroDKG player1 = new GennaroDKG(testBroadcaster, channel1, commMap1, 1, params, "ID=" + 1);
        final GennaroDKG player2 = new GennaroDKG(testBroadcaster, channel2, commMap2, 2, params, "ID=" + 2);
        final GennaroDKG player3 = new GennaroDKG(testBroadcaster, channel3, commMap3, 3, params, "ID=" + 3);

        final List<GennaroDKG> players = Arrays.asList(player1, player2, player3);

        players.forEach(GennaroDKG::generationPhase);

        //No change in state, as no complaints has been registered
        //No change in state, as there were no complaints to be resolved
        assertEquals("All players should have broadcasted their commitments", 3, testBroadcaster.commitments.size());
        assertEquals("No players should have lodged a complaint", 0, testBroadcaster.complaints.size());

        // Fetching partial secret keys from extraction phase
        final PartialKeyPair partialSecret1 = player1.extractionPhase();
        final PartialKeyPair partialSecret2 = player2.extractionPhase();
        final PartialKeyPair partialSecret3 = player3.extractionPhase();

        // Assuring that all keys aren't null
        assertNotNull("Partial secret 1 was null", partialSecret1);
        assertNotNull("Partial secret 2 was null", partialSecret2);
        assertNotNull("Partial secret 3 was null", partialSecret3);

        // Fetching partial public keys
        final BigInteger partialPublicKey1 = partialSecret1.getPartialPublicKey();
        final BigInteger partialPublicKey2 = partialSecret2.getPartialPublicKey();
        final BigInteger partialPublicKey3 = partialSecret3.getPartialPublicKey();

        // Fetching partial secret keys
        final BigInteger partialSecretKey1 = player1.getPartialSecret();
        final BigInteger partialSecretKey2 = player2.getPartialSecret();
        final BigInteger partialSecretKey3 = player3.getPartialSecret();

        // Compute public key y and secret key x
        final BigInteger p = partialSecret1.getPublicKey().getP();
        final BigInteger q = partialSecret1.getPublicKey().getQ();
        final BigInteger g = partialSecret1.getPublicKey().getG();
        final BigInteger publicKey = partialPublicKey1.multiply(partialPublicKey2)
                .multiply(partialPublicKey3).mod(p);
        final BigInteger secretKey = partialSecretKey1.add(partialSecretKey2).add(partialSecretKey3).mod(q);
        final BigInteger testPublicKey = g.modPow(secretKey, p);

        // Assert that y = g^x mod p
        assertEquals("Public key Y and g^x did not match", testPublicKey, publicKey);
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
