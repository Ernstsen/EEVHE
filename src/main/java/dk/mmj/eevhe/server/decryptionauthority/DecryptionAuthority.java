package dk.mmj.eevhe.server.decryptionauthority;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.GennaroDKG;
import dk.mmj.eevhe.protocols.connectors.BulletinBoardBroadcaster;
import dk.mmj.eevhe.protocols.connectors.RestPeerCommunicator;
import dk.mmj.eevhe.protocols.connectors.ServerStateIncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.DKG;
import dk.mmj.eevhe.server.AbstractServer;
import dk.mmj.eevhe.server.decryptionauthority.interfaces.Decrypter;
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

public class DecryptionAuthority extends AbstractServer {
    private static final String RSA_PUBLIC_KEY_NAME = "rsa";
    private final Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final JerseyWebTarget bulletinBoard;
    private final int port;
    private final Integer id;
    private final ObjectMapper mapper = new ObjectMapper();
    private Decrypter decrypter;
    private final KeyGenParams params;
    private final DecryptionAuthorityInput input;
    private final long endTime;
    private final List<Candidate> candidates;
    private DKG<PartialKeyPair> dkg;
    private boolean timeCorrupt = false;
    private Iterator<DKG.Step> dkgSteps;
    private PartialKeyPair keyPair;

    public DecryptionAuthority(DecryptionAuthorityConfiguration configuration) {
        logger = LogManager.getLogger(DecryptionAuthority.class + " " + configuration.id + ":");
        port = configuration.port;
        id = configuration.id;

        if (configuration.timeCorrupt > 0) {
            timeCorrupt = true;
        }

        bulletinBoard = configureWebTarget(logger, configuration.bulletinBoard);
        try {
            candidates = mapper.readValue(new File("conf/testing_candidates.json"), new TypeReference<List<Candidate>>() {
            });
        } catch (IOException e) {
            logger.error("You moved the file, and are yet to do this properly!");
            throw new RuntimeException("Failed to load file for candidates", e);
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
            throw new RuntimeException("Failed to read DecryptionAuthorityInput from file", e);
        }
    }

    @Override
    protected void afterStart() {
        logger.info("Starting keyGeneration protocol for DA id=" + id);
        scheduler.execute(this::scheduleDKG);
    }

    /**
     * Initializes the key-generation protocol by sending polynomial to the other peers,
     * and awaiting input from the other authorities
     */
    private void scheduleDKG() {
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

        dkg = new GennaroDKG(new BulletinBoardBroadcaster(bulletinBoard),
                incoming, communicators, id, params, "ID:" + id);
        dkgSteps = dkg.getSteps().iterator();

        logger.info("scheduling verification of received values. DA with id=" + id);

        executeDKGStep();
    }

    /**
     * Step execution logic - schedules step, and extracts output when DKG is finished
     */
    private void executeDKGStep() {
        if (dkgSteps.hasNext()) {
            DKG.Step step = dkgSteps.next();
            scheduler.schedule(stepWrapper(step), step.getDelay(), step.getTimeUnit());
        } else {
            logger.info("Retrieving keys from DKG and sending to BulletinBoard");
            keyPair = dkg.output();
            PartialPublicInfo ppi = new PartialPublicInfo(id, keyPair.getPublicKey(), keyPair.getPartialPublicKey(), candidates, endTime);

            decrypter = new DecrypterIml(id,
                    this::getBallots,
                    b -> VoteProofUtils.verifyBallot(b, keyPair.getPublicKey()),
                    candidates
            );

            Response resp = bulletinBoard.path("publicInfo").request().post(Entity.entity(ppi, MediaType.APPLICATION_JSON));
            if (resp.getStatus() != 204) {
                logger.error("Failed to post public info to bulletinBoard! Status was: " + resp.getStatus());
            }
        }
    }

    /**
     * Runs step and then schedules next
     *
     * @param step step to execute
     */
    private Runnable stepWrapper(final DKG.Step step) {
        return () -> {
            step.getExecutable().run();
            executeDKGStep();
        };
    }

    private String partialSecretKey(Integer id) {
        return this.id + "secret:" + id;
    }

    private void terminateVoting() {
        Long bulletinBoardTime = new Long(bulletinBoard.path("getCurrentTime").request().get(String.class));

        long remainingTime = endTime - bulletinBoardTime;

        if (!timeCorrupt && remainingTime > 0) {
            logger.info("Attempted to collect votes from BB, but voting not finished. Retrying in " + (remainingTime / 1000) + "s");
            scheduler.schedule(this::terminateVoting, remainingTime, TimeUnit.MILLISECONDS);
            return;
        }

        PartialResultList res = decrypter.generatePartialResult(endTime, keyPair);

        logger.info("Posting to bulletin board");
        Entity<PartialResultList> resultEntity = Entity.entity(res, MediaType.APPLICATION_JSON);
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
    public static class DecryptionAuthorityConfiguration extends AbstractInstanceCreatingConfiguration<DecryptionAuthority> {
        private final int port;
        private final String bulletinBoard;
        private final String confPath;
        private final int timeCorrupt;
        private final int id;

        DecryptionAuthorityConfiguration(int port, String bulletinBoard, String confPath, int id, int timeCorrupt) {
            super(DecryptionAuthority.class);
            this.port = port;
            this.bulletinBoard = bulletinBoard;
            this.id = id;
            this.confPath = confPath;
            this.timeCorrupt = timeCorrupt;
        }

        public int getPort() {
            return port;
        }

        public String getBulletinBoard() {
            return bulletinBoard;
        }

        public String getConfPath() {
            return confPath;
        }

        public int getTimeCorrupt() {
            return timeCorrupt;
        }

        public int getId() {
            return id;
        }

    }

    public static class KeyGenParams implements ExtendedKeyGenerationParameters {
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

        @Override
        public BigInteger getGroupElement() {
            // TODO: Implement this correctly. Read from file..
            return BigInteger.ONE;
        }
    }
}
