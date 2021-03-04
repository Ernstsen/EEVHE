package dk.mmj.eevhe.server.decryptionauthority;

import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedPersistedKeyParameters;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.GennaroDKG;
import dk.mmj.eevhe.protocols.connectors.PrivateCommunicationChannel;
import dk.mmj.eevhe.protocols.connectors.TestBroadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.DKG;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestDecrypterIml {

    private HashMap<Integer, PartialKeyPair> dkgRes;

    private static HashMap<Integer, PartialKeyPair> runDKG() {
        ExtendedKeyGenerationParameters params = new ExtendedPersistedKeyParameters(
                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1 29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245 E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F 83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D 670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9 DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510 15728E5A 8AACAA68 FFFFFFFF FFFFFFFF",
                "2"
        );

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

        HashMap<Integer, List<DKG.Step>> steps = new HashMap<>();
        steps.put(1, player1.getSteps());
        steps.put(2, player2.getSteps());
        steps.put(3, player3.getSteps());

        for (int i = 0; i < steps.get(1).size(); i++) {
            steps.get(1).get(i).getExecutable().run();
            steps.get(2).get(i).getExecutable().run();
            steps.get(3).get(i).getExecutable().run();
        }
        HashMap<Integer, PartialKeyPair> dkgRes = new HashMap<>();
        dkgRes.put(1, player1.output());
        dkgRes.put(2, player2.output());
        dkgRes.put(3, player3.output());
        return dkgRes;
    }

    @Before
    public void setup() {
        dkgRes = runDKG();
    }

    @Test
    public void generatePartialResult() {
        PublicKey pk = dkgRes.get(1).getPublicKey();
        List<PersistedBallot> ballots = Arrays.asList(
                new PersistedBallot(SecurityUtils.generateBallot(2, 3, "1", pk)),
                new PersistedBallot(SecurityUtils.generateBallot(1, 3, "2", pk)),
                new PersistedBallot(SecurityUtils.generateBallot(0, 3, "3", pk)),
                new PersistedBallot(SecurityUtils.generateBallot(2, 3, "4", pk))
        );

        List<Candidate> candidates = Arrays.asList(
                new Candidate(0, "Mette", "A"),
                new Candidate(1, "Jakob", "V"),
                new Candidate(2, "Morten", "B")
        );

        DecrypterIml decrypterIml1 = new DecrypterIml(1, () -> ballots, b -> VoteProofUtils.verifyBallot(b, pk), candidates);
        DecrypterIml decrypterIml2 = new DecrypterIml(2, () -> ballots, b -> VoteProofUtils.verifyBallot(b, pk), candidates);
        DecrypterIml decrypterIml3 = new DecrypterIml(3, () -> ballots, b -> VoteProofUtils.verifyBallot(b, pk), candidates);

        PartialResultList partialResultList1 = decrypterIml1.generatePartialResult(new Date().getTime() - 10, dkgRes.get(1));
        PartialResultList partialResultList2 = decrypterIml2.generatePartialResult(new Date().getTime() - 10, dkgRes.get(2));
        PartialResultList partialResultList3 = decrypterIml3.generatePartialResult(new Date().getTime() - 10, dkgRes.get(3));

        partialResultList1.getResults().forEach(this::assertResultPasses);
        partialResultList2.getResults().forEach(this::assertResultPasses);
        partialResultList3.getResults().forEach(this::assertResultPasses);


    }

    private void assertResultPasses(PartialResult res) {
        PartialKeyPair partialKeyPair = dkgRes.get(res.getId());
        PublicKey pk = partialKeyPair.getPublicKey();

        CipherText sum = res.getCipherText();
        PublicKey partialPublicKey = new PublicKey(partialKeyPair.getPartialPublicKey(), pk.getG(), pk.getQ());

        assertTrue("Failed to validate proof of correct decryption for partial decryption!", DLogProofUtils.verifyProof(
                sum,
                new CipherText(res.getResult(), sum.getD()),
                partialPublicKey, res.getProof(), res.getId()));
    }

    @Test
    public void returnsNullWhenNoBallots() {
        DecrypterIml decrypterIml = new DecrypterIml(1, ArrayList::new, (b) -> false, new ArrayList<>());

        PartialResultList res = decrypterIml.generatePartialResult(-1, new PartialKeyPair(null, null, null));

        assertNull("Should be null", res);
    }
}
