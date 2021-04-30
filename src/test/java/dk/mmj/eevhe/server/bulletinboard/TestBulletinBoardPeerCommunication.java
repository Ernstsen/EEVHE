package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.client.SSLHelper;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.TestUtils;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.ServerState;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.assertEquals;

public class TestBulletinBoardPeerCommunication {
    private static final Logger logger = LogManager.getLogger(TestBulletinBoardPeerCommunication.class);
    private static final List<Integer> ids = Arrays.asList(1, 2, 3, 4);
    private final List<File> files = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Integer, BulletinBoardPeer> bulletinBoardPeers = new HashMap<>();
    private final Map<Integer, JerseyWebTarget> targets = new HashMap<>();
    private String confPath;
    private ArrayList<Thread> threads;
    private AsymmetricKeyParameter secretKey;
    private String cert;

    private void buildTempFiles() throws IOException {
        File folder = new File(confPath);

        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();

        File common = new File(folder, "BB_input.json");

//        TODO: Individual certificates
        String certString = new String(Files.readAllBytes(Paths.get("certs/test_glob.pem")));

        BBInput input = new BBInput(
                ids.stream().map(id -> new BBPeerInfo(id, "https://localhost:1808" + id, certString))
                        .collect(Collectors.toList()),
                new ArrayList<>());

        mapper.writeValue(common, input);
        files.add(common);

        for (int id : ids) {
            File zip = new File(folder, "BB_Peer" + id + ".zip");
            try (ZipOutputStream ous = new ZipOutputStream(new FileOutputStream(zip))) {
                ous.putNextEntry(new ZipEntry("sk.pem"));
                IOUtils.copy(Files.newInputStream(Paths.get("certs/test_glob_key.pem")), ous);
            }

            files.add(zip);
        }

        files.add(folder);
    }

    @Before
    public void setUp() throws Exception {
        confPath = "temp_conf/";

        ServerState.getInstance().reset();

        buildTempFiles();

        secretKey = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
        cert = new String(Files.readAllBytes(Paths.get("certs/test_glob_key.pem")));

        for (int id : ids) {
            Integer port = new Integer("1808" + id);

            BulletinBoardPeer.BulletinBoardPeerConfiguration config
                    = new BulletinBoardPeer.BulletinBoardPeerConfiguration(
                    port,
                    confPath,
                    id);

            BulletinBoardPeer bulletinBoardPeer = new BulletinBoardPeer(config);
            bulletinBoardPeers.put(id, bulletinBoardPeer);

            JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);
            targets.put(id, target);
        }

        threads = new ArrayList<>();

