package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.PedersenVSSUtils;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.exceptions.UnableToDecryptException;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedPersistedKeyParameters;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGBroadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGPeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.VSS;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.crypto.SecurityUtils.generatePolynomial;
import static dk.mmj.eevhe.protocols.PedersenVSS.PEDERSEN;
import static org.junit.Assert.*;

public class TestGennaroFeldmanVSS {
    private ExtendedPersistedKeyParameters params;
    private BigInteger q;
    private BigInteger p;
    private BigInteger g;
    private BigInteger e;
    private int t;

    @Before
    public void setUp() throws Exception {
        params = new ExtendedPersistedKeyParameters(
                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1 29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245 E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F 83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D 670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9 DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510 15728E5A 8AACAA68 FFFFFFFF FFFFFFFF",
                "2"
        );
        q = params.getPrimePair().getQ();
        p = params.getPrimePair().getP();
        g = params.getGenerator();
        e = params.getGroupElement();
        t = ((3) / 2);
    }

    /**
     * Tests FeldmanVSS with three non-corrupt participants
     */
    @Test
    public void testProtocolRunNoCorruption() {
        //Modelling communications channels
        final TestPedersenVSS.TestDKGBroadcaster testBroadcaster = new TestPedersenVSS.TestDKGBroadcaster();
        final TestPedersenVSS.PrivateCommunicationChannelDKG channel1 = new TestPedersenVSS.PrivateCommunicationChannelDKG();
        final TestPedersenVSS.PrivateCommunicationChannelDKG channel2 = new TestPedersenVSS.PrivateCommunicationChannelDKG();
        final TestPedersenVSS.PrivateCommunicationChannelDKG channel3 = new TestPedersenVSS.PrivateCommunicationChannelDKG();

        final HashMap<Integer, DKGPeerCommunicator> commMap1 = new HashMap<>();
        final HashMap<Integer, DKGPeerCommunicator> commMap2 = new HashMap<>();
        final HashMap<Integer, DKGPeerCommunicator> commMap3 = new HashMap<>();

        commMap1.put(2, channel2);
        commMap1.put(3, channel3);

        commMap2.put(1, channel1);
        commMap2.put(3, channel3);

        commMap3.put(1, channel1);
        commMap3.put(2, channel2);

        BigInteger[] pol1 = generatePolynomial(t, q);
        BigInteger[] pol2 = generatePolynomial(t, q);
        BigInteger[] pol3 = generatePolynomial(t, q);

        Map<Integer, PartialSecretMessageDTO> secrets1 = computeSecretsMap(pol1, pol2, pol3, 1);
        Map<Integer, PartialSecretMessageDTO> secrets2 = computeSecretsMap(pol1, pol2, pol3, 2);
        Map<Integer, PartialSecretMessageDTO> secrets3 = computeSecretsMap(pol1, pol2, pol3, 3);

        testBroadcaster.commit(new CommitmentDTO(PedersenVSSUtils.computeCoefficientCommitments(g, e, p, pol1, pol1), 1, PEDERSEN));
        testBroadcaster.commit(new CommitmentDTO(PedersenVSSUtils.computeCoefficientCommitments(g, e, p, pol2, pol2), 2, PEDERSEN));
        testBroadcaster.commit(new CommitmentDTO(PedersenVSSUtils.computeCoefficientCommitments(g, e, p, pol3, pol3), 3, PEDERSEN));

        //Creating players
        final GennaroFeldmanVSS player1 = new GennaroFeldmanVSS(testBroadcaster, channel1, commMap1,
                1, params, "ID=" + 1, pol1, secrets1);
        final GennaroFeldmanVSS player2 = new GennaroFeldmanVSS(testBroadcaster, channel2, commMap2,
                2, params, "ID=" + 2, pol2, secrets2);
        final GennaroFeldmanVSS player3 = new GennaroFeldmanVSS(testBroadcaster, channel3, commMap3,
                3, params, "ID=" + 3, pol3, secrets3);

        final List<GennaroFeldmanVSS> players = Arrays.asList(player1, player2, player3);

        players.forEach(VSS::startProtocol);

        assertEquals("All players should have broadcast their commitments",
                6, testBroadcaster.commitments.size());

        players.forEach(VSS::handleReceivedValues);

        assertEquals("No players should have lodged a complaint",
                0, testBroadcaster.feldmanComplaints.size());

        players.forEach(VSS::handleComplaints);

        //No change in state, as no complaints has been registered

        players.forEach(VSS::applyResolves);

        assertEquals("Honest parties should be of size 3", 3, player1.getHonestParties().size());
        assertEquals("Honest parties should be of size 3", 3, player2.getHonestParties().size());
        assertEquals("Honest parties should be of size 3", 3, player3.getHonestParties().size());

        //No change in state, as there were no complaints to be resolved

        final BigInteger partialSecret1 = player1.output();
        final BigInteger partialSecret2 = player2.output();
        final BigInteger partialSecret3 = player3.output();

        assertNotNull("Partial secret was null", partialSecret1);
        assertNotNull("Partial secret was null", partialSecret2);
        assertNotNull("Partial secret was null", partialSecret3);

        Set<Integer> honestParties = new HashSet<>();
        honestParties.add(1);
        honestParties.add(2);
        honestParties.add(3);

        PartialKeyPair keyPair1 = computeKeyPair(testBroadcaster, honestParties, player1);
        PartialKeyPair keyPair2 = computeKeyPair(testBroadcaster, honestParties, player2);
        PartialKeyPair keyPair3 = computeKeyPair(testBroadcaster, honestParties, player3);

        HashMap<Integer, BigInteger> partialSecretKeys = new HashMap<>();
        partialSecretKeys.put(1, keyPair1.getPartialSecretKey().getSecretValue());
        partialSecretKeys.put(2, keyPair2.getPartialSecretKey().getSecretValue());
        partialSecretKeys.put(3, keyPair3.getPartialSecretKey().getSecretValue());

        assertEncryptDecrypt(keyPair1, partialSecretKeys, p, g);
    }

