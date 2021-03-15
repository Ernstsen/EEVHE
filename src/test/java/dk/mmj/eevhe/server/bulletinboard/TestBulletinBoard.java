package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.client.SSLHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.*;

public class TestBulletinBoard {
    private static final Logger logger = LogManager.getLogger(TestBulletinBoard.class);
    private static final int port = 4894;
    private BulletinBoard bulletinBoard;

    @Before
    public void setUp() throws Exception {
        BulletinBoard.BulletinBoardConfiguration config = new BulletinBoard.BulletinBoardConfiguration(port);
        bulletinBoard = new BulletinBoard(config);
    }

    @Test
    public void getPort() {
        assertEquals("did not use supplied port", port, bulletinBoard.getPort());
    }

    @Test
    public void serverTypeAndTime() throws InterruptedException {
        Thread thread = new Thread(bulletinBoard);
        thread.start();
        Thread.sleep(2_000);

        JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);
        String type = target.path("type").request().get(String.class);
        assertEquals("Wrong type string returned", "<b>ServerType:</b> Bulletin Board", type);

        Long time = target.path("getCurrentTime").request().get(Long.class);
        long now = new Date().getTime();
        assertTrue("Time should have passed since fetching time there: " + time + ", now:" + now, time <= now);


        bulletinBoard.terminate();
        thread.join();
    }

    @Test
    public void postAndRetrieve() throws InterruptedException, IOException {
        Thread thread = new Thread(bulletinBoard);
        thread.start();
        Thread.sleep(2_000);

        JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);

        ServerState.getInstance().reset();
        //Assert we get 404 when items are not found
        assertEquals("Expected 404", 404, target.path("publicKey").request().get().getStatus());
        assertEquals("Expected 404", 404, target.path("getPublicInfo").request().get().getStatus());
        assertEquals("Expected 404", 404, target.path("getBallots").request().get().getStatus());
        assertEquals("Expected 404", 404, target.path("commitments").request().get().getStatus());

        //POST values
        PublicKey pk = new PublicKey(valueOf(864466), valueOf(681136), valueOf(89134));

        CipherText c = new CipherText(valueOf(165), valueOf(684983));
        Proof p1 = new Proof(valueOf(64986), valueOf(859483), valueOf(92873452), valueOf(293885671));
        CipherText c2 = new CipherText(valueOf(1652), valueOf(68498));
        Proof p2 = new Proof(valueOf(4986), valueOf(8359483), valueOf(873452), valueOf(885671));
        List<CandidateVoteDTO> candidates = Arrays.asList(new CandidateVoteDTO(c, "id", p1), new CandidateVoteDTO(c2, "id2", p2));
        Proof p3 = new Proof(valueOf(486), valueOf(359483), valueOf(73452), valueOf(85671));

        BallotDTO ballotDTO = new BallotDTO(candidates, "id", p3);

        DLogProofUtils.Proof dp1 = new DLogProofUtils.Proof(valueOf(54), valueOf(9846));
        DLogProofUtils.Proof dp2 = new DLogProofUtils.Proof(valueOf(62968), valueOf(613658874));

        AsymmetricKeyParameter sk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
        String cert = new String(Files.readAllBytes(Paths.get("certs/test_glob_key.pem")));

        SignedEntity<PartialResultList> partialResultList = new SignedEntity<>(new PartialResultList(
                Arrays.asList(
                        new PartialResult(1, valueOf(234), dp1, c),
                        new PartialResult(2, valueOf(6854), dp2, c2)),
                58,
                18
        ), sk);

        SignedEntity<CommitmentDTO> commitmentDTO = new SignedEntity<>(new CommitmentDTO(new BigInteger[]{valueOf(584), valueOf(56498), valueOf(650)}, 1, "BAR"), sk);

        SignedEntity<PedersenComplaintDTO> pedersenComplaint = new SignedEntity<>(new PedersenComplaintDTO(1, 2), sk);
        SignedEntity<FeldmanComplaintDTO> feldmanComplaint = new SignedEntity<>(new FeldmanComplaintDTO(1, 2, valueOf(123), valueOf(123)), sk);
        SignedEntity<ComplaintResolveDTO> resolve = new SignedEntity<>(new ComplaintResolveDTO(1, 2, new PartialSecretMessageDTO(valueOf(564), valueOf(568), 2, 1)), sk);

        SignedEntity<PartialPublicInfo> partialPublicInfo = new SignedEntity<>(new PartialPublicInfo(
                1, pk, valueOf(124121),
                Arrays.asList(new Candidate(1, "name", "desc"), new Candidate(2, "name2", "desc2")),
                6584198494L, cert
        ), sk);


        //Do POSTs
        String mediaType = MediaType.APPLICATION_JSON;
        assertEquals("should be successful post", 204,
                target.path("postBallot").request().post(Entity.entity(ballotDTO, mediaType)).getStatus()
        );
        assertEquals("should be disallowed", 405,
                target.path("postBallot").request().post(Entity.entity(ballotDTO, mediaType)).getStatus()
        );
        assertEquals("should be successful post", 204,
                target.path("result").request().post(Entity.entity(partialResultList, mediaType)).getStatus()
        );
        assertEquals("should be successful post", 204,
                target.path("commitments").request().post(Entity.entity(commitmentDTO, mediaType)).getStatus()
        );
        assertEquals("should be successful post", 204,
                target.path("pedersenComplain").request().post(Entity.entity(pedersenComplaint, mediaType)).getStatus()
        );
        assertEquals("should be successful post", 204,
                target.path("feldmanComplain").request().post(Entity.entity(feldmanComplaint, mediaType)).getStatus()
        );
        assertEquals("should be successful post", 204,
                target.path("resolveComplaint").request().post(Entity.entity(resolve, mediaType)).getStatus()
        );
        assertEquals("should be successful post", 204,
                target.path("publicInfo").request().post(Entity.entity(partialPublicInfo, mediaType)).getStatus()
        );

        //Do gets and compares
        ObjectMapper mapper = new ObjectMapper();

        String publicInfoString = target.path("getPublicInfo").request()
                .get(String.class);
        List<SignedEntity<PartialPublicInfo>> fetchedPublicInfos = mapper.readValue(publicInfoString, new TypeReference<List<SignedEntity<PartialPublicInfo>>>() {
        });
        assertEquals("Unexpected list size", 1, fetchedPublicInfos.size());
        assertEquals("Fetched publicInfo did not match posted one", partialPublicInfo, fetchedPublicInfos.get(0));

        ResultList fetchedResultList = target.path("result").request()
                .get(new GenericType<>(ResultList.class));
        assertEquals("Unexpected list size", 1, fetchedResultList.getResults().size());
        assertEquals("Fetched result did not match posted one", partialResultList, fetchedResultList.getResults().get(0));

        BallotList fetchedBallotList = target.path("getBallots").request()
                .get(new GenericType<>(BallotList.class));
        assertEquals("Unexpected list size", 1, fetchedBallotList.getBallots().size());
        PersistedBallot persistedBallot = fetchedBallotList.getBallots().get(0);
        assertEquals("Fetched ballot did not match posted one; votes", ballotDTO.getCandidateVotes(), persistedBallot.getCandidateVotes());
        assertEquals("Fetched ballot did not match posted one; id", ballotDTO.getId(), persistedBallot.getId());
        assertEquals("Fetched ballot did not match posted one; proof", ballotDTO.getSumIsOneProof(), persistedBallot.getSumIsOneProof());
        assertNotNull("Fetched ballot had no timestamp", persistedBallot.getTs());

        String commitmentsString = target.path("commitments").request()
                .get(String.class);
        List<SignedEntity<CommitmentDTO>> fetchedCommitmentList = mapper.readValue(commitmentsString, new TypeReference<List<SignedEntity<CommitmentDTO>>>() {
        });
        assertEquals("Unexpected list size", 1, fetchedCommitmentList.size());
        assertEquals("Fetched commitment did not match posted one", commitmentDTO, fetchedCommitmentList.get(0));

        String complaintsString = target.path("pedersenComplaints").request()
                .get(String.class);
        List<SignedEntity<PedersenComplaintDTO>> fetchedComplaintList = mapper.readValue(complaintsString, new TypeReference<List<SignedEntity<PedersenComplaintDTO>>>() {
        });
        assertEquals("Unexpected list size", 1, fetchedComplaintList.size());
        assertEquals("Fetched complaint did not match posted one", pedersenComplaint, fetchedComplaintList.get(0));

        String feldmanComplaintsString = target.path("feldmanComplaints").request()
                .get(String.class);
        List<SignedEntity<FeldmanComplaintDTO>> feldmanFetchedComplaintList = mapper.readValue(feldmanComplaintsString, new TypeReference<List<SignedEntity<FeldmanComplaintDTO>>>() {
        });
        assertEquals("Unexpected list size", 1, feldmanFetchedComplaintList.size());
        assertEquals("Fetched complaint did not match posted one", feldmanComplaint, feldmanFetchedComplaintList.get(0));

        String complaintResolvesString = target.path("complaintResolves").request()
                .get(String.class);
        List<SignedEntity<ComplaintResolveDTO>> fetchedResolvesList = mapper.readValue(complaintResolvesString, new TypeReference<List<SignedEntity<ComplaintResolveDTO>>>() {
        });
        assertEquals("Unexpected list size", 1, fetchedResolvesList.size());
        assertEquals("Fetched resolve did not match posted one", resolve, fetchedResolvesList.get(0));


        String partialPublicInfoString = target.path("publicInfo").request()
                .get(String.class);
        List<SignedEntity<PartialPublicInfo>> fetchedPublicInfoList = mapper.readValue(partialPublicInfoString, new TypeReference<List<SignedEntity<PartialPublicInfo>>>() {
        });
        assertEquals("Unexpected list size", 1, fetchedPublicInfoList.size());
        assertEquals("Fetched partial public info did not match posted one", partialPublicInfo, fetchedPublicInfoList.get(0));

        bulletinBoard.terminate();
        thread.join();
    }
}
