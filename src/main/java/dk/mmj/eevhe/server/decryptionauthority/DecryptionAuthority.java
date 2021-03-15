package dk.mmj.eevhe.server.decryptionauthority;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.client.FetchingUtilities;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.CertificateProviderImpl;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.interfaces.Decrypter;
import dk.mmj.eevhe.protocols.GennaroDKG;
import dk.mmj.eevhe.protocols.connectors.BulletinBoardBroadcaster;
import dk.mmj.eevhe.protocols.connectors.RestPeerCommunicator;
import dk.mmj.eevhe.protocols.connectors.ServerStateIncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.DKG;
import dk.mmj.eevhe.server.AbstractServer;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.client.JerseyWebTarget;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;

public class DecryptionAuthority extends AbstractServer {
    private final Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final JerseyWebTarget bulletinBoard;
    private final int port;
    private final Integer id;
    private final KeyGenParams params;
    private final DecryptionAuthorityInput input;
    private final long endTime;
    private final List<Candidate> candidates;
    private String certString;
    private AsymmetricKeyParameter electionPk;
    private AsymmetricKeyParameter sk;
    private Decrypter decrypter;
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

        Path conf = Paths.get(configuration.confPath);
        if (!Files.exists(conf) || !Files.exists(conf)) {
            logger.error("Configuration folder either did not exists or were not a folder. Path: " + conf + "\n");
            terminate();
        }

        Path privateInput = conf.resolve("DA" + id + ".zip");
        logger.info("Reading private input from file: " + privateInput);
        try (ZipFile zipFile = new ZipFile(privateInput.toFile())) {
            ZipEntry skEntry = zipFile.getEntry("sk.pem");
            ZipEntry certEntry = zipFile.getEntry("cert.pem");

            try (InputStream is = zipFile.getInputStream(skEntry)) {
                sk = KeyHelper.readKey(IOUtils.toByteArray(is));
            }

            try (InputStream is = zipFile.getInputStream(certEntry)) {
                certString = new String(IOUtils.toByteArray(is));
            }

        } catch (IOException e) {
            logger.error("Error occurred while reading private input file from " + privateInput, e);
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            candidates = mapper.readValue(conf.resolve("candidates.json").toFile(), new TypeReference<List<Candidate>>() {
            });
        } catch (IOException e) {
            logger.error("Unable to read candidates from file: " + conf.resolve("candidates.json"));
            throw new RuntimeException("Failed to load file for candidates", e);
        }


        try {
            input = mapper.readValue(conf.resolve("common_input.json").toFile(), DecryptionAuthorityInput.class);

            BigInteger p = new BigInteger(Hex.decode(input.getpHex()));
            BigInteger g = new BigInteger(Hex.decode(input.getgHex()));
            BigInteger e = new BigInteger(Hex.decode(input.geteHex()));
            BigInteger q = p.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2));
            params = new KeyGenParams(p, q, g, e);
            endTime = input.getEndTime();
            long relativeEndTime = endTime - new Date().getTime();

            if (timeCorrupt) {
                relativeEndTime -= configuration.timeCorrupt; //30 sec.
            }

            if (input.getInfos().stream().anyMatch(i -> i.getId() == 0)) {
                logger.error("Found DA with id=0, which is illegal!. Terminating");
                terminate();
                return;
            }

            electionPk = CertificateHelper
                    .getPublicKeyFromCertificate(input.getEncodedElectionCertificate().getBytes(StandardCharsets.UTF_8));

            scheduler.schedule(this::terminateVoting, relativeEndTime, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            logger.error("Failed to read authorities input file", e);
            throw new RuntimeException("Failed to read DecryptionAuthorityInput from file", e);
        }
    }

    @Override
    protected void afterStart() {
        logger.info("Posting ");
        Entity<SignedEntity<CertificateDTO>> entity = Entity.entity(
                new SignedEntity<>(new CertificateDTO(certString, id), sk),
                MediaType.APPLICATION_JSON);
        bulletinBoard.path("certificates").request().post(entity);

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
                .collect(Collectors.toMap(DecryptionAuthorityInfo::getId, inf -> new RestPeerCommunicator(configureWebTarget(logger, inf.getAddress()), sk)));
        communicators.remove(id);//We remove ourself, to be able to iterate without
        CertificateProviderImpl certProvider = new CertificateProviderImpl(this::getCertificates, electionPk);
        final ServerStateIncomingChannel incoming = new ServerStateIncomingChannel(
                input.getInfos().stream()
                        .map(DecryptionAuthorityInfo::getId)
                        .map(this::partialSecretKey)
                        .collect(Collectors.toList()),
                certProvider
        );

        dkg = new GennaroDKG(new BulletinBoardBroadcaster(bulletinBoard, sk, certProvider),
                incoming, communicators, id, params, "ID:" + id);
        dkgSteps = dkg.getSteps().iterator();

        logger.info("scheduling verification of received values. DA with id=" + id);

        executeDKGStep();
    }

    /**
     * Fetches certificates
     *
     * @return list of signed certificates
     */
    private List<SignedEntity<CertificateDTO>> getCertificates() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String certificates = bulletinBoard.path("certificates").request().get(String.class);
            return mapper.readValue(certificates, new TypeReference<List<SignedEntity<CertificateDTO>>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to fetch certificates", e);
        }
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
            PartialPublicInfo ppi = new PartialPublicInfo(
                    id, keyPair.getPublicKey(),
                    keyPair.getPartialPublicKey(),
                    candidates, endTime, certString
            );

            decrypter = new DecrypterImpl(id,
                    () -> FetchingUtilities.getBallots(logger, bulletinBoard),
                    b -> VoteProofUtils.verifyBallot(b, keyPair.getPublicKey()),
                    candidates
            );

            Response resp = bulletinBoard.path("publicInfo").request().post(Entity.entity(new SignedEntity<>(ppi, sk), MediaType.APPLICATION_JSON));
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
        if (res == null) {
            logger.info("No result to post");
            return;
        }

        logger.info("Posting to bulletin board");
        Entity<SignedEntity<PartialResultList>> resultEntity = Entity.entity(new SignedEntity<>(res, sk), MediaType.APPLICATION_JSON);
        Response post = bulletinBoard.path("result").request().post(resultEntity);

        if (post.getStatus() < 200 || post.getStatus() > 300) {
            logger.error("Unable to post result to bulletinBoard, got response:" + post);
            System.exit(-1);
        } else {
            logger.info("Successfully transferred partial decryption to bulletin board");
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
        private final BigInteger groupElement;

        public KeyGenParams(BigInteger p, BigInteger q, BigInteger generator, BigInteger groupElement) {
            this.pair = new PrimePair(p, q);
            this.generator = generator;
            this.groupElement = groupElement;
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
            return groupElement;
        }
    }
}
