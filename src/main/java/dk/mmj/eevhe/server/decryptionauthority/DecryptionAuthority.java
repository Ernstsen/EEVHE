package dk.mmj.eevhe.server.decryptionauthority;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.Configuration;
import dk.mmj.eevhe.client.FetchingUtilities;
import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.AbstractServer;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.client.JerseyWebTarget;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;
import static dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthorityResource.statePrefix;

public class DecryptionAuthority extends AbstractServer {
    private static final Logger logger = LogManager.getLogger(DecryptionAuthority.class);
    private static final String RSA_PUBLIC_KEY_NAME = "rsa";
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final JerseyWebTarget bulletinBoard;
    private final int port;
    private final Integer id;
    private final boolean integrationTest;
    private DecryptionAuthorityInput input;
    private boolean timeCorrupt = false;
    private PartialSecretKey sk;
    private long endTime;
    private PublicKey pk;
    private final ObjectMapper mapper = new ObjectMapper();

    private final HashMap<Integer, BigInteger[]> commitmentMap = new HashMap<>();

    public DecryptionAuthority(DecryptionAuthorityConfiguration configuration) {

        port = configuration.port;
        id = configuration.id;
        integrationTest = configuration.integrationTest;

        if (configuration.timeCorrupt > 0) {
            timeCorrupt = true;
        }

        bulletinBoard = configureWebTarget(logger, configuration.bulletinBoard);

        File conf = new File(configuration.confPath);
        if (!conf.exists() || !conf.isFile()) {
            logger.error("Configuration file either did not exists or were not a file. Path: " + conf.getAbsolutePath() + "\nTerminating");
            System.exit(-1);
        }


        try {
            input = mapper.readValue(conf, DecryptionAuthorityInput.class);

            endTime = input.getEndTime();
            long relativeEndTime = endTime - new Date().getTime();

            if (timeCorrupt) {
                relativeEndTime -= configuration.timeCorrupt; //30 sec.
            }

            scheduler.schedule(this::terminateVoting, relativeEndTime, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            logger.error("Failed to read authorities input file", e);
            System.exit(-1);
        }
    }

    @Override
    protected void afterStart() {
        scheduler.execute(this::startKeyGenProtocol);
    }

    /**
     * Initializes the key-generation protocol by sending polynomial to the other peers,
     * and awaiting input from the other authorities
     */
    private void startKeyGenProtocol() {
        if (integrationTest) {
            integrationTestStateWorkaround();
        }

        Map<Integer, String> daInfos = input.getInfos()
                .stream().collect(Collectors.toMap(DecryptionAuthorityInfo::getId, DecryptionAuthorityInfo::getAddress));
        daInfos.remove(id);//We remove ourself, to be able to iterate without

        //We chose our polynomial
        BigInteger[] pol = new BigInteger[]{BigInteger.valueOf(2), BigInteger.valueOf(3)};//TODO


        //Calculates commitments
        BigInteger[] commitments = new BigInteger[]{BigInteger.valueOf(2), BigInteger.valueOf(3)};//TODO

        Entity<CommitmentDTO> commitmentsEntity = Entity.entity(new CommitmentDTO(commitments, id), MediaType.APPLICATION_JSON);
        Response postCommitments = bulletinBoard.path("postCommitments").request().post(commitmentsEntity);
        if (!(postCommitments.getStatus() == 200)) {
            logger.error("failed to post commitments to bulletin board. Terminating. Status: " + postCommitments.getStatus());
            System.exit(-1);
        }

        //Post commitments to bulletin board
        for (Integer daId : daInfos.keySet()) {
            JerseyWebTarget da = configureWebTarget(logger, daInfos.get(daId));

            BigInteger fij = BigInteger.valueOf(87); //f_i(j) //TODO
            Entity<PartialSecretMessage> partialSecretEntity = Entity.entity(new PartialSecretMessage(fij, id), MediaType.APPLICATION_JSON);
            Response resp = da.path("postPartialSecret").request().post(partialSecretEntity);
            if (!(resp.getStatus() == 200)) {
                logger.error("failed to post f_i(j) to DA with id=" + da + ". Terminating. Status: " + resp.getStatus());
            }
        }

        scheduler.schedule(this::verifyReceivedValues, 4, TimeUnit.SECONDS);
    }

    /**
     * Verifies received secret-values as part of the key-generation protocol
     */
    private void verifyReceivedValues() {
        boolean hasReceivedFromAll = true;
        List<Integer> ids = input.getInfos().stream()
                .map(DecryptionAuthorityInfo::getId)
                .filter(i -> i != id.intValue())
                .collect(Collectors.toList());

        ServerState state = ServerState.getInstance();
        HashMap<Integer, PartialSecretMessage> secrets = new HashMap<>();
        for (Integer integer : ids) {
            PartialSecretMessage ps = state.get(partialSecretKey(integer), PartialSecretMessage.class);
            if (ps != null) {
                secrets.put(integer, ps);
            } else {
                hasReceivedFromAll = false;
            }
        }

        if (!hasReceivedFromAll) {//TODO: When should we just ignore the missing value(s)? Otherwise risk corruption killing election by non-participation
            scheduler.schedule(this::verifyReceivedValues, 200, TimeUnit.MILLISECONDS);
            return;
        }

        //TODO: Check partial secrets, and potentially send complaint to BB
        //TODO: BB fetch of commitments, and compare with secret values
        String commitmentsString = bulletinBoard.path("commitments").request().get(String.class);
        TypeReference<List<CommitmentDTO>> commitmentListType = new TypeReference<List<CommitmentDTO>>() {
        };
        List<CommitmentDTO> commitments;

        try {
            commitments = mapper.readValue(commitmentsString, commitmentListType);
        } catch (IOException e) {
            logger.error("Failed to read commitments from BulletinBoard! Failing.", e);
            System.exit(-1);
            return;
        }

        for (CommitmentDTO commitment : commitments) {
            int id = commitment.getId();

            PartialSecretMessage partialSecretMessage = secrets.get(id);
            if (partialSecretMessage == null) {
                logger.error("Commitment with id=" + id + ", had no corresponding secret! Ignoring");
                continue;
            }

            Predicate<CommitmentDTO> verifier = (m) -> m.getCommitment() != null;//TODO: Placeholder for actual verifier!

            if (!verifier.test(commitment)) {
                Entity<ComplaintDTO> entity = Entity.entity(new ComplaintDTO(commitment.getId()), MediaType.APPLICATION_JSON);
                bulletinBoard.path("complain").request().post(entity);
            } else {
                commitmentMap.put(id, commitment.getCommitment());
            }
        }

        scheduler.schedule(this::handleComplaints, 20, TimeUnit.SECONDS);
    }

    /**
     * Resolves complaints made about values from this DA, as part of the key-generation protocol
     */
    private void handleComplaints() {
        String complaintsString = bulletinBoard.path("complaints").request().get(String.class);
        List<ComplaintDTO> complaints;
        try {
            complaints = mapper.readValue(complaintsString, new TypeReference<List<ComplaintDTO>>() {
            });
        } catch (IOException e) {
            logger.error("Failed to read complaint list from Bulletin Board", e);
            System.exit(-1);
            return;
        }

        for (ComplaintDTO complaint : complaints) {
            if (complaint.getId() == id) {
                BigInteger complaintValue = BigInteger.ZERO;//TODO: Which value?!?
                ComplaintResolveDTO complaintResolveDTO = new ComplaintResolveDTO(id, complaintValue);
                bulletinBoard.path("resolveComplaint").request().post(Entity.entity(complaintResolveDTO, MediaType.APPLICATION_JSON));
            }
        }

        scheduler.schedule(this::checkIfComplaintsAreResolved, 20, TimeUnit.SECONDS);
    }

    /**
     * Asserts that all raised complaints are resolved, and updates state to reflect resolves.
     * <br>
     * Part of the key-generation protocol
     */
    private void checkIfComplaintsAreResolved() {
        String complaintResolvesString = bulletinBoard.request("complaintResolves").get(String.class);
        List<ComplaintResolveDTO> resolves;
        try {
            resolves = mapper.readValue(complaintResolvesString, new TypeReference<List<ComplaintResolveDTO>>() {
            });
        } catch (IOException e) {
            logger.error("Failed to read resolved complaints from Bulletin Board. Terminating", e);
            System.exit(-1);
            return;
        }

        ServerState state = ServerState.getInstance();

        for (ComplaintResolveDTO resolve : resolves) {
            boolean resolveIsVerifiable = true; //TODO: CHECK!
            if (resolveIsVerifiable) {
                state.put(partialSecretKey(resolve.getId()), resolve.getValue());
            }
        }

        scheduler.schedule(this::combineKeys, 20, TimeUnit.SECONDS);
    }

    /**
     * Finalizes the key-generation protocol, by combining the data received to a public key, and a partial secret key
     */
    private void combineKeys() {
        BigInteger g = BigInteger.ONE;//TODO - where will we get this? Common input?
        BigInteger q = BigInteger.ONE;//TODO - where will we get this? Common input?
        BigInteger p = BigInteger.ONE;//TODO - where will we get this? Common input?
        BigInteger combineToPublicKey = BigInteger.TEN; //TODO!
        BigInteger combineToPartialSecret = BigInteger.TEN; //TODO!
        pk = new PublicKey(combineToPublicKey, g, q);
        sk = new PartialSecretKey(combineToPartialSecret, p);
    }

    private String partialSecretKey(Integer id) {
        return statePrefix(integrationTest ? this.id : null) + "secret:" + id;
    }

    /**
     * Posts id to self.
     * <br>
     * Workaround as static vars are not respected in the integrationTest setup
     */
    private void integrationTestStateWorkaround() {
        JerseyWebTarget me = configureWebTarget(logger, "127.0.0.1:" + getPort());
        Response idResponse = me.path("id").request().post(Entity.entity(this.id, MediaType.APPLICATION_JSON));
        if (idResponse.getStatus() != 200) {
            logger.error("Failed to write id to own state for integration test work-around. Failing. Status: " + idResponse.getStatus());
            System.exit(-1);
        }
    }


    private void terminateVoting() {
        Long bulletinBoardTime = new Long(bulletinBoard.path("getCurrentTime").request().get(String.class));
        PublicInformationEntity pi = FetchingUtilities.fetchPublicInfo(logger, RSA_PUBLIC_KEY_NAME, bulletinBoard);
        List<Candidate> candidates = pi.getCandidates();

        long remainingTime = endTime - bulletinBoardTime;

        if (!timeCorrupt && remainingTime > 0) {
            logger.info("Attempted to collect votes from BB, but voting not finished. Retrying in " + (remainingTime / 1000) + "s");
            scheduler.schedule(this::terminateVoting, remainingTime, TimeUnit.MILLISECONDS);
            return;
        }

        logger.info("Terminating voting - Fetching ballots");
        List<PersistedBallot> ballots = getBallots();

        if (ballots == null || ballots.size() < 1) {
            logger.error("No votes registered. Terminating server without result");
            terminate();
            return;
        }

        logger.info("Verifying ballots");
        ballots = ballots.parallelStream()
                .filter(v -> v.getTs().getTime() < endTime)
                .filter(b -> VoteProofUtils.verifyBallot(b, pk)).collect(Collectors.toList());


        logger.info("Summing votes");
        Map<Integer, List<CandidateVoteDTO>> votes = new HashMap<>(candidates.size());
        ballots.forEach(b -> {
            for (int i = 0; i < candidates.size(); i++) {
                List<CandidateVoteDTO> lst = votes.computeIfAbsent(i, j -> new ArrayList<>());
                lst.add(b.getCandidateVotes().get(i));
            }
        });

        CipherText[] sums = new CipherText[candidates.size()];

        for (int i = 0; i < candidates.size(); i++) {
            sums[i] = SecurityUtils.concurrentVoteSum(votes.get(i), pk, 1000);
        }

        logger.info("Beginning partial decryption");

        BigInteger[] results = new BigInteger[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            results[i] = ElGamal.partialDecryption(sums[i].getC(), sk.getSecretValue(), sk.getP());
        }

        logger.info("Partially decrypted value. Generating proof");

        PublicKey partialPublicKey = new PublicKey(pk.getG().modPow(sk.getSecretValue(), sk.getP()), pk.getG(), pk.getQ());

        DLogProofUtils.Proof[] proofs = new DLogProofUtils.Proof[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            proofs[i] = DLogProofUtils.generateProof(sums[i], sk.getSecretValue(), partialPublicKey, id);
        }

        logger.info("Posting to bulletin board");
        ArrayList<PartialResult> partialResults = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            partialResults.add(new PartialResult(id, results[i], proofs[i], sums[i], ballots.size()));
        }

        Entity<PartialResultList> resultEntity = Entity.entity(new PartialResultList(partialResults, ballots.size()), MediaType.APPLICATION_JSON);
        Response post = bulletinBoard.path("result").request().post(resultEntity);

        if (post.getStatus() < 200 || post.getStatus() > 300) {
            logger.error("Unable to post result to bulletinBoard, got response:" + post);
            System.exit(-1);
        } else {
            logger.info("Successfully transferred partial decryption to bulletin board");
        }

    }