    @Test
    public void testProtocolWithOneNonParticipant() {
        //Modelling communications channels
        final TestPedersenVSS.TestDKGBroadcaster testBroadcaster = new TestPedersenVSS.TestDKGBroadcaster();
        final TestPedersenVSS.PrivateCommunicationChannelDKG channel1 = new TestPedersenVSS.PrivateCommunicationChannelDKG();
        final TestPedersenVSS.PrivateCommunicationChannelDKG channel2 = new TestPedersenVSS.PrivateCommunicationChannelDKG();
        final TestPedersenVSS.PrivateCommunicationChannelDKG channel3 = new TestPedersenVSS.PrivateCommunicationChannelDKG();

        final HashMap<Integer, DKGPeerCommunicator> commMap1 = new HashMap<>();
        final HashMap<Integer, DKGPeerCommunicator> commMap3 = new HashMap<>();

        commMap1.put(2, channel2);
        commMap1.put(3, channel3);

        commMap3.put(1, channel1);
        commMap3.put(2, channel2);

        //Creating players
        BigInteger[] pol1 = generatePolynomial(t, q);
        BigInteger[] pol3 = generatePolynomial(t, q);

        Map<Integer, PartialSecretMessageDTO> secrets1 = computeSecretsMap(pol1, null, pol3, 1);
        Map<Integer, PartialSecretMessageDTO> secrets3 = computeSecretsMap(pol1, null, pol3, 3);

        testBroadcaster.commit(new CommitmentDTO(PedersenVSSUtils.computeCoefficientCommitments(g, e, p, pol1, pol1), 1, PEDERSEN));
        testBroadcaster.commit(new CommitmentDTO(PedersenVSSUtils.computeCoefficientCommitments(g, e, p, pol3, pol3), 3, PEDERSEN));

        //Creating players
        final GennaroFeldmanVSS player1 = new GennaroFeldmanVSS(testBroadcaster, channel1, commMap1,
                1, params, "ID=" + 1, pol1, secrets1);
        final GennaroFeldmanVSS player3 = new GennaroFeldmanVSS(testBroadcaster, channel3, commMap3,
                3, params, "ID=" + 3, pol3, secrets3);

        final List<GennaroFeldmanVSS> players = Arrays.asList(player1, player3);

        players.forEach(VSS::startProtocol);

        assertEquals("All players should have broadcasted their commitments", 4, testBroadcaster.commitments.size());

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

        Set<Integer> honestParties = new HashSet<>();
        honestParties.add(1);
        honestParties.add(3);

        PartialKeyPair keyPair1 = computeKeyPair(testBroadcaster, honestParties, player1);
        PartialKeyPair keyPair3 = computeKeyPair(testBroadcaster, honestParties, player3);

        HashMap<Integer, BigInteger> partialSecretKeys = new HashMap<>();
        partialSecretKeys.put(1, keyPair1.getPartialSecretKey().getSecretValue());
        partialSecretKeys.put(3, keyPair3.getPartialSecretKey().getSecretValue());

        assertEncryptDecrypt(keyPair1, partialSecretKeys, p, g);
    }

