package dk.mmj.eevhe.client;

import dk.mmj.eevhe.client.results.ElectionResult;
import dk.mmj.eevhe.client.results.ResultCombinerImpl;
import dk.mmj.eevhe.entities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * Class for retrieving vote results
 */
public class ResultFetcher extends Client {
    private static final Logger logger = LogManager.getLogger(ResultFetcher.class);
    private final boolean forceCalculation;
    private ElectionResult electionResult;

    public ResultFetcher(ResultFetcherConfiguration configuration) {
        super(configuration);
        this.forceCalculation = configuration.forceCalculations;
    }

    @Override
    public void run() {
        logger.info("Initializing");
        PublicKey publicKey = getPublicKey();

        logger.info("Fetching public information");
        PartialPublicInfo publicInformationEntity = fetchPublicInfo();
        if(publicInformationEntity == null){
            logger.error("Failed to fetch public information");
            return;
        }

        long endTime = publicInformationEntity.getEndTime();
        if (new Date().getTime() < endTime) {
            long diff = (endTime - new Date().getTime()) / 60_000;
            logger.info("The vote has not yet terminated, so results are unavailable. " +
                    "The vote should terminate in about " + diff + " minutes");
            return;
        }

        logger.info("Fetching partial results");
        ResultList fetchedResultList = target.path("result").request().get(ResultList.class);
        List<PartialResultList> results = fetchedResultList.getResults();
        if (results == null || results.isEmpty()) {
            logger.info("Did not fetch any results. Probable cause is unfinished decryption. Try again later");
            return;
        }

        List<Candidate> candidates = getCandidates();

        if(candidates == null){
            logger.error("Failed to fetch list of candidates");
            return;
        }

        ResultCombinerImpl combiner = new ResultCombinerImpl(
                forceCalculation, publicKey, candidates,
                () -> FetchingUtilities.getPublicInfos(logger, target),
                () -> FetchingUtilities.getBallots(logger, target),
                endTime);


        electionResult = combiner.computeResult(results);

        StringBuilder resBuilder = new StringBuilder().append("Results:\n-----------------------------\n");

        for (Candidate candidate : candidates) {
            int idx = candidate.getIdx();
            resBuilder.append("Candidate: ")
                    .append(candidate.getName())
                    .append("(").append(idx).append(")")
                    .append("\nReceived :")
                    .append(electionResult.getCandidateVotes().get(idx))
                    .append(" votes, from ")
                    .append(electionResult.getVotesTotal())
                    .append(" cast in total\n\n");
        }
        logger.info(resBuilder.toString());
    }

    /**
     * Enables extraction of election results
     *
     * @return results of the election - null if fetch was unsuccessful
     */
    public ElectionResult getElectionResult() {
        return electionResult;
    }

    /**
     * The Result fetcher Configuration.
     * <br>
     * Created in the {@link ClientConfigBuilder}.
     */
    public static class ResultFetcherConfiguration extends ClientConfiguration<ResultFetcher> {
        private final boolean forceCalculations;

        /**
         * @param targetUrl         url for {@link dk.mmj.eevhe.server.bulletinboard.BulletinBoard} to get data from
         * @param forceCalculations whether ciphertext containing sum of votes should be computed locally
         */
        ResultFetcherConfiguration(String targetUrl, boolean forceCalculations) {
            super(ResultFetcher.class, targetUrl);
            this.forceCalculations = forceCalculations;
        }

        boolean isForceCalculations() {
            return forceCalculations;
        }
    }
}
