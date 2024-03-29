package dk.mmj.eevhe.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import dk.mmj.eevhe.client.results.ElectionResult;
import dk.mmj.eevhe.client.results.ResultCombinerImpl;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.entities.wrappers.PartialResultWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Class for retrieving vote results
 */
public class ResultFetcher extends Client {
    private static final Logger logger = LogManager.getLogger(ResultFetcher.class);
    private final boolean forceCalculation;
    private ElectionResult electionResult;

    @SuppressWarnings("unused")//Used through reflection
    public ResultFetcher(ResultFetcherConfiguration configuration) {
        super(configuration);
        this.forceCalculation = configuration.forceCalculations;
    }

    @SuppressWarnings("unused")//For gui impl
    public ResultFetcher(String address, boolean forceCalculation) {
        super(new ResultFetcherConfiguration(address, forceCalculation, Paths.get("certs/test_glob.pem")));
        this.forceCalculation = forceCalculation;
    }

    @Override
    public void run() {
        logger.info("Initializing");
        PublicKey publicKey = getPublicKey();

        logger.info("Fetching public information");
        PartialPublicInfo publicInformationEntity = fetchPublicInfo();
        if (publicInformationEntity == null) {
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
        List<SignedEntity<PartialResultList>> signedResults = FetchingUtilities.fetch(
                target.path("result"),
                new TypeReference<PartialResultWrapper>() {
                },
                getBBPeerCertificates(),
                logger
        ).getContent();

        if (signedResults == null || signedResults.isEmpty()) {
            logger.info("Did not fetch any results. Probable cause is unfinished decryption. Try again later");
            return;
        }

        List<PartialResultList> results = signedResults.stream()
                .filter(this::verifySignature)
                .map(SignedEntity::getEntity)
                .collect(Collectors.toList());

        List<Candidate> candidates = getCandidates();

        if (candidates == null) {
            logger.error("Failed to fetch list of candidates");
            return;
        }

        ResultCombinerImpl combiner = new ResultCombinerImpl(
                forceCalculation, publicKey, candidates,
                () -> FetchingUtilities.getPublicInfos(logger, target, cert, getBBPeerCertificates()),
                () -> FetchingUtilities.getBallots(logger, target, getBBPeerCertificates()),
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

    private boolean verifySignature(SignedEntity<PartialResultList> entity) {
        try {
            int daId = entity.getEntity().getDaId();
            Optional<PartialPublicInfo> any = fetchPublicInfos().stream().filter(inf -> inf.getSenderId() == daId).findAny();

            if (any.isPresent()) {
                return entity.verifySignature(FetchingUtilities.getSignaturePublicKey(any.get(), cert, logger));
            } else {
                logger.error("Failed to find PartialPublicInfo for da with id=" + daId);
                return false;
            }

        } catch (JsonProcessingException e) {
            logger.error("Failed to verify signature of entity: " + entity + ", due to exception", e);
            return false;
        }
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
         * @param targetUrl         url for {@link dk.mmj.eevhe.server.bulletinboard.BulletinBoardEdge} to get data from
         * @param forceCalculations whether ciphertext containing sum of votes should be computed locally
         * @param electionCertPath  path to global election certificate
         */
        ResultFetcherConfiguration(String targetUrl, boolean forceCalculations, Path electionCertPath) {
            super(ResultFetcher.class, targetUrl, electionCertPath);
            this.forceCalculations = forceCalculations;
        }

        boolean isForceCalculations() {
            return forceCalculations;
        }
    }
}