    /**
     * Runs FeldmanVSS with no private channels. All secret values should be made public in this case
     */
    @Test
    public void testProtocolWithComplaintsAboutOneParty() {
        //Modelling communications channels
        final TestPedersenVSS.TestDKGBroadcaster testBroadcaster = new TestPedersenVSS.TestDKGBroadcaster();
        final TestPedersenVSS.PrivateCommunicationChannelDKG channel1 = new TestPedersenVSS.PrivateCommunicationChannelDKG();
        final TestPedersenVSS.PrivateCommunicationChannelDKG channel2 = new TestPedersenVSS.PrivateCommunicationChannelDKG();
        final TestPedersenVSS.PrivateCommunicationChannelDKG channel3 = new TestPedersenVSS.PrivateCommunicationChannelDKG();

        final HashMap<Integer, DKGPeerCommunicator> commMap1 = new HashMap<>();
        final HashMap<Integer, DKGPeerCommunicator> commMap2 = new HashMap<>();
        final HashMap<Integer, DKGPeerCommunicator> commMap3 = new HashMap<>();

        commMap1.put(2, channel2);
        commMap1.put(3, channel3);

        commMap2.put(1, channel1);
        commMap2.put(3, channel3);

        commMap3.put(1, channel1);
        commMap3.put(2, channel2);

        BigInteger[] pol1 = generatePolynomial(t, q);
        BigInteger[] pol2 = generatePolynomial(t, q);
        BigInteger[] pol3 = generatePolynomial(t, q);

        Map<Integer, PartialSecretMessageDTO> secrets1 = computeSecretsMap(pol1, pol2, pol3, 1);
        Map<Integer, PartialSecretMessageDTO> secrets2 = computeSecretsMap(pol1, pol2, pol3, 2);
        Map<Integer, PartialSecretMessageDTO> secrets3 = computeSecretsMap(pol1, pol2, pol3, 3);

        testBroadcaster.commit(new CommitmentDTO(PedersenVSSUtils.computeCoefficientCommitments(g, e, p, pol1, pol1), 1, PEDERSEN));
        testBroadcaster.commit(new CommitmentDTO(PedersenVSSUtils.computeCoefficientCommitments(g, e, p, pol2, pol2), 2, PEDERSEN));
        testBroadcaster.commit(new CommitmentDTO(PedersenVSSUtils.computeCoefficientCommitments(g, e, p, pol3, pol3), 3, PEDERSEN));

        //Creating players
        final DishonestGennaroFeldmanVSS player1 = new DishonestGennaroFeldmanVSS(testBroadcaster, channel1, commMap1,
                1, params, "ID=" + 1, pol1, secrets1,
                true, false, false);
        final GennaroFeldmanVSS player2 = new GennaroFeldmanVSS(testBroadcaster, channel2, commMap2,
                2, params, "ID=" + 2, pol2, secrets2);
        final GennaroFeldmanVSS player3 = new GennaroFeldmanVSS(testBroadcaster, channel3, commMap3,
                3, params, "ID=" + 3, pol3, secrets3);

        final List<GennaroFeldmanVSS> players = Arrays.asList(player1, player2, player3);

        players.forEach(VSS::startProtocol);

        assertEquals("All players should have broadcast their commitments",
                6, testBroadcaster.commitments.size());

        players.forEach(VSS::handleReceivedValues);

        assertEquals("Players should have lodged a complaint about one party",
                2, testBroadcaster.feldmanComplaints.size());

        players.forEach(VSS::handleComplaints);

        players.forEach(VSS::applyResolves);

        assertEquals("Size of honest parties should be 2", 2, player2.getHonestParties().size());
        assertEquals("Size of honest parties should be 2", 2, player3.getHonestParties().size());
        assertEquals("Player 2 and Player 3 should have the same set of honest parties",
                player2.getHonestParties(), player3.getHonestParties());

        final BigInteger partialSecret1 = player1.output();
        final BigInteger partialSecret2 = player2.output();
        final BigInteger partialSecret3 = player3.output();

        assertNotNull("Partial secret was null", partialSecret1);
        assertNotNull("Partial secret was null", partialSecret2);
        assertNotNull("Partial secret was null", partialSecret3);

        Set<Integer> honestParties = player2.getHonestParties();

        PartialKeyPair keyPair2 = computeKeyPair(testBroadcaster, honestParties, player2);
        PartialKeyPair keyPair3 = computeKeyPair(testBroadcaster, honestParties, player3);

        HashMap<Integer, BigInteger> partialSecretKeys = new HashMap<>();
        partialSecretKeys.put(2, keyPair2.getPartialSecretKey().getSecretValue());
        partialSecretKeys.put(3, keyPair3.getPartialSecretKey().getSecretValue());

        assertEncryptDecrypt(keyPair2, partialSecretKeys, p, g);
    }

