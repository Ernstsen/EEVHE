package dk.mmj.eevhe.crypto;

import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.KeyPair;
import dk.mmj.eevhe.entities.PersistedBallot;
import dk.mmj.eevhe.entities.PublicKey;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.crypto.TestUtils.generateBallots;
import static dk.mmj.eevhe.crypto.TestUtils.generateKeysFromP2048bitsG2;

@SuppressWarnings("unused")
public class BenchmarkBallotVerification {

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Fork(value = 1)
    @Measurement(iterations = 10, batchSize = 1)
    public void verifyBallots(Blackhole blackhole, BallotState ballotState) {
        List<PersistedBallot> collect = ballotState.ballots.stream()
                .filter(b -> VoteProofUtils.verifyBallot(b, ballotState.publicKey))
                .collect(Collectors.toList());
        blackhole.consume(collect);
    }

    @State(Scope.Benchmark)
    public static class BallotState {

        @Param({"100", "500", "1000", "10000", "20000"})
        public int size;

        @Param({"1", "4", "8", "10", "13"})
        public int candidates;

        public int partitionSize;

        public List<PersistedBallot> ballots;
        public PublicKey publicKey;

        @Setup(Level.Trial)
        public void setUp() {
            KeyPair keyPair = generateKeysFromP2048bitsG2();
            publicKey = keyPair.getPublicKey();
            ballots = generateBallots(size, publicKey, candidates);
            partitionSize = ballots.size() / 10;
        }
    }
}
