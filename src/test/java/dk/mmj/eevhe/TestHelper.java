package dk.mmj.eevhe;

import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedPersistedKeyParameters;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.Candidate;
import dk.mmj.eevhe.entities.PartialKeyPair;
import dk.mmj.eevhe.entities.PartialResultList;
import dk.mmj.eevhe.entities.PublicKey;
import dk.mmj.eevhe.interfaces.BallotFetcher;
import dk.mmj.eevhe.protocols.GennaroDKG;
import dk.mmj.eevhe.protocols.connectors.PrivateCommunicationChannel;
import dk.mmj.eevhe.protocols.connectors.TestBroadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.DKG;
import dk.mmj.eevhe.server.decryptionauthority.DecrypterImpl;
import dk.mmj.eevhe.server.decryptionauthority.TestDecrypterImpl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestHelper {

    /**
     * Executes {@link GennaroDKG}
     * <br>
     * ONLY works i {@link dk.mmj.eevhe.protocols.TestGennaroDKG} passes
     *
     * @return {@link GennaroDKG} output
     */
    public static HashMap<Integer, PartialKeyPair> runDKG() {
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

    /**
     * Computes partial decryptions of a number of ballots
     *<br>
     * ONLY works if {@link TestDecrypterImpl} passes
     * @param ballotFetcher ballotFetcher for ballots
     * @param candidates    list of candidates in the election
     * @param dkgRes        output from a DKG protocol run - for instance {@link #runDKG()}
     * @return map with keys '1', '2' and '3', and corresponding PartialResultList objects
     */
    public static Map<Integer, PartialResultList> computePartialDecryptions(BallotFetcher ballotFetcher,
                                                                            List<Candidate> candidates,
                                                                            HashMap<Integer, PartialKeyPair> dkgRes) {
        PublicKey pk = dkgRes.get(1).getPublicKey();
        HashMap<Integer, PartialResultList> res = new HashMap<>();
        long endTime = new Date().getTime() - 1;
        for (int i = 1; i < 4; i++) {
            DecrypterImpl decrypter = new DecrypterImpl(i, ballotFetcher, b -> VoteProofUtils.verifyBallot(b, pk), candidates);
            res.put(i, decrypter.generatePartialResult(endTime, dkgRes.get(i)));

        }
        return res;
    }
}
