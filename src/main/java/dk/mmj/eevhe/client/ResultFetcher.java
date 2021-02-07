package dk.mmj.eevhe.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.exceptions.UnableToDecryptException;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for retrieving vote results
 */
public class ResultFetcher extends Client {
    private static final Logger logger = LogManager.getLogger(ResultFetcher.class);
    private final boolean forceCalculation;

    public ResultFetcher(ResultFetcherConfiguration configuration) {
        super(configuration);
        this.forceCalculation = configuration.forceCalculations;
    }

    @Override
    public void run() {
        logger.info("Initializing");
        PublicKey publicKey = getPublicKey();

        logger.info("Fetching public information");
        PublicInformationEntity publicInformationEntity = fetchPublicInfo();
        long endTime = publicInformationEntity.getEndTime();
        if (new Date().getTime() < endTime) {
            long diff = (endTime - new Date().getTime()) / 60_000;
            logger.info("The vote has not yet terminated, so results are unavailable. " +
                    "The vote should terminate in about " + diff + " minutes");
            return;
        }

        logger.info("Fetching partial results");
        ResultList resultList = target.path("result").request().get(ResultList.class);
        List<PartialResultList> results = resultList.getResults();
        if (results == null || results.isEmpty()) {
            logger.info("Did not fetch any results. Probable cause is unfinished decryption. Try again later");
            return;
        }

        List<MinimalPartialResult> minimalPartials = getSumCiphertextArray(results, endTime);

        List<Map<Integer, BigInteger>> listOfPartialMaps = new ArrayList<>();
        logger.info("Combining partial results to total results");
        for (int candIdx = 0; candIdx < getCandidates().size(); candIdx++) {
            HashMap<Integer, BigInteger> decAuthorityIdToCiphertextCMap = new HashMap<>();

            MinimalPartialResult candidatePartial = minimalPartials.get(candIdx);
            CipherText sumCiphertext = candidatePartial.getCipherText();

            for (PartialResultList partialResultList : results) {
                PartialResult result = partialResultList.getResults().get(candIdx);

                CipherText partialDecryption = new CipherText(result.getResult(), sumCiphertext.getD());
                PublicKey partialPublicKey = new PublicKey(
                        publicInformationEntity.getPublicKeys().get(result.getId()),
                        publicInformationEntity.getG(),
                        publicInformationEntity.getQ());
                boolean validProof = DLogProofUtils.verifyProof(sumCiphertext, partialDecryption, partialPublicKey, result.getProof(), result.getId());

                if (validProof) {
                    decAuthorityIdToCiphertextCMap.put(result.getId(), result.getResult());
                }
            }

            listOfPartialMaps.add(decAuthorityIdToCiphertextCMap);
        }


        int amountOfVotes = minimalPartials.get(0).getVoteCount();
        StringBuilder resBuilder = new StringBuilder().append("Results:\n-----------------------------\n");
        try {
            logger.info("Attempting to decrypt from " + listOfPartialMaps.get(0).size() + " partials");

            for (Candidate candidate : getCandidates()) {
                int idx = candidate.getIdx();
                Map<Integer, BigInteger> partialMap = listOfPartialMaps.get(idx);
                resBuilder.append("Candidate: ")
                        .append(candidate.getName())
                        .append("(").append(idx).append(")")
                        .append("\nReceived :");
                BigInteger cs = SecurityUtils.combinePartials(partialMap, publicKey.getP());
                CipherText cipherText = minimalPartials.get(idx).getCipherText();
                int result = ElGamal.homomorphicDecryptionFromPartials(cipherText.getD(), cs, publicKey.getG(), publicKey.getP(), amountOfVotes);
                resBuilder.append(result).append("votes, from ").append(amountOfVotes).append(" cast in total\n\n");

            }
        } catch (UnableToDecryptException e) {
            logger.error("Failed to decrypt from partial decryptions.", e);
            System.exit(-1);
        }

        logger.info(resBuilder.toString());
    }

    /**
     * Determines and returns the List of (Ciphertext, #votes) to be used in determining the election result
     *
     * @param results the list of <code>PartialResultList</code> to be evaluated
     * @param endTime the endTime for the election - fetched from the <code>BulletinBoard</code>
     * @return List of <code>MinimalPartialResult</code> to be used in retrieval of election results
     */
    private List<MinimalPartialResult> getSumCiphertextArray(List<PartialResultList> results, long endTime) {
        PublicKey publicKey = getPublicKey();
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
            BallotList ballots;
            List<PersistedBallot> validBallots;
            try {
                String getBallotResponse = target.path("getBallots").request().get(String.class);
                ballots = new ObjectMapper().readValue(getBallotResponse, BallotList.class);
                logger.debug("Filtering ballots");
                validBallots = ballots.getBallots().parallelStream()
                        .filter(v -> v.getTs().getTime() < endTime)
                        .filter(v -> VoteProofUtils.verifyBallot(v, publicKey))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                logger.error("Failed to fetch ballots from server", e);
                System.exit(-1);
                return null;
            }

            logger.info("Summing votes");
            List<MinimalPartialResult> res = new ArrayList<>();
            for (int i = 0; i < getCandidates().size(); i++) {
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
        int size = getCandidates().size();

        boolean sameVoteCount = partialResultLists.stream().map(PartialResultList::getVoteCount).allMatch(i -> i == partialResultLists.get(0).getVoteCount());
        if(!sameVoteCount){
            logger.info("DAs differed in number of votes");
            return false;
        }

        for (int candIdx = 0; candIdx < size; candIdx++) {
            ArrayList<PartialResult> partialsForCandidate = new ArrayList<>();
            for (PartialResultList partialResultList : partialResultLists) {
                partialsForCandidate.add(partialResultList.getResults().get(candIdx));
            }
            if (partialResultsDiffers(partialsForCandidate)) {
                logger.info("Some DA did not match the first DA on candidate with index: " + candIdx);
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

    /**
     * The Result fetcher Configuration.
     * <br/>
     * Created in the {@link ClientConfigBuilder}.
     */
    public static class ResultFetcherConfiguration extends ClientConfiguration {
        private final boolean forceCalculations;

        /**
         * @param targetUrl         url for {@link dk.mmj.eevhe.server.bulletinboard.BulletinBoard} to get data from
         * @param forceCalculations whether ciphertext containing sum of votes should be computed locally
         */
        ResultFetcherConfiguration(String targetUrl, boolean forceCalculations) {
            super(targetUrl);
            this.forceCalculations = forceCalculations;
        }
    }

    public static class MinimalPartialResult {
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
