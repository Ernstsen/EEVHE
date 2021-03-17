package dk.mmj.eevhe.client.results;

import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.exceptions.UnableToDecryptException;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.interfaces.BallotFetcher;
import dk.mmj.eevhe.interfaces.PublicInfoFetcher;
import dk.mmj.eevhe.interfaces.ResultCombiner;
import dk.mmj.eevhe.server.decryptionauthority.DecrypterImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ResultsCombiner implementation combining Partial Decryptions output by
 * {@link DecrypterImpl}
 */
public class ResultCombinerImpl implements ResultCombiner {
    private final Logger logger = LogManager.getLogger(ResultCombinerImpl.class);
    private final boolean forceCalculation;
    private final PublicKey publicKey;
    private final List<Candidate> candidates;
    private final PublicInfoFetcher infoFetcher;
    private final BallotFetcher ballotFetcher;
    private final long endTime;

    /**
     * @param forceCalculation whether encrypted vote totals must be calculated, even if all DAs agree on their value
     * @param publicKey        public key, used by majority of DAs
     * @param candidates       list of candidates in the election
     * @param infoFetcher      fetcher for {@link PartialPublicInfo} entities for the DAs
     * @param ballotFetcher    fetcher for all ballots cast throughout the election
     * @param endTime          timestamp used in determining which votes were cast within the time limit
     */
    public ResultCombinerImpl(boolean forceCalculation, PublicKey publicKey, List<Candidate> candidates,
                              PublicInfoFetcher infoFetcher, BallotFetcher ballotFetcher, long endTime) {
        this.forceCalculation = forceCalculation;
        this.publicKey = publicKey;
        this.candidates = candidates;
        this.infoFetcher = infoFetcher;
        this.ballotFetcher = ballotFetcher;
        this.endTime = endTime;
    }

    @Override
    public ElectionResult computeResult(List<PartialResultList> partialResultLists) {

        List<MinimalPartialResult> minimalPartials = getSumCiphertextArray(partialResultLists, endTime);

        Map<Integer, PartialPublicInfo> publicInfos = infoFetcher.fetch()
                .stream().collect(Collectors.toMap(PartialPublicInfo::getSenderId, (i) -> i));

        List<Map<Integer, BigInteger>> listOfPartialMaps = new ArrayList<>();
        logger.info("Combining partial results to total results");
        for (int candidateIdx = 0; candidateIdx < candidates.size(); candidateIdx++) {
            HashMap<Integer, BigInteger> decAuthorityIdToCiphertextCMap = new HashMap<>();

            MinimalPartialResult candidatePartial = minimalPartials.get(candidateIdx);
            CipherText sumCiphertext = candidatePartial.getCipherText();

            for (PartialResultList partialResultList : partialResultLists) {
                PartialResult result = partialResultList.getResults().get(candidateIdx);
                CipherText partialDecryption = new CipherText(result.getResult(), sumCiphertext.getD());

                PartialPublicInfo info = publicInfos.get(result.getId());
                PublicKey partialPublicKey = new PublicKey(
                        info.getPartialPublicKey(),
                        info.getPublicKey().getG(),
                        info.getPublicKey().getQ());

                boolean validProof = DLogProofUtils.verifyProof(sumCiphertext, partialDecryption, partialPublicKey, result.getProof(), result.getId());

                if (validProof) {
                    decAuthorityIdToCiphertextCMap.put(result.getId(), result.getResult());
                }
            }

            listOfPartialMaps.add(decAuthorityIdToCiphertextCMap);
        }


        int amountOfVotes = minimalPartials.get(0).getVoteCount();
        ArrayList<Integer> resultList = new ArrayList<>(candidates.size());
        try {
            logger.info("Attempting to decrypt from " + listOfPartialMaps.get(0).size() + " partials");
            for (Candidate candidate : candidates) {
                int idx = candidate.getIdx();
                Map<Integer, BigInteger> partialMap = listOfPartialMaps.get(idx);
                BigInteger cs = SecurityUtils.lagrangeInterpolate(partialMap, publicKey.getP());
                CipherText cipherText = minimalPartials.get(idx).getCipherText();
                int result = ElGamal.homomorphicDecryptionFromPartials(cipherText.getD(), cs, publicKey.getG(), publicKey.getP(), amountOfVotes);
                resultList.add(idx, result);
            }
        } catch (UnableToDecryptException e) {
            logger.error("Failed to decrypt from partial decryptions.", e);
            return null;
        }

        return new ElectionResult(resultList, amountOfVotes);
    }

