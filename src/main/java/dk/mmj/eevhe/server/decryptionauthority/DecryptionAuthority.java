package dk.mmj.eevhe.server.decryptionauthority;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.Configuration;
import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.AbstractServer;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.signers.RSADigestSigner;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.util.encoders.Base64;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.client.JerseyWebTarget;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;

public class DecryptionAuthority extends AbstractServer {
    private static final Logger logger = LogManager.getLogger(DecryptionAuthority.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final JerseyWebTarget bulletinBoard;
    private boolean timeCorrupt = false;
    private PartialSecretKey sk;
    private int port = 8081;
    private long endTime;
    private PublicKey pk;
    private Integer id;

    public DecryptionAuthority(DecryptionAuthorityConfiguration configuration) {
        if (configuration.port != null) {
            port = configuration.port;
        }

        if (configuration.timeCorrupt > 0) {
            timeCorrupt = true;
        }

        bulletinBoard = configureWebTarget(logger, configuration.bulletinBoard);

        File conf = new File(configuration.confPath);
        if (!conf.exists() || !conf.isFile()) {
            logger.error("Configuration file either did not exists or were not a file. Path: " + conf.getAbsolutePath() + "\nTerminating");
            System.exit(-1);
        }


        try (FileInputStream ous = new FileInputStream(conf)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(ous));

            id = Integer.parseInt(reader.readLine());
            BigInteger secretValue = new BigInteger(reader.readLine());
            BigInteger p = new BigInteger(reader.readLine());
            String publicKeyString = reader.readLine();
            String endTimeString = reader.readLine();

            pk = new ObjectMapper().readValue(publicKeyString, PublicKey.class);


            sk = new PartialSecretKey(secretValue, p);
            endTime = Long.parseLong(endTimeString);
            long relativeEndTime = endTime - new Date().getTime();

            if (timeCorrupt) {
                relativeEndTime -= configuration.timeCorrupt; //30 sec.
            }

            scheduler.schedule(this::terminateVoting, relativeEndTime, TimeUnit.MILLISECONDS);

        } catch (JsonProcessingException e) {
            logger.error("Unable to deserialize public key. Terminating", e);
            System.exit(-1);
        } catch (FileNotFoundException e) {
            logger.error("Configuration file not found. Terminating", e);
            System.exit(-1);
        } catch (IOException e) {
            logger.error("Unable to read configuration file. Terminating", e);
            System.exit(-1);
        }
    }

    /**
     * TODO: Find better solution than duplicate code
     *
     * @return
     */
    protected PublicInformationEntity fetchPublicInfo() {
        Response response = bulletinBoard.path("getPublicInfo").request().buildGet().invoke();
        String responseString = response.readEntity(String.class);

        PublicInfoList publicInfoList;
        try {
            publicInfoList = new ObjectMapper().readValue(responseString, PublicInfoList.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize public informations list retrieved from bulletin board", e);
            System.exit(-1);
            return null;//Never happens
        }

        Optional<PublicInformationEntity> any = publicInfoList.getInformationEntities().stream()
//                .filter(this::verifyPublicInformation)//TODO!
                .findAny();

        if (!any.isPresent()) {
            logger.error("No public information retrieved from the server was signed by the trusted dealer. Terminating");
            System.exit(-1);
            return null;//Never happens
        }

        return any.get();
    }

    private void terminateVoting() {
        Long bulletinBoardTime = new Long(bulletinBoard.path("getCurrentTime").request().get(String.class));
        PublicInformationEntity pi = fetchPublicInfo();
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

        Entity<PartialResultList> resultEntity = Entity.entity(new PartialResultList(partialResults), MediaType.APPLICATION_JSON);
        Response post = bulletinBoard.path("result").request().post(resultEntity);

        if (post.getStatus() < 200 || post.getStatus() > 300) {
            logger.error("Unable to post result to bulletinBoard, got response:" + post);
            System.exit(-1);
        } else {
            logger.info("Successfully transferred partial decryption to bulletin board");
        }

    }

    @Deprecated
    private ArrayList<PersistedVote> getVotes() {
        try {
            String getVotes = bulletinBoard.path("getVotes").request().get(String.class);
            VoteList voteObjects = new ObjectMapper().readValue(getVotes, VoteList.class);
            ArrayList<PersistedVote> votes = new ArrayList<>();

            for (Object vote : voteObjects.getVotes()) {
                if (vote instanceof PersistedVote) {
                    votes.add((PersistedVote) vote);
                } else {
                    logger.error("Found vote that was not ciphertext. Was " + vote.getClass() + ". Terminating server");
                    terminate();
                }
            }
            return votes;
        } catch (IOException e) {
            logger.error("Failed to read VoteList from JSON string", e);
            return null;
        }
    }

    /**
     * Retrieves cast ballots from BulletinBoard
     *
     * @return list of ballots from BulletinBoard
     */
    private List<PersistedBallot> getBallots() {
        try {
            String getVotes = bulletinBoard.path("getVotes").request().get(String.class);
            BallotList voteObjects = new ObjectMapper().readValue(getVotes, BallotList.class);
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
            logger.error("Failed to read VoteList from JSON string", e);
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

        DecryptionAuthorityConfiguration(Integer port, String bulletinBoard, String confPath, int timeCorrupt) {
            this.port = port;
            this.bulletinBoard = bulletinBoard;
            this.confPath = confPath;
            this.timeCorrupt = timeCorrupt;
        }
    }
}
