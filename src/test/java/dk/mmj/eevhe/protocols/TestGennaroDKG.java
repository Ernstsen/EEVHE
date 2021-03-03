package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.exceptions.UnableToDecryptException;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedPersistedKeyParameters;
import dk.mmj.eevhe.entities.*;
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
        assertEquals("No players should have lodged a complaint", 0, testBroadcaster.getComplaints().size());

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

        assertInvariants(output1, output2, output3, partialPublicKey1, partialPublicKey2, partialPublicKey3, p, q, g);
        assertEncryptDecrypt(output1, partialSecretKey1.getSecretValue(), partialSecretKey2.getSecretValue(), partialSecretKey3.getSecretValue(), p, g);
    }

    private void assertInvariants(PartialKeyPair output1, PartialKeyPair output2, PartialKeyPair output3,
                                  BigInteger partialPublicKey1, BigInteger partialPublicKey2, BigInteger partialPublicKey3,
                                  BigInteger p, BigInteger q, BigInteger g) {
        //Invariant Assertions
        final BigInteger publicKey = partialPublicKey1.multiply(partialPublicKey2)
                .multiply(partialPublicKey3).mod(p);

        BigInteger partialSecret1 = output1.getPartialSecretKey().getdLogPublicValue();
        BigInteger partialSecret2 = output2.getPartialSecretKey().getdLogPublicValue();
        BigInteger partialSecret3 = output3.getPartialSecretKey().getdLogPublicValue();
        final BigInteger x = partialSecret1.add(partialSecret2).add(partialSecret3).mod(q);
        final BigInteger testPublicKey = g.modPow(x, p);

        assertEquals("partials for player 1 did not match", partialPublicKey1, g.modPow(partialSecret1, p));
        assertEquals("partials for player 2 did not match", partialPublicKey2, g.modPow(partialSecret2, p));
        assertEquals("partials for player 3 did not match", partialPublicKey3, g.modPow(partialSecret3, p));

        // Assert that y = g^x mod p
        assertEquals("Public key Y and g^x did not match", testPublicKey, publicKey);


        assertEquals("PublicKey for instances 1 and 2 did not match", output1.getPublicKey(), output2.getPublicKey());
        assertEquals("PublicKey for instances 2 and 3 did not match", output2.getPublicKey(), output3.getPublicKey());
    }

    private void assertEncryptDecrypt(PartialKeyPair output1, BigInteger partialSecretKey1,
                                      BigInteger partialSecretKey2, BigInteger partialSecretKey3,
                                      BigInteger p, BigInteger g) {
        //Asserts that encryption and subsequent decryption is possible
        BigInteger msg = new BigInteger("15");
        CipherText cipherText = ElGamal.homomorphicEncryption(output1.getPublicKey(), msg);


        BigInteger dec1 = ElGamal.partialDecryption(cipherText.getC(), partialSecretKey1, p);
        BigInteger dec2 = ElGamal.partialDecryption(cipherText.getC(), partialSecretKey2, p);
        BigInteger dec3 = ElGamal.partialDecryption(cipherText.getC(), partialSecretKey3, p);

        HashMap<Integer, BigInteger> partials = new HashMap<>();
        partials.put(1, dec1);
        partials.put(2, dec2);
        partials.put(3, dec3);
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
}
