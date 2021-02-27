package dk.mmj.eevhe.client;

import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.entities.BallotDTO;
import dk.mmj.eevhe.entities.Candidate;
import dk.mmj.eevhe.entities.PublicKey;
import dk.mmj.eevhe.entities.CandidateVoteDTO;
import dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthorityConfigBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Voter extends Client {
    private static final Logger logger = LogManager.getLogger(DecryptionAuthorityConfigBuilder.class);
    private final Integer multi;
    private String id;
    private Integer vote;


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
     * Fetches the public key from the public server, and casts vote.
     * <br>
     * if <code>multi</code> is set it casts <code>multi</code> random votes, for testing purposes.
     * <br>
     * Otherwise just casts a single, specified vote.
     */
    @Override
    public void run() {
        assertBulletinBoard();

        PublicKey publicKey = getPublicKey();
        if (multi != null) {
            doMultiVote(publicKey);
        } else {
            int vote = getVote();
            doVote(vote, publicKey);
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
        int[] castVotes = new int[size];
        Arrays.fill(castVotes, 0);

        for (int i = 0; i < multi; i++) {
            System.out.print("Dispatching votes: " + i + "/" + multi + " \r");

            id = UUID.randomUUID().toString();
            int vote = random.nextInt(size);
            castVotes[vote]++;

            doVote(vote, publicKey);
        }
        System.out.println("Dispatched " + multi + " votes with distribution: " + Arrays.toString(castVotes));
    }

    /**
     * Encrypts the vote under the public key, and casts the encrypted vote.
     *
     * @param publicKey is the public key used to encrypt the vote.
     * @param vote      is the vote to be cast, either 0 or 1.
     */
    private void doVote(int vote, PublicKey publicKey) {
        int candidateCount = getCandidates().size();

        BallotDTO ballot = SecurityUtils.generateBallot(vote, candidateCount, id, publicKey);
        postBallot(ballot);
    }

    /**
     * Makes sure that the {@link javax.ws.rs.client.WebTarget} is a public-server, and that it is live.
     * <br/>
     * Throws a {@link RuntimeException} if this is not the case.
     */
    private void assertBulletinBoard() {
        Response publicServerResp = target.path("type").request().buildGet().invoke();

        if (publicServerResp.getStatus() != 200) {
            logger.error("Couldn't connect to the bulletinBoard.");
            throw new RuntimeException("Failed : HTTP error code : " + publicServerResp.getStatus());
        }

        String responseEntity = publicServerResp.readEntity(String.class);

        if (!responseEntity.contains("Bulletin Board")) {
            throw new RuntimeException("Server was not of type bulletinBoard");
        }
    }

    /**
     * Posts the encrypted vote to the public server, using the "/vote" path.
     *
     * @param ballot the ballot with vote encrypted under the public key, and zero knowledge proofs.
     */
    private void postBallot(BallotDTO ballot) {
        Entity<?> entity = Entity.entity(ballot, MediaType.APPLICATION_JSON_TYPE);
        Response response = target.path("postBallot").request().post(entity);

        if (response.getStatus() != 204) {
            logger.warn("Failed to post vote to server: Error code was " + response.getStatus());
        }
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
            } catch (IOException ignored) {
                System.out.println("Unable to read vote - terminating");
                System.exit(-1);
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
         * @param targetUrl url for bulletin board to post vote(s) to
         * @param id        voter id
         * @param vote      what to vote. True is pro while False is against
         * @param multi     if different from null, multiple random votes are dispatched
         */
        VoterConfiguration(String targetUrl, String id, Integer vote, Integer multi) {
            super(Voter.class,targetUrl);
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