    /**
     * Determines and returns the List of (Ciphertext, #votes) to be used in determining the election result
     *
     * @param results the list of <code>PartialResultList</code> to be evaluated
     * @param endTime the endTime for the election - fetched from the <code>BulletinBoard</code>
     * @return List of <code>MinimalPartialResult</code> to be used in retrieval of election results
     */
    private List<MinimalPartialResult> getSumCiphertextArray(List<PartialResultList> results, long endTime) {
        if (!forceCalculation) {
            logger.info("Checking if fetched ciphertexts and amount of collected votes match");
        }
        boolean decryptionAuthoritiesAgrees = decryptionAuthoritiesAgrees(results);

        if (forceCalculation || !decryptionAuthoritiesAgrees) {
            if (!decryptionAuthoritiesAgrees) {
                logger.info("Fetched partial results differed in ciphertexts or amount of collected votes. Computing own.");
            }
            if (forceCalculation) {
                logger.info("Forcing local calculations on votes");
            }

            logger.info("Fetching ballots");
            List<PersistedBallot> ballots = ballotFetcher.getBallots();

            logger.debug("Filtering ballots");
            List<PersistedBallot> validBallots = ballots.parallelStream()
                    .filter(v -> v.getTs().getTime() < endTime)
                    .filter(v -> VoteProofUtils.verifyBallot(v, publicKey))
                    .collect(Collectors.toList());

            logger.info("Summing votes");
            List<MinimalPartialResult> res = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                final int idx = i;
                List<CandidateVoteDTO> votesForI = validBallots.parallelStream()
                        .map(b -> b.getCandidateVotes().get(idx))
                        .collect(Collectors.toList());

                CipherText sumCiphertext = SecurityUtils.concurrentVoteSum(votesForI, publicKey, 1000);
                int amountOfVotes = validBallots.size();
                res.add(new MinimalPartialResult(sumCiphertext, amountOfVotes));
            }
            return res;
        } else {
            PartialResultList firstDA = results.get(0);
            logger.info("Fetched ciphertexts and number of votes were equal");
            logger.info("Using ciphertext and amount of collected votes from DA with id=" + firstDA.getResults().get(0).getId());
            return firstDA.getResults().stream()
                    .map(pr -> new MinimalPartialResult(pr.getCipherText(), firstDA.getVoteCount()))
                    .collect(Collectors.toList());
        }
    }


    /**
     * Iterates through partial results and determines whether the Decryption Authorities agrees on the ciphertexts,
     * and number of votes registered
     *
     * @param partialResultLists the list of partialResultLists to be iterated through
     * @return whether all DAs agrees on ciphertexts and number of registered votes
     */
    private boolean decryptionAuthoritiesAgrees(List<PartialResultList> partialResultLists) {
        int size = candidates.size();

        boolean sameVoteCount = partialResultLists.stream().map(PartialResultList::getVoteCount).allMatch(i -> i == partialResultLists.get(0).getVoteCount());
        if (!sameVoteCount) {
            logger.info("DAs differed in number of votes");
            return false;
        }

        for (int candidateIdx = 0; candidateIdx < size; candidateIdx++) {
            ArrayList<PartialResult> partialsForCandidate = new ArrayList<>();
            for (PartialResultList partialResultList : partialResultLists) {
                partialsForCandidate.add(partialResultList.getResults().get(candidateIdx));
            }
            if (partialResultsDiffers(partialsForCandidate)) {
                logger.info("Some DA did not match the first DA on candidate with index: " + candidateIdx);
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether a list of partial results matches
     *
     * @param results the list of partial results
     * @return <code>False</code> if all partialResults has the same voteCount, and ciphertext. <code>True</code> otherwise
     */
    private boolean partialResultsDiffers(List<PartialResult> results) {
        List<CipherText> cipherTexts = results.stream().map(PartialResult::getCipherText).collect(Collectors.toList());

        boolean ciphertextEquals = cipherTexts.stream().allMatch(cipherTexts.get(0)::equals);

        return !ciphertextEquals;
    }

    private static class MinimalPartialResult {
        private final CipherText cipherText;
        private final int voteCount;

        public MinimalPartialResult(CipherText cipherText, int voteCount) {
            this.cipherText = cipherText;
            this.voteCount = voteCount;
        }

        public CipherText getCipherText() {
            return cipherText;
        }

        public int getVoteCount() {
            return voteCount;
        }
    }
}