        for (BulletinBoardPeer bulletinBoardPeer : bulletinBoardPeers.values()) {
            Thread thread = new Thread(bulletinBoardPeer);
            thread.start();
            threads.add(thread);
        }
        Thread.sleep(2_000);
    }

    private void testPostSingleBallot(List<Integer> postTo) throws InterruptedException, JsonProcessingException {
        PublicKey publicKey = TestUtils.generateKeysFromP2048bitsG2().getPublicKey();
        BallotDTO ballotDTO = SecurityUtils.generateBallot(1, 3, "voter_id", publicKey);

        for (Integer id : postTo) {
            targets.get(id).path("postBallot").request().post(Entity.entity(ballotDTO, MediaType.APPLICATION_JSON));
        }

        // Allow consensus protocol to be run
        Thread.sleep(4_000);

        List<PersistedBallot> lastSeenBallotList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String ballotsString = target.path("getBallots").request().get(String.class);
            List<PersistedBallot> fetchedBallotList = mapper.readValue(ballotsString, new TypeReference<List<PersistedBallot>>() {
            });

            if (!lastSeenBallotList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on ballot lists", lastSeenBallotList, fetchedBallotList);
            }

            assertEquals("Ballot list should be of size 1", 1, fetchedBallotList.size());

            lastSeenBallotList = fetchedBallotList;
        }
    }

    @Test
    public void shouldAgreeOnSingleBallotWhenOnePeerReceivesFromEdge() throws InterruptedException, JsonProcessingException {
        testPostSingleBallot(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSingleBallotWhenTwoPeersReceivesFromEdge() throws InterruptedException, JsonProcessingException {
        testPostSingleBallot(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSingleBallotWhenThreePeersReceivesFromEdge() throws InterruptedException, JsonProcessingException {
        testPostSingleBallot(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSingleBallotWhenFourPeersReceivesFromEdge() throws InterruptedException, JsonProcessingException {
        testPostSingleBallot(Arrays.asList(1, 2, 3, 4));
    }

    private void testPostSinglePublicInfo(List<Integer> postTo) throws InterruptedException, IOException {
        PublicKey publicKey = TestUtils.generateKeysFromP2048bitsG2().getPublicKey();

        SignedEntity<PartialPublicInfo> partialPublicInfo = new SignedEntity<PartialPublicInfo>(new PartialPublicInfo(
                1, publicKey, valueOf(124121),
                Arrays.asList(new Candidate(1, "name", "desc"), new Candidate(2, "name2", "desc2")),
                6584198494L, cert
        ), secretKey);

        for (Integer id : postTo) {
            targets.get(id).path("publicInfo").request().post(Entity.entity(partialPublicInfo, MediaType.APPLICATION_JSON));
        }

        // Allow consensus protocol to be run
        Thread.sleep(4_000);

        List<SignedEntity<PartialPublicInfo>> lastSeenPartialInfoList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String publicInfoString = target.path("getPublicInfo").request()
                    .get(String.class);
            List<SignedEntity<PartialPublicInfo>> fetchedPublicInfoList
                    = mapper.readValue(publicInfoString, new TypeReference<List<SignedEntity<PartialPublicInfo>>>() {
            });

            if (!lastSeenPartialInfoList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on partial public info lists", lastSeenPartialInfoList, fetchedPublicInfoList);
            }

            assertEquals("Partial public info list should be of size 1", 1, fetchedPublicInfoList.size());

            lastSeenPartialInfoList = fetchedPublicInfoList;
        }
    }

    @Test
    public void shouldAgreeOnSinglePublicInfoWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        testPostSinglePublicInfo(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSinglePublicInfoWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSinglePublicInfo(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSinglePublicInfoWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSinglePublicInfo(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSinglePublicInfoWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSinglePublicInfo(Arrays.asList(1, 2, 3, 4));
    }

    private void testPostSingleResult(List<Integer> postTo) throws InterruptedException, IOException {
        CipherText c = new CipherText(valueOf(165), valueOf(684983));
        CipherText c2 = new CipherText(valueOf(1652), valueOf(68498));
        DLogProofUtils.Proof dp1 = new DLogProofUtils.Proof(valueOf(54), valueOf(9846));
        DLogProofUtils.Proof dp2 = new DLogProofUtils.Proof(valueOf(62968), valueOf(613658874));

        SignedEntity<PartialResultList> partialResultList = new SignedEntity<>(new PartialResultList(
                Arrays.asList(
                        new PartialResult(1, valueOf(234), dp1, c),
                        new PartialResult(2, valueOf(6854), dp2, c2)),
                58,
                18
        ), secretKey);

        for (Integer id : postTo) {
            targets.get(id).path("result").request().post(Entity.entity(partialResultList, MediaType.APPLICATION_JSON));
        }

        // Allow consensus protocol to be run
        Thread.sleep(4_000);

        List<SignedEntity<PartialResultList>> lastSeenResultList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String resultListString = target.path("result").request()
                    .get(String.class);
            List<SignedEntity<PartialResultList>> fetchedResultList = mapper.readValue(resultListString, new TypeReference<List<SignedEntity<PartialResultList>>>() {
            });

            if (!lastSeenResultList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on result lists", lastSeenResultList, fetchedResultList);
            }

            assertEquals("Result list should be of size 1", 1, fetchedResultList.size());

            lastSeenResultList = fetchedResultList;
        }
    }

    @Test
    public void shouldAgreeOnSingleResultWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleResult(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSingleResultWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleResult(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSingleResultWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleResult(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSingleResultWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleResult(Arrays.asList(1, 2, 3, 4));
    }

    private void testPostSingleCommitment(List<Integer> postTo) throws InterruptedException, IOException {
        SignedEntity<CommitmentDTO> commitmentDTO = new SignedEntity<>(
                new CommitmentDTO(new BigInteger[]{valueOf(584), valueOf(56498), valueOf(650)}, 1, "BAR"),
                secretKey);

        for (Integer id : postTo) {
            targets.get(id).path("commitments").request().post(Entity.entity(commitmentDTO, MediaType.APPLICATION_JSON));
        }

        // Allow consensus protocol to be run
        Thread.sleep(4_000);

        List<SignedEntity<CommitmentDTO>> lastSeenCommitmentList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String commitmentsString = target.path("commitments").request().get(String.class);
            List<SignedEntity<CommitmentDTO>> fetchedCommitmentList
                    = mapper.readValue(commitmentsString, new TypeReference<List<SignedEntity<CommitmentDTO>>>() {
            });

            if (!lastSeenCommitmentList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on commitment lists", lastSeenCommitmentList, fetchedCommitmentList);
            }

            assertEquals("Commitment list should be of size 1", 1, fetchedCommitmentList.size());

            lastSeenCommitmentList = fetchedCommitmentList;
        }
    }

    @Test
    public void shouldAgreeOnSingleCommitmentWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleCommitment(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSingleCommitmentWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleCommitment(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSingleCommitmentWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleCommitment(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSingleCommitmentWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleCommitment(Arrays.asList(1, 2, 3, 4));
    }

    private void testPostSinglePedersenComplaint(List<Integer> postTo) throws InterruptedException, IOException {
        SignedEntity<PedersenComplaintDTO> pedersenComplaint = new SignedEntity<>(new PedersenComplaintDTO(1, 2), secretKey);

        for (Integer id : postTo) {
            targets.get(id).path("pedersenComplain").request().post(Entity.entity(pedersenComplaint, MediaType.APPLICATION_JSON));
        }

        // Allow consensus protocol to be run
        Thread.sleep(4_000);

        List<SignedEntity<PedersenComplaintDTO>> lastSeenComplaintsList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String complaintsString = target.path("pedersenComplaints").request()
                    .get(String.class);
            List<SignedEntity<PedersenComplaintDTO>> fetchedComplaintList
                    = mapper.readValue(complaintsString, new TypeReference<List<SignedEntity<PedersenComplaintDTO>>>() {
            });

            if (!lastSeenComplaintsList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on complaints lists", lastSeenComplaintsList, fetchedComplaintList);
            }

            assertEquals("Complaints list should be of size 1", 1, fetchedComplaintList.size());

            lastSeenComplaintsList = fetchedComplaintList;
        }
    }

    @Test
    public void shouldAgreeOnSinglePedersenComplaintWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        testPostSinglePedersenComplaint(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSinglePedersenComplaintWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSinglePedersenComplaint(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSinglePedersenComplaintWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSinglePedersenComplaint(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSinglePedersenComplaintWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSinglePedersenComplaint(Arrays.asList(1, 2, 3, 4));
    }

    private void testPostSingleFeldmanComplaint(List<Integer> postTo) throws InterruptedException, IOException {
        SignedEntity<FeldmanComplaintDTO> feldmanComplaint
                = new SignedEntity<>(new FeldmanComplaintDTO(1, 2, valueOf(123), valueOf(123)), secretKey);

        for (Integer id : postTo) {
            targets.get(id).path("feldmanComplain").request().post(Entity.entity(feldmanComplaint, MediaType.APPLICATION_JSON));
        }

        // Allow consensus protocol to be run
        Thread.sleep(4_000);

        List<SignedEntity<FeldmanComplaintDTO>> lastSeenComplaintsList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String complaintsString = target.path("feldmanComplaints").request().get(String.class);
            List<SignedEntity<FeldmanComplaintDTO>> fetchedComplaintList
                    = mapper.readValue(complaintsString, new TypeReference<List<SignedEntity<FeldmanComplaintDTO>>>() {
            });

            if (!lastSeenComplaintsList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on complaints lists", lastSeenComplaintsList, fetchedComplaintList);
            }

            assertEquals("Complaints list should be of size 1", 1, fetchedComplaintList.size());

            lastSeenComplaintsList = fetchedComplaintList;
        }
    }

    @Test
    public void shouldAgreeOnSingleFeldmanComplaintWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleFeldmanComplaint(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSingleFeldmanComplaintWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleFeldmanComplaint(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSingleFeldmanComplaintWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleFeldmanComplaint(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSingleFeldmanComplaintWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleFeldmanComplaint(Arrays.asList(1, 2, 3, 4));
    }

    private void testPostSingleComplaintResolve(List<Integer> postTo) throws InterruptedException, IOException {
        SignedEntity<ComplaintResolveDTO> resolve
                = new SignedEntity<>(new ComplaintResolveDTO(
                1,
                2,
                new PartialSecretMessageDTO(valueOf(564), valueOf(568), 2, 1)), secretKey);

        for (Integer id : postTo) {
            targets.get(id).path("resolveComplaint").request().post(Entity.entity(resolve, MediaType.APPLICATION_JSON));
        }

        // Allow consensus protocol to be run
        Thread.sleep(4_000);

        List<SignedEntity<ComplaintResolveDTO>> lastSeenResolvesList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String complaintResolvesString = target.path("complaintResolves").request().get(String.class);
            List<SignedEntity<ComplaintResolveDTO>> fetchedResolvesList
                    = mapper.readValue(complaintResolvesString, new TypeReference<List<SignedEntity<ComplaintResolveDTO>>>() {});

            if (!lastSeenResolvesList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on complaint resolves lists", lastSeenResolvesList, fetchedResolvesList);
            }

            assertEquals("Complaint resolves list should be of size 1", 1, fetchedResolvesList.size());

            lastSeenResolvesList = fetchedResolvesList;
        }
    }

    @Test
    public void shouldAgreeOnSingleComplaintResolveWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleComplaintResolve(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSingleComplaintResolveWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleComplaintResolve(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSingleComplaintResolveWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleComplaintResolve(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSingleComplaintResolveWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleComplaintResolve(Arrays.asList(1, 2, 3, 4));
    }

    private void testPostSingleCertificate(List<Integer> postTo) throws InterruptedException, IOException {
        SignedEntity<CertificateDTO> certificateDTOSignedEntity = new SignedEntity<>(new CertificateDTO("Test certificate", 1), secretKey);

        for (Integer id : postTo) {
            targets.get(id).path("certificates").request().post(Entity.entity(certificateDTOSignedEntity, MediaType.APPLICATION_JSON));
        }

        // Allow consensus protocol to be run
        Thread.sleep(4_000);

        List<SignedEntity<CertificateDTO>> lastSeenCertificateList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String certificateString = target.path("certificates").request().get(String.class);
            List<SignedEntity<CertificateDTO>> fetchedCertificateList = mapper.readValue(certificateString, new TypeReference<List<SignedEntity<CertificateDTO>>>() {
            });

            if (!lastSeenCertificateList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on certificate lists", lastSeenCertificateList, fetchedCertificateList);
            }

            assertEquals("Certificate list should be of size 1", 1, fetchedCertificateList.size());

            lastSeenCertificateList = fetchedCertificateList;
        }
    }

    @Test
    public void shouldAgreeOnSingleCertificateWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleCertificate(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSingleCertificateWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleCertificate(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSingleCertificateWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleCertificate(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSingleCertificateWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostSingleCertificate(Arrays.asList(1, 2, 3, 4));
    }

    @After
    public void tearDown() throws InterruptedException {
        for (File file : files) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            } catch (Exception ignored) {
            }
        }

        for (BulletinBoardPeer peer : bulletinBoardPeers.values()) {
            peer.terminate();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
