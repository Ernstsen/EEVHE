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
    private final int CONSENSUS_WAIT_TIMEOUT = 2000;

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

    private void testPostAndRetrieveBallot(List<Integer> postTo) throws InterruptedException, JsonProcessingException {
        PublicKey publicKey = TestUtils.generateKeysFromP2048bitsG2().getPublicKey();
        BallotDTO ballotDTO = SecurityUtils.generateBallot(1, 3, "voter_id", publicKey);

        for (Integer id : postTo) {
            targets.get(id).path("postBallot").request().post(Entity.entity(ballotDTO, MediaType.APPLICATION_JSON));
        }

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

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
        testPostAndRetrieveBallot(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSingleBallotWhenTwoPeersReceivesFromEdge() throws InterruptedException, JsonProcessingException {
        testPostAndRetrieveBallot(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSingleBallotWhenThreePeersReceivesFromEdge() throws InterruptedException, JsonProcessingException {
        testPostAndRetrieveBallot(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSingleBallotWhenFourPeersReceivesFromEdge() throws InterruptedException, JsonProcessingException {
        testPostAndRetrieveBallot(Arrays.asList(1, 2, 3, 4));
    }

    private void testPostAndRetrievePublicInfo(List<Integer> postTo) throws InterruptedException, IOException {
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
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

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
        testPostAndRetrievePublicInfo(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSinglePublicInfoWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostAndRetrievePublicInfo(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSinglePublicInfoWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostAndRetrievePublicInfo(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSinglePublicInfoWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostAndRetrievePublicInfo(Arrays.asList(1, 2, 3, 4));
    }

    private void testPostAndRetrieveResultList(List<Integer> postTo) throws InterruptedException, IOException {
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
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

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
        testPostAndRetrieveResultList(Arrays.asList(1));
    }

    @Test
    public void shouldAgreeOnSingleResultWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostAndRetrieveResultList(Arrays.asList(1, 2));
    }

    @Test
    public void shouldAgreeOnSingleResultWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostAndRetrieveResultList(Arrays.asList(1, 2, 3));
    }

    @Test
    public void shouldAgreeOnSingleResultWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        testPostAndRetrieveResultList(Arrays.asList(1, 2, 3, 4));
    }

    private SignedEntity<CommitmentDTO> getCommitmentDTO(BigInteger[] commitment, int id, String protocol) {
        return new SignedEntity<>(new CommitmentDTO(commitment, id, protocol), secretKey);
    }

    private void testPostAndRetrieveCommitment(List<Integer> postTo, List<SignedEntity<CommitmentDTO>> commitmentDTOList) throws InterruptedException, IOException {
        for (Integer id : postTo) {
            for (SignedEntity<CommitmentDTO> commitmentDTO : commitmentDTOList) {
                targets.get(id).path("commitments").request().post(Entity.entity(commitmentDTO, MediaType.APPLICATION_JSON));
            }
        }

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        List<SignedEntity<CommitmentDTO>> lastSeenCommitmentList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String commitmentsString = target.path("commitments").request().get(String.class);
            List<SignedEntity<CommitmentDTO>> fetchedCommitmentList
                    = mapper.readValue(commitmentsString, new TypeReference<List<SignedEntity<CommitmentDTO>>>() {
            });

            if (!lastSeenCommitmentList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on commitment lists", lastSeenCommitmentList, fetchedCommitmentList);
            }

            assertEquals("Commitment list should be of size " + commitmentDTOList.size(), commitmentDTOList.size(), fetchedCommitmentList.size());

            lastSeenCommitmentList = fetchedCommitmentList;
        }
    }

    @Test
    public void shouldAgreeOnSingleCommitmentWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CommitmentDTO>> commitmentDTOList = Arrays.asList(
                getCommitmentDTO(new BigInteger[]{valueOf(584), valueOf(56498), valueOf(650)}, 1, "BAR"));
        testPostAndRetrieveCommitment(Arrays.asList(1), commitmentDTOList);
    }

    @Test
    public void shouldAgreeOnSingleCommitmentWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CommitmentDTO>> commitmentDTOList = Arrays.asList(
                getCommitmentDTO(new BigInteger[]{valueOf(584), valueOf(56498), valueOf(650)}, 1, "BAR"));
        testPostAndRetrieveCommitment(Arrays.asList(1, 2), commitmentDTOList);
    }

    @Test
    public void shouldAgreeOnSingleCommitmentWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CommitmentDTO>> commitmentDTOList = Arrays.asList(
                getCommitmentDTO(new BigInteger[]{valueOf(584), valueOf(56498), valueOf(650)}, 1, "BAR"));
        testPostAndRetrieveCommitment(Arrays.asList(1, 2, 3), commitmentDTOList);
    }

    @Test
    public void shouldAgreeOnSingleCommitmentWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CommitmentDTO>> commitmentDTOList = Arrays.asList(
                getCommitmentDTO(new BigInteger[]{valueOf(584), valueOf(56498), valueOf(650)}, 1, "BAR"));
        testPostAndRetrieveCommitment(Arrays.asList(1, 2, 3, 4), commitmentDTOList);
    }

    @Test
    public void shouldAgreeOnMultipleCommitmentsWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CommitmentDTO>> commitmentDTOList = Arrays.asList(
                getCommitmentDTO(new BigInteger[]{valueOf(584), valueOf(56498), valueOf(650)}, 1, "BAR"),
                getCommitmentDTO(new BigInteger[]{valueOf(114), valueOf(31651), valueOf(951)}, 2, "FOO"));
        testPostAndRetrieveCommitment(Arrays.asList(1), commitmentDTOList);
    }

    private SignedEntity<PedersenComplaintDTO> getPedersenComplaint(int senderId, int receiverId) {
        return new SignedEntity<>(new PedersenComplaintDTO(senderId, receiverId), secretKey);
    }

    private void testPostAndRetrievePedersenComplaint(List<Integer> postTo, List<SignedEntity<PedersenComplaintDTO>> complaintList) throws InterruptedException, IOException {
        for (Integer id : postTo) {
            for (SignedEntity<PedersenComplaintDTO> complaint : complaintList) {
                targets.get(id).path("pedersenComplain").request().post(Entity.entity(complaint, MediaType.APPLICATION_JSON));
            }
        }

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

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

            assertEquals("Complaints list should be of size " + complaintList.size(), complaintList.size(), fetchedComplaintList.size());

            lastSeenComplaintsList = fetchedComplaintList;
        }
    }

    @Test
    public void shouldAgreeOnSinglePedersenComplaintWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<PedersenComplaintDTO>> complaintList = Arrays.asList(getPedersenComplaint(1, 2));
        testPostAndRetrievePedersenComplaint(Arrays.asList(1), complaintList);
    }

    @Test
    public void shouldAgreeOnSinglePedersenComplaintWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<PedersenComplaintDTO>> complaintList = Arrays.asList(getPedersenComplaint(1, 2));
        testPostAndRetrievePedersenComplaint(Arrays.asList(1, 2), complaintList);
    }

    @Test
    public void shouldAgreeOnSinglePedersenComplaintWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<PedersenComplaintDTO>> complaintList = Arrays.asList(getPedersenComplaint(1, 2));
        testPostAndRetrievePedersenComplaint(Arrays.asList(1, 2, 3), complaintList);
    }

    @Test
    public void shouldAgreeOnSinglePedersenComplaintWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<PedersenComplaintDTO>> complaintList = Arrays.asList(getPedersenComplaint(1, 2));
        testPostAndRetrievePedersenComplaint(Arrays.asList(1, 2, 3, 4), complaintList);
    }

    @Test
    public void shouldAgreeOnMultiplePedersenComplaintsWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<PedersenComplaintDTO>> complaintList = Arrays.asList(
                getPedersenComplaint(1, 2),
                getPedersenComplaint(2, 3));
        testPostAndRetrievePedersenComplaint(Arrays.asList(1, 2), complaintList);
    }

    private SignedEntity<FeldmanComplaintDTO> getFeldmanComplaint(int senderId, int receiverId, int val1, int val2) {
        return new SignedEntity<>(new FeldmanComplaintDTO(senderId, receiverId, valueOf(val1), valueOf(val2)), secretKey);
    }

    private void testPostAndRetrieveFeldmanComplaint(List<Integer> postTo, List<SignedEntity<FeldmanComplaintDTO>> complaintList) throws InterruptedException, IOException {
        for (Integer id : postTo) {
            for (SignedEntity<FeldmanComplaintDTO> complaint : complaintList) {
                targets.get(id).path("feldmanComplain").request().post(Entity.entity(complaint, MediaType.APPLICATION_JSON));
            }
        }

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        List<SignedEntity<FeldmanComplaintDTO>> lastSeenComplaintsList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String complaintsString = target.path("feldmanComplaints").request().get(String.class);
            List<SignedEntity<FeldmanComplaintDTO>> fetchedComplaintList
                    = mapper.readValue(complaintsString, new TypeReference<List<SignedEntity<FeldmanComplaintDTO>>>() {
            });

            if (!lastSeenComplaintsList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on complaints lists", lastSeenComplaintsList, fetchedComplaintList);
            }

            assertEquals("Complaints list should be of size " + complaintList.size(), complaintList.size(), fetchedComplaintList.size());

            lastSeenComplaintsList = fetchedComplaintList;
        }
    }

    @Test
    public void shouldAgreeOnSingleFeldmanComplaintWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<FeldmanComplaintDTO>> complaintList = Arrays.asList(getFeldmanComplaint(1, 2, 123, 456));
        testPostAndRetrieveFeldmanComplaint(Arrays.asList(1), complaintList);
    }

    @Test
    public void shouldAgreeOnSingleFeldmanComplaintWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<FeldmanComplaintDTO>> complaintList = Arrays.asList(getFeldmanComplaint(1, 2, 123, 456));
        testPostAndRetrieveFeldmanComplaint(Arrays.asList(1, 2), complaintList);
    }

    @Test
    public void shouldAgreeOnSingleFeldmanComplaintWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<FeldmanComplaintDTO>> complaintList = Arrays.asList(getFeldmanComplaint(1, 2, 123, 456));
        testPostAndRetrieveFeldmanComplaint(Arrays.asList(1, 2, 3), complaintList);
    }

    @Test
    public void shouldAgreeOnSingleFeldmanComplaintWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<FeldmanComplaintDTO>> complaintList = Arrays.asList(getFeldmanComplaint(1, 2, 123, 456));
        testPostAndRetrieveFeldmanComplaint(Arrays.asList(1, 2, 3, 4), complaintList);
    }

    @Test
    public void shouldAgreeOnMultipleFeldmanComplaintsWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<FeldmanComplaintDTO>> complaintList = Arrays.asList(
                getFeldmanComplaint(1, 2, 123, 456),
                getFeldmanComplaint(2, 3, 852, 6523));
        testPostAndRetrieveFeldmanComplaint(Arrays.asList(1, 2, 3), complaintList);
    }

    private SignedEntity<ComplaintResolveDTO> getComplaintResolve(int senderId, int receiverId, int val1, int val2) {
        return new SignedEntity<>(
                new ComplaintResolveDTO(
                        senderId,
                        receiverId,
                        new PartialSecretMessageDTO(valueOf(val1), valueOf(val2), receiverId, senderId)),
                secretKey);
    }

    private void testPostAndRetrieveComplaintResolves(List<Integer> postTo, List<SignedEntity<ComplaintResolveDTO>> complaintResolveList) throws InterruptedException, IOException {
        for (Integer id : postTo) {
            for (SignedEntity<ComplaintResolveDTO> complaintResolve : complaintResolveList) {
                targets.get(id).path("resolveComplaint").request().post(Entity.entity(complaintResolve, MediaType.APPLICATION_JSON));
            }
        }

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        List<SignedEntity<ComplaintResolveDTO>> lastSeenResolvesList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String complaintResolvesString = target.path("complaintResolves").request().get(String.class);
            List<SignedEntity<ComplaintResolveDTO>> fetchedResolvesList
                    = mapper.readValue(complaintResolvesString, new TypeReference<List<SignedEntity<ComplaintResolveDTO>>>() {
            });

            if (!lastSeenResolvesList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on complaint resolves lists", lastSeenResolvesList, fetchedResolvesList);
            }

            assertEquals("Complaint resolves list should be of size " + complaintResolveList.size(),
                    complaintResolveList.size(),
                    fetchedResolvesList.size());

            lastSeenResolvesList = fetchedResolvesList;
        }
    }

    @Test
    public void shouldAgreeOnSingleComplaintResolveWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<ComplaintResolveDTO>> complaintResolveList = Arrays.asList(getComplaintResolve(1, 2, 123, 321));
        testPostAndRetrieveComplaintResolves(Arrays.asList(1), complaintResolveList);
    }

    @Test
    public void shouldAgreeOnSingleComplaintResolveWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<ComplaintResolveDTO>> complaintResolveList = Arrays.asList(getComplaintResolve(1, 2, 123, 321));
        testPostAndRetrieveComplaintResolves(Arrays.asList(1, 2), complaintResolveList);
    }

    @Test
    public void shouldAgreeOnSingleComplaintResolveWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<ComplaintResolveDTO>> complaintResolveList = Arrays.asList(getComplaintResolve(1, 2, 123, 321));
        testPostAndRetrieveComplaintResolves(Arrays.asList(1, 2, 3), complaintResolveList);
    }

    @Test
    public void shouldAgreeOnSingleComplaintResolveWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<ComplaintResolveDTO>> complaintResolveList = Arrays.asList(getComplaintResolve(1, 2, 123, 321));
        testPostAndRetrieveComplaintResolves(Arrays.asList(1, 2, 3, 4), complaintResolveList);
    }

    @Test
    public void shouldAgreeOnMultipleComplaintResolvesWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<ComplaintResolveDTO>> complaintResolveList = Arrays.asList(
                getComplaintResolve(1, 2, 123, 321),
                getComplaintResolve(2, 3, 541, 653));
        testPostAndRetrieveComplaintResolves(Arrays.asList(1, 2, 3, 4), complaintResolveList);
    }

    private SignedEntity<CertificateDTO> getCertificateDTO(String certificateString, int id) {
        return new SignedEntity<>(new CertificateDTO(certificateString, id), secretKey);
    }

    private void testPostAndRetrieveCertificate(List<Integer> postTo, List<SignedEntity<CertificateDTO>> certificateList) throws InterruptedException, IOException {
        for (Integer id : postTo) {
            for (SignedEntity<CertificateDTO> certificateDTO : certificateList) {
                targets.get(id).path("certificates").request().post(Entity.entity(certificateDTO, MediaType.APPLICATION_JSON));
            }
        }

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        List<SignedEntity<CertificateDTO>> lastSeenCertificateList = new ArrayList<>();
        for (JerseyWebTarget target : targets.values()) {
            String certificateString = target.path("certificates").request().get(String.class);
            List<SignedEntity<CertificateDTO>> fetchedCertificateList
                    = mapper.readValue(certificateString, new TypeReference<List<SignedEntity<CertificateDTO>>>() {
            });

            if (!lastSeenCertificateList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on certificate lists", lastSeenCertificateList, fetchedCertificateList);
            }

            assertEquals("Certificate list should be of size " + certificateList.size(), certificateList.size(), fetchedCertificateList.size());

            lastSeenCertificateList = fetchedCertificateList;
        }
    }

    @Test
    public void shouldAgreeOnSingleCertificateWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CertificateDTO>> certificateList = Arrays.asList(getCertificateDTO("Certificate", 1));
        testPostAndRetrieveCertificate(Arrays.asList(1), certificateList);
    }

    @Test
    public void shouldAgreeOnSingleCertificateWhenTwoPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CertificateDTO>> certificateList = Arrays.asList(getCertificateDTO("Certificate", 1));
        testPostAndRetrieveCertificate(Arrays.asList(1, 2), certificateList);
    }

    @Test
    public void shouldAgreeOnSingleCertificateWhenThreePeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CertificateDTO>> certificateList = Arrays.asList(getCertificateDTO("Certificate", 1));
        testPostAndRetrieveCertificate(Arrays.asList(1, 2, 3), certificateList);
    }

    @Test
    public void shouldAgreeOnSingleCertificateWhenFourPeersReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CertificateDTO>> certificateList = Arrays.asList(getCertificateDTO("Certificate", 1));
        testPostAndRetrieveCertificate(Arrays.asList(1, 2, 3, 4), certificateList);
    }

    @Test
    public void shouldAgreeOnMultipleCertificatesWhenOnePeerReceivesFromEdge() throws InterruptedException, IOException {
        List<SignedEntity<CertificateDTO>> certificateList = Arrays.asList(
                getCertificateDTO("Certificate", 1),
                getCertificateDTO("Certificate2", 2));
        testPostAndRetrieveCertificate(Arrays.asList(1), certificateList);
    }

    @Test
    public void shouldAgreeOnMixedContent() throws InterruptedException, IOException {
        testPostAndRetrieveBallot(Arrays.asList(1));

        List<SignedEntity<CertificateDTO>> certificateList = Arrays.asList(
                getCertificateDTO("Certificate", 1),
                getCertificateDTO("Certificate2", 2));
        testPostAndRetrieveCertificate(Arrays.asList(1), certificateList);

        List<SignedEntity<ComplaintResolveDTO>> complaintResolveList = Arrays.asList(getComplaintResolve(1, 2, 123, 321));
        testPostAndRetrieveComplaintResolves(Arrays.asList(1, 2, 3, 4), complaintResolveList);

        List<SignedEntity<FeldmanComplaintDTO>> feldmanComplaintList = Arrays.asList(getFeldmanComplaint(1, 2, 123, 456));
        testPostAndRetrieveFeldmanComplaint(Arrays.asList(1, 2), feldmanComplaintList);

        List<SignedEntity<PedersenComplaintDTO>> pedersenComplaintList = Arrays.asList(
                getPedersenComplaint(1, 2),
                getPedersenComplaint(2, 3));
        testPostAndRetrievePedersenComplaint(Arrays.asList(1, 2), pedersenComplaintList);

        List<SignedEntity<CommitmentDTO>> commitmentDTOList = Arrays.asList(
                getCommitmentDTO(new BigInteger[]{valueOf(584), valueOf(56498), valueOf(650)}, 1, "BAR"));
        testPostAndRetrieveCommitment(Arrays.asList(1, 2), commitmentDTOList);

        testPostAndRetrieveResultList(Arrays.asList(1, 2));

        testPostAndRetrievePublicInfo(Arrays.asList(1));

        testPostAndRetrieveBallot(Arrays.asList(1, 2, 3));
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
