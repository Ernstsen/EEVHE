package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.exceptions.UnableToDecryptException;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedPersistedKeyParameters;
import dk.mmj.eevhe.entities.CipherText;
import dk.mmj.eevhe.entities.PartialKeyPair;
import dk.mmj.eevhe.entities.PartialSecretKey;
import dk.mmj.eevhe.entities.PublicKey;
import dk.mmj.eevhe.protocols.connectors.PrivateCommunicationChannel;
import dk.mmj.eevhe.protocols.connectors.TestBroadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.DKG;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestGennaroDKG {
    private ExtendedKeyGenerationParameters params;

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

        HashMap<Integer, List<DKG.Step>> generationSteps = new HashMap<>();
        generationSteps.put(1, player1.generationPhase());
        generationSteps.put(2, player2.generationPhase());
        generationSteps.put(3, player3.generationPhase());

        for (int i = 0; i < generationSteps.get(1).size(); i++) {
            generationSteps.get(1).get(i).getExecutable().run();
            generationSteps.get(2).get(i).getExecutable().run();
            generationSteps.get(3).get(i).getExecutable().run();
        }

        //No change in state, as no complaints has been registered
        //No change in state, as there were no complaints to be resolved
        assertEquals("All players should have broadcast their commitments", 3, testBroadcaster.getCommitments().size());
        assertEquals("No players should have lodged a complaint", 0, testBroadcaster.getPedersenComplaints().size());

        // Fetching partial secret keys from extraction phase

        HashMap<Integer, List<DKG.Step>> extractionSteps = new HashMap<>();
        extractionSteps.put(1, player1.extractionPhase());
        extractionSteps.put(2, player2.extractionPhase());
        extractionSteps.put(3, player3.extractionPhase());

        for (int i = 0; i < extractionSteps.get(1).size(); i++) {
            extractionSteps.get(1).get(i).getExecutable().run();
            extractionSteps.get(2).get(i).getExecutable().run();
            extractionSteps.get(3).get(i).getExecutable().run();
        }

        final PartialKeyPair output1 = player1.output();
        final PartialKeyPair output2 = player2.output();
        final PartialKeyPair output3 = player3.output();

        // Assuring that all keys aren't null
        assertNotNull("Partial secret 1 was null", output1);
        assertNotNull("Partial secret 2 was null", output2);
        assertNotNull("Partial secret 3 was null", output3);

        // Fetching partial public keys
        final BigInteger partialPublicKey1 = output1.getPartialPublicKey();
        final BigInteger partialPublicKey2 = output2.getPartialPublicKey();
        final BigInteger partialPublicKey3 = output3.getPartialPublicKey();

        // Fetching partial secret keys
        final PartialSecretKey partialSecretKey1 = output1.getPartialSecretKey();
        final PartialSecretKey partialSecretKey2 = output2.getPartialSecretKey();
        final PartialSecretKey partialSecretKey3 = output3.getPartialSecretKey();

        // Compute public key y and secret key x
        final BigInteger p = output1.getPublicKey().getP();
        final BigInteger q = output1.getPublicKey().getQ();
        final BigInteger g = output1.getPublicKey().getG();

        HashMap<Integer, BigInteger> partialSecretKeys = new HashMap<>();
        partialSecretKeys.put(1, partialSecretKey1.getSecretValue());
        partialSecretKeys.put(2, partialSecretKey2.getSecretValue());
        partialSecretKeys.put(3, partialSecretKey3.getSecretValue());

        HashMap<Integer, BigInteger> partialPublicKeys = new HashMap<>();
        partialPublicKeys.put(1, partialPublicKey1);
        partialPublicKeys.put(2, partialPublicKey2);
        partialPublicKeys.put(3, partialPublicKey3);

        HashMap<Integer, BigInteger> partialSecrets = new HashMap<>();
        partialSecrets.put(1, partialSecretKey1.getSecretValue());
        partialSecrets.put(2, partialSecretKey2.getSecretValue());
        partialSecrets.put(3, partialSecretKey3.getSecretValue());

        List<PublicKey> publicKeys = new ArrayList<>();
        publicKeys.add(output1.getPublicKey());
        publicKeys.add(output2.getPublicKey());
        publicKeys.add(output3.getPublicKey());

        assertInvariants(publicKeys, partialPublicKeys, partialSecrets, p, q, g);
        assertEncryptDecrypt(output1, partialSecretKeys, p, g);
    }

    private void assertInvariants(List<PublicKey> publicKeys,
                                  HashMap<Integer, BigInteger> partialPublicKeys,
                                  HashMap<Integer, BigInteger> partialSecrets,
                                  BigInteger p, BigInteger q, BigInteger g) {
        // Compute public key
        final BigInteger publicKey = partialPublicKeys.values().stream().reduce(BigInteger.ONE, BigInteger::multiply).mod(p);
        final BigInteger x = partialSecrets.values().stream().reduce(BigInteger::add).orElse(BigInteger.ZERO).mod(q);

        final BigInteger testPublicKey = g.modPow(x, p);

        // Assert that partials match
        for (Map.Entry<Integer, BigInteger> entry : partialPublicKeys.entrySet()) {
            Integer key = entry.getKey();
            BigInteger value = entry.getValue();

            assertEquals("partials for player " + key + " did not match",
                    value, g.modPow(partialSecrets.get(key), p));

        }

        // Assert that y = g^x mod p
        assertEquals("Public key Y and g^x did not match", testPublicKey, publicKey);
        // Assert that public keys all match
        assertTrue("PublicKey did not match for all instances", publicKeys.stream()
                .allMatch(publicKeys.get(0)::equals));
    }

    private void assertEncryptDecrypt(PartialKeyPair output, HashMap<Integer, BigInteger> partialSecretKeys,
                                      BigInteger p, BigInteger g) {
        //Asserts that encryption and subsequent decryption is possible
        BigInteger msg = new BigInteger("15");
        CipherText cipherText = ElGamal.homomorphicEncryption(output.getPublicKey(), msg);

        Map<Integer, BigInteger> partials;

        partials = partialSecretKeys.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> ElGamal.partialDecryption(cipherText.getC(), partialSecretKeys.get(e.getKey()), p)
                ));

        BigInteger cs = SecurityUtils.lagrangeInterpolate(partials, p);
        try {
            int result = ElGamal.homomorphicDecryptionFromPartials(cipherText.getD(), cs, g, p, 15);
            assertEquals("Results was incorrect", 15, result);
        } catch (UnableToDecryptException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSteps() {
        final TestBroadcaster testBroadcaster = new TestBroadcaster();
        final PrivateCommunicationChannel channel = new PrivateCommunicationChannel();
        final HashMap<Integer, PeerCommunicator> commMap = new HashMap<>();

        GennaroDKG gennaroDKG = new GennaroDKG(testBroadcaster, channel, commMap, 0, params, "");

        ArrayList<DKG.Step> steps = new ArrayList<>();
        steps.addAll(gennaroDKG.generationPhase());
        steps.addAll(gennaroDKG.extractionPhase());
        assertEquals("steps should contain exactly the two phases!", steps.size(), gennaroDKG.getSteps().size());
    }

    /**
     * Tests GennaroDKG with two non-corrupt participants, and one corrupt
     */
    @Test
    public void testProtocolRunOneCorrupt() {
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
        final GennaroDKG player1 = new DishonestGennaroDKG(testBroadcaster,
                channel1, commMap1, 1, params, "ID=" + 1, true, false, false);
        final GennaroDKG player2 = new GennaroDKG(testBroadcaster, channel2, commMap2, 2, params, "ID=" + 2);
        final GennaroDKG player3 = new GennaroDKG(testBroadcaster, channel3, commMap3, 3, params, "ID=" + 3);

        HashMap<Integer, List<DKG.Step>> generationSteps = new HashMap<>();
        generationSteps.put(1, player1.generationPhase());
        generationSteps.put(2, player2.generationPhase());
        generationSteps.put(3, player3.generationPhase());

        for (int i = 0; i < generationSteps.get(1).size(); i++) {
            generationSteps.get(1).get(i).getExecutable().run();
            generationSteps.get(2).get(i).getExecutable().run();
            generationSteps.get(3).get(i).getExecutable().run();
        }

        //No change in state, as no complaints has been registered
        //No change in state, as there were no complaints to be resolved
        assertEquals("All players should have broadcast their commitments", 3, testBroadcaster.getCommitments().size());
        assertEquals("Players should not have lodged a complaint", 0, testBroadcaster.getPedersenComplaints().size());

        // Fetching partial secret keys from extraction phase

        HashMap<Integer, List<DKG.Step>> extractionSteps = new HashMap<>();
        extractionSteps.put(1, player1.extractionPhase());
        extractionSteps.put(2, player2.extractionPhase());
        extractionSteps.put(3, player3.extractionPhase());

        for (int i = 0; i < extractionSteps.get(1).size(); i++) {
            extractionSteps.get(1).get(i).getExecutable().run();
            extractionSteps.get(2).get(i).getExecutable().run();
            extractionSteps.get(3).get(i).getExecutable().run();
        }

        assertEquals("Two players should have lodged complaints in extraction phase",
                2, testBroadcaster.getFeldmanComplaints().size());
        assertNotEquals("Size of honest parties did not change",
                player2.getHonestPartiesPedersen().size(), player2.getHonestPartiesFeldman().size());

        assertTrue("Dishonest party 1 was not removed from honestParty set",
                (player2.getHonestPartiesPedersen().contains(1) &&
                        !player2.getHonestPartiesFeldman().contains(1)));

        final PartialKeyPair output2 = player2.output();
        final PartialKeyPair output3 = player3.output();

        // Assuring that all keys aren't null
        assertNotNull("Partial secret 2 was null", output2);
        assertNotNull("Partial secret 3 was null", output3);

        // Fetching partial public keys
        final BigInteger partialPublicKey2 = output2.getPartialPublicKey();
        final BigInteger partialPublicKey3 = output3.getPartialPublicKey();

        // Fetching partial secret keys
        final PartialSecretKey partialSecretKey2 = output2.getPartialSecretKey();
        final PartialSecretKey partialSecretKey3 = output3.getPartialSecretKey();

        // Compute public key y and secret key x
        final BigInteger p = output2.getPublicKey().getP();
        final BigInteger q = output2.getPublicKey().getQ();
        final BigInteger g = output2.getPublicKey().getG();

        HashMap<Integer, BigInteger> partialSecretKeys = new HashMap<>();
        partialSecretKeys.put(2, partialSecretKey2.getSecretValue());
        partialSecretKeys.put(3, partialSecretKey3.getSecretValue());

        HashMap<Integer, BigInteger> partialPublicKeys = new HashMap<>();
        partialPublicKeys.put(2, partialPublicKey2);
        partialPublicKeys.put(3, partialPublicKey3);

        HashMap<Integer, BigInteger> partialSecrets = new HashMap<>();
        partialSecrets.put(2, partialSecretKey2.getSecretValue());
        partialSecrets.put(3, partialSecretKey3.getSecretValue());

        List<PublicKey> publicKeys = new ArrayList<>();
        publicKeys.add(output2.getPublicKey());
        publicKeys.add(output3.getPublicKey());

        assertInvariants(publicKeys, partialPublicKeys, partialSecrets, p, q, g);
        assertEncryptDecrypt(output2, partialSecretKeys, p, g);
    }

    /**
     * Tests GennaroDKG with two non-corrupt participants, and one corrupt
     */
    @Test
    public void testProtocolOneCommitmentIsNull() {
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
        final GennaroDKG player1 = new DishonestGennaroDKG(testBroadcaster,
                channel1, commMap1, 1, params, "ID=" + 1,
                false, true, false);
        final GennaroDKG player2 = new GennaroDKG(testBroadcaster, channel2, commMap2, 2, params, "ID=" + 2);
        final GennaroDKG player3 = new GennaroDKG(testBroadcaster, channel3, commMap3, 3, params, "ID=" + 3);

        HashMap<Integer, List<DKG.Step>> generationSteps = new HashMap<>();
        generationSteps.put(1, player1.generationPhase());
        generationSteps.put(2, player2.generationPhase());
        generationSteps.put(3, player3.generationPhase());

        for (int i = 0; i < generationSteps.get(1).size(); i++) {
            generationSteps.get(1).get(i).getExecutable().run();
            generationSteps.get(2).get(i).getExecutable().run();
            generationSteps.get(3).get(i).getExecutable().run();
        }

        //No change in state, as no complaints has been registered
        //No change in state, as there were no complaints to be resolved
        assertEquals("All players should have broadcast their commitments", 3, testBroadcaster.getCommitments().size());
        assertEquals("Players should not have lodged a complaint", 0, testBroadcaster.getPedersenComplaints().size());

        // Fetching partial secret keys from extraction phase

        HashMap<Integer, List<DKG.Step>> extractionSteps = new HashMap<>();
        extractionSteps.put(1, player1.extractionPhase());
        extractionSteps.put(2, player2.extractionPhase());
        extractionSteps.put(3, player3.extractionPhase());

        for (int i = 0; i < extractionSteps.get(1).size(); i++) {
            extractionSteps.get(1).get(i).getExecutable().run();
            extractionSteps.get(2).get(i).getExecutable().run();
            extractionSteps.get(3).get(i).getExecutable().run();
        }

        assertNotEquals("Size of honest parties did not change",
                player2.getHonestPartiesPedersen().size(), player2.getHonestPartiesFeldman().size());

        assertTrue("Dishonest party 1 was not removed from honestParty set",
                (player2.getHonestPartiesPedersen().contains(1) &&
                        !player2.getHonestPartiesFeldman().contains(1)));

        final PartialKeyPair output2 = player2.output();
        final PartialKeyPair output3 = player3.output();

        // Assuring that all keys aren't null
        assertNotNull("Partial secret 2 was null", output2);
        assertNotNull("Partial secret 3 was null", output3);

        // Fetching partial public keys
        final BigInteger partialPublicKey2 = output2.getPartialPublicKey();
        final BigInteger partialPublicKey3 = output3.getPartialPublicKey();

        // Fetching partial secret keys
        final PartialSecretKey partialSecretKey2 = output2.getPartialSecretKey();
        final PartialSecretKey partialSecretKey3 = output3.getPartialSecretKey();

        // Compute public key y and secret key x
        final BigInteger p = output2.getPublicKey().getP();
        final BigInteger q = output2.getPublicKey().getQ();
        final BigInteger g = output2.getPublicKey().getG();

        HashMap<Integer, BigInteger> partialSecretKeys = new HashMap<>();
        partialSecretKeys.put(2, partialSecretKey2.getSecretValue());
        partialSecretKeys.put(3, partialSecretKey3.getSecretValue());

        HashMap<Integer, BigInteger> partialPublicKeys = new HashMap<>();
        partialPublicKeys.put(2, partialPublicKey2);
        partialPublicKeys.put(3, partialPublicKey3);

        HashMap<Integer, BigInteger> partialSecrets = new HashMap<>();
        partialSecrets.put(2, partialSecretKey2.getSecretValue());
        partialSecrets.put(3, partialSecretKey3.getSecretValue());

        List<PublicKey> publicKeys = new ArrayList<>();
        publicKeys.add(output2.getPublicKey());
        publicKeys.add(output3.getPublicKey());

        assertInvariants(publicKeys, partialPublicKeys, partialSecrets, p, q, g);
        assertEncryptDecrypt(output2, partialSecretKeys, p, g);
    }
}