    /**
     * Retrieves cast ballots from BulletinBoard
     *
     * @return list of ballots from BulletinBoard
     */
    private List<PersistedBallot> getBallots() {
        try {
            String getVotes = bulletinBoard.path("getBallots").request().get(String.class);
            BallotList voteObjects = mapper.readValue(getVotes, BallotList.class);
            ArrayList<PersistedBallot> ballots = new ArrayList<>();

            for (Object ballot : voteObjects.getBallots()) {
                if (ballot instanceof PersistedBallot) {
                    ballots.add((PersistedBallot) ballot);
                } else {
                    logger.error("Found ballot that was not correct class. Was " + ballot.getClass() + ". Terminating server");
                    terminate();
                }
            }
            return ballots;
        } catch (IOException e) {
            logger.error("Failed to read BallotList from JSON string", e);
            return null;
        }
    }

    @Override
    protected void configure(ServletHolder servletHolder) {
        servletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                DecryptionAuthorityResource.class.getCanonicalName());
    }

    @Override
    protected int getPort() {
        return port;
    }

    @SuppressWarnings("unused")
    public static class PartialSecretMessage {
        private BigInteger partialSecret;
        private Integer id;

        public PartialSecretMessage(BigInteger partialSecret, Integer id) {
            this.partialSecret = partialSecret;
            this.id = id;
        }

        public PartialSecretMessage() {
        }

        public BigInteger getPartialSecret() {
            return partialSecret;
        }

        public void setPartialSecret(BigInteger partialSecret) {
            this.partialSecret = partialSecret;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }

    /**
     * Configuration for a DecryptionAuthority
     */
    public static class DecryptionAuthorityConfiguration implements Configuration {
        private final Integer port;
        private final String bulletinBoard;
        private final String confPath;
        private final int timeCorrupt;
        private final Integer id;
        private final boolean integrationTest;

        DecryptionAuthorityConfiguration(int port, String bulletinBoard, String confPath, Integer id, int timeCorrupt, boolean integrationTest) {
            this.port = port;
            this.bulletinBoard = bulletinBoard;
            this.id = id;
            this.confPath = confPath;
            this.timeCorrupt = timeCorrupt;
            this.integrationTest = integrationTest;
        }
    }
}
