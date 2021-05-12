package dk.mmj.eevhe.crypto;

import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParameters;
import dk.mmj.eevhe.crypto.keygeneration.PersistedKeyParameters;
import dk.mmj.eevhe.entities.*;
import org.apache.commons.collections4.ListUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TestUtils {
    private static KeyPair generateKeysForTesting(KeyGenerationParameters params) {
        BigInteger g = params.getGenerator();
        PrimePair primePair = params.getPrimePair();

        BigInteger secretKey = generateSecretKeyForTesting(primePair.getQ());
        PublicKey publicKey = generatePublicKeyForTesting(secretKey, g, primePair.getQ());

        return new KeyPair(secretKey, publicKey);
    }

    private static BigInteger generateSecretKeyForTesting(BigInteger q) {
        return SecurityUtils.getRandomNumModN(q);
    }

    private static PublicKey generatePublicKeyForTesting(BigInteger secretKey, BigInteger g, BigInteger q) {
        BigInteger p = q.multiply(BigInteger.valueOf(2)).add(BigInteger.ONE);
        BigInteger h = g.modPow(secretKey, p);

        return new PublicKey(h, g, q);
    }

    static KeyPair generateKeysFromP11G2() {
        return generateKeysForTesting(getKeyGenParamsFromP11G2());
    }

    public static KeyPair generateKeysFromP2048bitsG2() {
        return generateKeysForTesting(getKeyGenParamsFromP2048bitsG2());
    }

    static KeyGenerationParameters getKeyGenParamsFromP2048bitsG2() {
        String pString = "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1 29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245 E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F 83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D 670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9 DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510 15728E5A 8AACAA68 FFFFFFFF FFFFFFFF";
        return new PersistedKeyParameters(pString, "2");
    }

    static KeyGenerationParameters getKeyGenParamsFromP11G2() {
        PrimePair primes = new PrimePair(new BigInteger("11"), new BigInteger("5"));
        BigInteger g = new BigInteger("2");
        return new TestElGamal.SimpleKeyGenParams(g, primes);
    }

    static KeyGenerationParameters getKeyGenParamsFromP227G172() {
        PrimePair primes = new PrimePair(new BigInteger("227"), new BigInteger("113"));
        BigInteger g = new BigInteger("172"); //9025

        return new TestElGamal.SimpleKeyGenParams(g, primes);
    }

    /**
     * Concurrently generates a big number of votes, with different ID's
     *
     * @param publicKey public key to use in vote encryption and proofs
     * @return list of candidate votes
     */
    static List<CandidateVoteDTO> generateVotes(final PublicKey publicKey) {
        ArrayList<String> ids = new ArrayList<>();
        for (int i = 0; i < TestSecurityUtils.ITERATIONS; i++) {
            ids.add("ID" + i);
        }

        List<List<String>> partitions = ListUtils.partition(ids, TestSecurityUtils.ITERATIONS / 20);

        ConcurrentLinkedQueue<CandidateVoteDTO> res = new ConcurrentLinkedQueue<>();

        ArrayList<Thread> threads = new ArrayList<>();
        for (List<String> partition : partitions) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < partition.size(); i++) {
                    res.add(SecurityUtils.generateVote(i%2, partition.get(i), publicKey));
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to concurrently create votes", e);
            }
        }

        return new ArrayList<>(res);
    }


    /**
     * Concurrently generates a big number of votes, with different ID's
     *
     * @param amount    number of votes
     * @param publicKey public key to use in vote encryption and proofs
     * @param candidates number of candidates in election
     * @return list of ballots
     */
    static List<PersistedBallot> generateBallots(int amount, final PublicKey publicKey, int candidates) {
        ArrayList<String> ids = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            ids.add("ID" + i);
        }

        List<List<String>> partitions = ListUtils.partition(ids, amount / 20);

        ConcurrentLinkedQueue<PersistedBallot> res = new ConcurrentLinkedQueue<>();

        ArrayList<Thread> threads = new ArrayList<>();
        for (List<String> partition : partitions) {
            Thread thread = new Thread(new BallotCreator(partition, res, publicKey, candidates));
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to concurrently create votes", e);
            }
        }

        return new ArrayList<>(res);
    }

    private static class BallotCreator implements Runnable {
        private final List<String> ids;
        private final Collection<PersistedBallot> votes;
        private final PublicKey publicKey;
        private final int candidates;

        private BallotCreator(List<String> ids, Collection<PersistedBallot> votes, PublicKey publicKey, int candidates) {
            this.ids = ids;
            this.votes = votes;
            this.publicKey = publicKey;
            this.candidates = candidates;
        }

        @Override
        public void run() {
            ArrayList<PersistedBallot> result = new ArrayList<>();

            for (int i = 0; i < ids.size(); i++) {
                PersistedBallot e = new PersistedBallot(SecurityUtils.generateBallot(i % candidates, candidates, ids.get(i), publicKey));
                e.setTs(new Date());
                result.add(e);
            }

            votes.addAll(result);
        }
    }
}
