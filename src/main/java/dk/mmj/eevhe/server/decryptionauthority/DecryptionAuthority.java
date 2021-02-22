package dk.mmj.eevhe.server.decryptionauthority;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.Configuration;
import dk.mmj.eevhe.client.FetchingUtilities;
import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParameters;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.GennaroDKG;
import dk.mmj.eevhe.protocols.connectors.BulletinBoardBroadcaster;
import dk.mmj.eevhe.protocols.connectors.RestPeerCommunicator;
import dk.mmj.eevhe.protocols.connectors.ServerStateIncomingChannel;
import dk.mmj.eevhe.protocols.interfaces.PeerCommunicator;
import dk.mmj.eevhe.server.AbstractServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.client.JerseyWebTarget;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;
import static dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthorityResource.statePrefix;

public class DecryptionAuthority extends AbstractServer {
    private static final Logger logger = LogManager.getLogger(DecryptionAuthority.class);
    private static final String RSA_PUBLIC_KEY_NAME = "rsa";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final JerseyWebTarget bulletinBoard;
    private final int port;
    private final Integer id;
    private final boolean integrationTest;
    private final ObjectMapper mapper = new ObjectMapper();
    private KeyGenParams params;
    private GennaroDKG dkg;
    private DecryptionAuthorityInput input;
    private boolean timeCorrupt = false;
    private PartialSecretKey sk;
    private long endTime;
    private PublicKey pk;
    private List<Candidate> candidates;

    public DecryptionAuthority(DecryptionAuthorityConfiguration configuration) {

        port = configuration.port;
        id = configuration.id;
        integrationTest = configuration.integrationTest;

        if (configuration.timeCorrupt > 0) {
            timeCorrupt = true;
        }

        bulletinBoard = configureWebTarget(logger, configuration.bulletinBoard);
        try {
            candidates = mapper.readValue(new File("conf/testing_candidates.json"), new TypeReference<List<Candidate>>() {
            });
        } catch (IOException e) {
            logger.error("You moved the file, and are yet to do this properly!");
            System.exit(-1);
            return;
        }

        File conf = new File(configuration.confPath);
        if (!conf.exists() || !conf.isFile()) {
            logger.error("Configuration file either did not exists or were not a file. Path: " + conf.getAbsolutePath() + "\nTerminating");
            System.exit(-1);
        }

        try {
            input = mapper.readValue(conf, DecryptionAuthorityInput.class);

            BigInteger p = new BigInteger(Hex.decode(input.getpHex()));
            BigInteger g = new BigInteger(Hex.decode(input.getgHex()));
            BigInteger q = p.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2));
            params = new KeyGenParams(p, q, g);
            endTime = input.getEndTime();
            long relativeEndTime = endTime - new Date().getTime();

            if (timeCorrupt) {
                relativeEndTime -= configuration.timeCorrupt; //30 sec.
            }

            if (input.getInfos().stream().anyMatch(i -> i.getId() == 0)) {
                logger.error("Found DA with id=0, which is illegal!. Terminating");
                System.exit(-1);
            }

            scheduler.schedule(this::terminateVoting, relativeEndTime, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            logger.error("Failed to read authorities input file", e);
            System.exit(-1);
        }
    }

    @Override
    protected void afterStart() {
        if (integrationTest) {
            integrationTestStateWorkaround();
        }
        logger.info("Starting keyGeneration protocol for DA id=" + id);
        scheduler.execute(this::startKeyGenProtocol);
    }

    /**
     * Initializes the key-generation protocol by sending polynomial to the other peers,
     * and awaiting input from the other authorities
     */
    private void startKeyGenProtocol() {
        logger.info("Started keygen protocol for DA with id=" + id);
        Map<Integer, PeerCommunicator> communicators = input.getInfos()
                .stream()
                .collect(Collectors.toMap(DecryptionAuthorityInfo::getId, inf -> new RestPeerCommunicator(configureWebTarget(logger, inf.getAddress()))));
        communicators.remove(id);//We remove ourself, to be able to iterate without
        final ServerStateIncomingChannel incoming = new ServerStateIncomingChannel(
                input.getInfos().stream()
                        .map(DecryptionAuthorityInfo::getId)
                        .map(this::partialSecretKey)
                        .collect(Collectors.toList())
        );
        dkg = new GennaroDKG(new BulletinBoardBroadcaster(bulletinBoard), incoming, communicators, id, params, "ID: " + id);
        dkg.startProtocol();

        logger.info("scheduling verification of received values. DA with id=" + id);
        scheduler.schedule(this::verifyReceivedValues, 20, TimeUnit.SECONDS);
    }

    /**
     * Verifies received secret-values as part of the key-generation protocol
     */
    private void verifyReceivedValues() {
        logger.info("Checking received secret values, for DA with id=" + id);
        final boolean cont = dkg.handleReceivedValues();

        if (!cont) {//TODO: When should we just ignore the missing value(s)? Otherwise risk corruption killing election by non-participation
            logger.info("Has not received all commitments - retrying later");
            scheduler.schedule(this::verifyReceivedValues, 10, TimeUnit.SECONDS);
            return;
        }

        scheduler.schedule(this::handleComplaints, 10, TimeUnit.SECONDS);
    }

    /**
     * Resolves complaints made about values from this DA, as part of the key-generation protocol
     */
    private void handleComplaints() {
        logger.info("Fetching complaints from Bulletin Board");
        dkg.handleComplaints();
        scheduler.schedule(this::checkIfComplaintsAreResolved, 10, TimeUnit.SECONDS);
    }

    /**
     * Asserts that all raised complaints are resolved, and updates state to reflect resolves.
     * <br>
     * Part of the key-generation protocol
     */
    private void checkIfComplaintsAreResolved() {
        logger.info("" + id + ": checking for complaint resolves");
        dkg.applyResolves();

        scheduler.schedule(this::combineKeys, 1, TimeUnit.SECONDS);
    }

    /**
     * Finalizes the key-generation protocol, by combining the data received to a public key, and a partial secret key
     */
    private void combineKeys() {
        logger.info("Combining values, to make keys");
        PartialKeyPair partialKeyPair = dkg.createKeys();
        pk = partialKeyPair.getPublicKey();
        sk = new PartialSecretKey(partialKeyPair.getPartialSecretKey(), pk.getP());
        PartialPublicInfo partialInfo = new PartialPublicInfo(id, pk, partialKeyPair.getPartialPublicKey(), candidates, endTime);
        bulletinBoard.path("publicInfo").request()
                .post(Entity.entity(partialInfo, MediaType.APPLICATION_JSON));
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
        PartialPublicInfo pi = FetchingUtilities.fetchPublicInfo(logger, RSA_PUBLIC_KEY_NAME, bulletinBoard);
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

    public static class KeyGenParams implements KeyGenerationParameters {
        private final PrimePair pair;
        private final BigInteger generator;

        public KeyGenParams(BigInteger p, BigInteger q, BigInteger generator) {
            this.pair = new PrimePair(p, q);
            this.generator = generator;
        }

        @Override
        public PrimePair getPrimePair() {
            return pair;
        }

        @Override
        public BigInteger getGenerator() {
            return generator;
        }
    }
}
