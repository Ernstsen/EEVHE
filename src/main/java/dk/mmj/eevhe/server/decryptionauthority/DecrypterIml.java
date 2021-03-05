package dk.mmj.eevhe.server.decryptionauthority;

import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.decryptionauthority.interfaces.BallotFetcher;
import dk.mmj.eevhe.server.decryptionauthority.interfaces.BallotVerifier;
import dk.mmj.eevhe.server.decryptionauthority.interfaces.Decrypter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DecrypterIml implements Decrypter {

    private final Logger logger;
    private final BallotFetcher ballotFetcher;
    private final BallotVerifier ballotVerifier;
    private final List<Candidate> candidates;
    private final int id;

    public DecrypterIml(int id, BallotFetcher ballotFetcher, BallotVerifier ballotVerifier, List<Candidate> candidates) {
        this.id = id;
        this.logger = LogManager.getLogger(DecrypterIml.class.getName() + " ID=" + id);
        this.ballotFetcher = ballotFetcher;
        this.ballotVerifier = ballotVerifier;
        this.candidates = candidates;
    }

    @Override
    public PartialResultList generatePartialResult(long endTime, PartialKeyPair keyPair) {

        PublicKey pk = keyPair.getPublicKey();
        PartialSecretKey sk = keyPair.getPartialSecretKey();

        logger.info("Terminating voting - Fetching ballots");
        List<PersistedBallot> receivedBallots = ballotFetcher.getBallots();

        if (receivedBallots == null || receivedBallots.size() < 1) {
            logger.error("No votes registered. Terminating server without result");
            return null;
        }

        logger.info("Verifying ballots");
        List<PersistedBallot> ballots = receivedBallots.parallelStream()
                .filter(v -> v.getTs().getTime() < endTime)
                .filter(ballotVerifier::verifyBallot).collect(Collectors.toList());


        logger.info("Summing votes");
        Map<Integer, List<CandidateVoteDTO>> votes = new HashMap<>(candidates.size());
        ballots.forEach(b -> {
            for (int i = 0; i < candidates.size(); i++) {
                List<CandidateVoteDTO> lst = votes.computeIfAbsent(i, j -> new ArrayList<>());
                lst.add(b.getCandidateVotes().get(i));
            }
        });

        logger.info("Beginning partial decryption");

        PublicKey partialPublicKey = new PublicKey(keyPair.getPartialPublicKey(), pk.getG(), pk.getQ());

        ArrayList<PartialResult> partialResults = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            CipherText sum = SecurityUtils.concurrentVoteSum(votes.get(i), pk, 1000);
            BigInteger result = ElGamal.partialDecryption(sum.getC(), sk.getSecretValue(), sk.getP());
            DLogProofUtils.Proof proof = DLogProofUtils.generateProof(sum, sk.getSecretValue(), partialPublicKey, id);

            partialResults.add(new PartialResult(id, result, proof, sum));
            logger.info("Partially decrypted " + (i + 1) + "/" + candidates.size() + " candidates");
        }

        return new PartialResultList(partialResults, ballots.size());
    }
}
