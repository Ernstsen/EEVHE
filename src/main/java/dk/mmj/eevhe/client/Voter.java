package dk.mmj.eevhe.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.entities.BallotDTO;
import dk.mmj.eevhe.entities.Candidate;
import dk.mmj.eevhe.entities.PublicKey;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthorityConfigBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static dk.mmj.eevhe.client.FetchingUtilities.verifySignedAndValid;

public class Voter extends Client {
    private static final Logger logger = LogManager.getLogger(DecryptionAuthorityConfigBuilder.class);
    private final Integer multi;
    private String id;
    private Integer vote;
    private int[] castVotes;


    /**
     * Creates a client instance, that utilizes the SSL protocol to communicate with the public server.
     *
     * @param configuration the VoterConfiguration built in the same class.
     */
    public Voter(VoterConfiguration configuration) {
        super(configuration);
        id = configuration.id;
        vote = configuration.vote;
        multi = configuration.multi;
    }

    /**
     * Constructor for using voter as part of the system, such as a GUI
     *
     * @param targetUrl url for the Bulletin Board
     * @param id        voter ID
     */
    @SuppressWarnings("unused")//For GUI impl.
    public Voter(String targetUrl, String id) {
        super(new VoterConfiguration(targetUrl, id, null, null, Paths.get("certs/test_glob.pem")));
        this.id = id;
        this.vote = null;
        multi = null;
    }

    /**
     * @param vote index of the candidate to vote for
     * @return whether the vote was successful
     */
    public boolean vote(Integer vote) {
        return doVote(vote, getPublicKey());
    }

    /**
     * Fetches the public key from the public server, and casts vote.
     * <br>
     * if <code>multi</code> is set it casts <code>multi</code> random votes, for testing purposes.
     * <br>
     * Otherwise just casts a single, specified vote.
     */
    @Override
    public void run() {
        assertBulletinBoard();

        if (multi != null) {
            doMultiVote(getPublicKey());
        } else {
            vote(getVote());
        }
    }

    /**
     * Casts <code>multi</code> random votes, for testing purposes.
     *
     * @param publicKey is the public key used to encrypt the vote.
     */
    private void doMultiVote(PublicKey publicKey) {
        int size = getCandidates().size();
        Random random = new Random();
        castVotes = new int[size];
        Arrays.fill(castVotes, 0);

        for (int i = 0; multi != null && i < multi; i++) {
            System.out.print("Dispatching votes: " + i + "/" + multi + " \r");

            id = UUID.randomUUID().toString();
            int vote = random.nextInt(size);

            if (doVote(vote, publicKey)) {
                castVotes[vote]++;
            }

        }
        System.out.println("Dispatched " + multi + " votes with distribution: " + Arrays.toString(castVotes));
    }

    /**
     * @return array of cast votes for each candidate
     */
    public int[] getCastVotes() {
        return castVotes;
    }

    /**
     * Encrypts the vote under the public key, and casts the encrypted vote.
     *
     * @param publicKey is the public key used to encrypt the vote.
     * @param vote      is the vote to be cast, either 0 or 1.
     * @return whether vote was successfully cast
     */
    private boolean doVote(int vote, PublicKey publicKey) {
        int candidateCount = getCandidates().size();

        BallotDTO ballot = SecurityUtils.generateBallot(vote, candidateCount, id, publicKey);
        return postBallot(ballot);
    }

    /**
     * Makes sure that the {@link javax.ws.rs.client.WebTarget} is a public-server, and that it is live.
     * <br/>
     * Throws a {@link RuntimeException} if this is not the case.
     */
    private void assertBulletinBoard() {
        if (!checkBulletinBoard()) {
            throw new RuntimeException("Server was not of type bulletinBoard");
        }
    }

    /**
     * @return whether the client points at a bulletin board
     */
    public boolean checkBulletinBoard() {
        Response publicServerResp = target.path("type").request().buildGet().invoke();

        if (publicServerResp.getStatus() != 200) {
            logger.error("Couldn't connect to the bulletinBoard.");
            throw new RuntimeException("Failed : HTTP error code : " + publicServerResp.getStatus());
        }

        String responseEntity = publicServerResp.readEntity(String.class);
        return responseEntity.contains("Bulletin Board");
    }

    /**
     * Posts the encrypted vote to the public server, using the "/vote" path.
     *
     * @param ballot the ballot with vote encrypted under the public key, and zero knowledge proofs.
     * @return whether post was a success
     */
    private boolean postBallot(BallotDTO ballot) {
        Entity<?> entity = Entity.entity(ballot, MediaType.APPLICATION_JSON_TYPE);
        Response response = target.path("postBallot").request().post(entity);

        if (response.getStatus() != 204) {
            logger.warn("Failed to post vote to server: Error code was " + response.getStatus());
            return false;
        }

        return verifyPosted();
    }

    private boolean verifyPosted() {
        List<X509CertificateHolder> certs = getBBPeerCertificates();
        int peers = certs.size();

        int retries = 0;
        do {
            try {
                Thread.sleep(1_000);

                String fetchedBallot = target.path("getBallot")
                        .path(id)
                        .request().get(String.class);

                List<SignedEntity<BallotDTO>> ballots = new ObjectMapper()
                        .readValue(fetchedBallot, new TypeReference<List<SignedEntity<BallotDTO>>>() {
                        });

                List<SignedEntity<BallotDTO>> validBallotSignatures = verifySignedAndValid(ballots, certs, logger);

                int threshold = (int) Math.ceil(((float) peers) / 2);
                System.out.print("Received vote confirmation from " + validBallotSignatures.size() + "/" + threshold + " peers \r");
                if (validBallotSignatures.size() >= threshold) {
                    return true;
                }
            } catch (JsonProcessingException | InterruptedException e) {
                logger.error("Failed to parse fetched ballots", e);
                return false;
            }
        } while (retries++ < 10);

        return false;
    }

    /**
     * Retrieves vote to be cast
     * <br/>
     * If none is defined, terminal is prompted for one
     * <br/>
     * Valid inputs for the vote is either "true" or "false".
     *
     * @return 1 or 0 according to input.
     */
    private int getVote() {
        List<Candidate> candidates = getCandidates();
        if (vote == null || vote >= candidates.size()) {
            System.out.println("Please enter vote to be cast. Candidates:");
            System.out.println("--------------------------------------------");
            for (Candidate candidate : candidates) {
                System.out.println(candidate.getIdx() + ": " + candidate.getName());
                System.out.println(candidate.getDescription());
                System.out.println("\n");
            }
            System.out.println("--------------------------------------------");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            try {
                String s = reader.readLine();
                vote = Integer.parseInt(s);
                System.out.println("voting: " + vote);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read vote", e);
            }
        }

        return vote;
    }


    /**
     * The Voter Configuration.
     * <br>
     * Built in the {@link ClientConfigBuilder}.
     */
    public static class VoterConfiguration extends ClientConfiguration<Voter> {
        private final String id;
        private final Integer vote;
        private final Integer multi;

        /**
         * @param targetUrl        url for bulletin board to post vote(s) to
         * @param id               voter id
         * @param vote             what to vote. True is pro while False is against
         * @param multi            if different from null, multiple random votes are dispatched
         * @param electionCertPath path to global election certificate
         */
        VoterConfiguration(String targetUrl, String id, Integer vote, Integer multi, Path electionCertPath) {
            super(Voter.class, targetUrl, electionCertPath);
            this.id = id;
            this.vote = vote;
            this.multi = multi;
        }

        String getId() {
            return id;
        }

        Integer getVote() {
            return vote;
        }

        Integer getMulti() {
            return multi;
        }
    }
}