    private Map<Integer, PartialSecretMessageDTO> computeSecretsMap(BigInteger[] pol1, BigInteger[] pol2, BigInteger[] pol3, int target) {
        Map<Integer, PartialSecretMessageDTO> secrets = new HashMap<>();

        if (pol1 != null) {
            secrets.put(1, new PartialSecretMessageDTO(
                    SecurityUtils.evaluatePolynomial(pol1, target), SecurityUtils.evaluatePolynomial(pol1, target), target, 1));
        }
        if (pol2 != null) {
            secrets.put(2, new PartialSecretMessageDTO(
                    SecurityUtils.evaluatePolynomial(pol2, target), SecurityUtils.evaluatePolynomial(pol2, target), target, 2));
        }
        if (pol3 != null) {
            secrets.put(3, new PartialSecretMessageDTO(
                    SecurityUtils.evaluatePolynomial(pol3, target), SecurityUtils.evaluatePolynomial(pol3, target), target, 3));
        }

        return secrets;
    }

    private PartialKeyPair computeKeyPair(DKGBroadcaster broadcaster, Set<Integer> honestParties, GennaroFeldmanVSS feldmanVSS) {
        BigInteger partialSecretKey = feldmanVSS.output();
        BigInteger partialPublicKey = g.modPow(partialSecretKey, p);

        // Computes Y = prod_i y_i mod p
        List<CommitmentDTO> commitments = broadcaster.getCommitments().stream()
                .filter(c -> GennaroFeldmanVSS.FELDMAN.equals(c.getProtocol()))
                .collect(Collectors.toList());

        List<BigInteger> partialPublicKeys = new ArrayList<>();
        for (CommitmentDTO commitment : commitments) {
            if (honestParties.contains(commitment.getId())) {
                if (commitment.getCommitment() != null) {
                    partialPublicKeys.add(commitment.getCommitment()[0]);
                }
            }
        }

        BigInteger publicKey = partialPublicKeys.stream().reduce(BigInteger::multiply).orElse(BigInteger.ZERO).mod(p);
        return new PartialKeyPair(
                new PartialSecretKey(partialSecretKey, p),
                partialPublicKey,
                new PublicKey(publicKey, g, q)
        );
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
}
