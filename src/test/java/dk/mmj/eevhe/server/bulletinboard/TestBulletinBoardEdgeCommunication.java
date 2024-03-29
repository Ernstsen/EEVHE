package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.client.SSLHelper;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.TestUtils;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.entities.wrappers.*;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestBulletinBoardEdgeCommunication {
    private static final Logger logger = LogManager.getLogger(TestBulletinBoardPeer.class);
    private static final int edgePort = 28081;
    private static final List<Integer> bulletinBoardPeerIds = Arrays.asList(1, 2, 3, 4);
    private final List<File> files = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Integer, BulletinBoardPeer> bulletinBoardPeers = new HashMap<>();
    private final Map<Integer, JerseyWebTarget> peerTargets = new HashMap<>();
    private final int CONSENSUS_WAIT_TIMEOUT = 2000;
    private BulletinBoardEdge bulletinBoardEdge;
    private String confPath;
    private JerseyWebTarget edgeTarget;
    private ArrayList<Thread> peerThreads;
    private AsymmetricKeyParameter secretKey;
    private String cert;
    private AsymmetricKeyParameter pk;

    private <T> T unpack(SignedEntity<? extends Wrapper<T>> entity) throws JsonProcessingException {
        assertTrue("Failed to verify signature", entity.verifySignature(pk));
        return entity.getEntity().getContent();
    }

    private void buildTempFiles() throws IOException {
        File folder = new File(confPath);

        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();

        File common = new File(folder, "BB_input.json");

        secretKey = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
        cert = new String(Files.readAllBytes(Paths.get("certs/test_glob.pem")));
        pk = CertificateHelper.getPublicKeyFromCertificate(cert.getBytes(StandardCharsets.UTF_8));

        BBInput input = new BBInput(
                bulletinBoardPeerIds.stream().map(id -> new BBPeerInfo(id, "https://localhost:1808" + id, cert))
                        .collect(Collectors.toList()),
                Collections.singletonList(new PeerInfo(1, "https://localhost:28081")));

        mapper.writeValue(common, input);
        files.add(common);

        for (int id : bulletinBoardPeerIds) {
            File zip = new File(folder, "BB_peer" + id + ".zip");
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
        ServerState.getInstance().reset();

        confPath = "temp_conf/";

        buildTempFiles();

        BulletinBoardEdge.BulletinBoardEdgeConfiguration config = new BulletinBoardEdge.BulletinBoardEdgeConfiguration(edgePort, confPath, "1");
        bulletinBoardEdge = new BulletinBoardEdge(config);

        Thread edgeThread = new Thread(bulletinBoardEdge);
        edgeThread.start();
        Thread.sleep(2_000);

        edgeTarget = SSLHelper.configureWebTarget(logger, "https://localhost:" + edgePort);

        for (int id : bulletinBoardPeerIds) {
            Integer port = new Integer("1808" + id);

            BulletinBoardPeer.BulletinBoardPeerConfiguration bulletinBoardPeerConfiguration
                    = new BulletinBoardPeer.BulletinBoardPeerConfiguration(
                    port,
                    confPath,
                    id);

            BulletinBoardPeer bulletinBoardPeer = new BulletinBoardPeer(bulletinBoardPeerConfiguration);
            bulletinBoardPeers.put(id, bulletinBoardPeer);

            JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);
            peerTargets.put(id, target);
        }

        peerThreads = new ArrayList<>();

        for (BulletinBoardPeer bulletinBoardPeer : bulletinBoardPeers.values()) {
            Thread thread = new Thread(bulletinBoardPeer);
            thread.start();
            peerThreads.add(thread);
        }
        Thread.sleep(2_000);
    }

    private <T extends Wrapper<?>> void assertEdgeReceivedCorrectDataFromPeers(String path, TypeReference<List<SignedEntity<T>>> typeReference) throws JsonProcessingException {
        assertEdgeReceivedCorrectDataFromPeers(
                path,
                typeReference,
                (lastSeen, current) -> assertEquals("Bulletin Board Peers do not agree", lastSeen, current));
    }

    private <T extends Wrapper<?>> void assertEdgeReceivedCorrectDataFromPeers(String path, TypeReference<List<SignedEntity<T>>> typeReference,
                                                                               BiConsumer<SignedEntity<T>, SignedEntity<T>> equalityAssertion) throws JsonProcessingException {
        // Assert that edge retrieves correct data from peers
        String listOfSignedWrapperStrings = edgeTarget.path(path).request().get(String.class);
        List<SignedEntity<T>> listOfSignedWrappers = mapper.readValue(listOfSignedWrapperStrings, typeReference);

        SignedEntity<T> lastSeenSignedWrapper = null;
        for (SignedEntity<T> signedWrapper : listOfSignedWrappers) {
            if (lastSeenSignedWrapper != null) {
                equalityAssertion.accept(lastSeenSignedWrapper, signedWrapper);
            }

            assertEquals("List should be of size 1", 1, ((List<?>) signedWrapper.getEntity().getContent()).size());

            lastSeenSignedWrapper = signedWrapper;
        }
    }

    @Test
    public void testPostAndRetrieveBallot() throws InterruptedException, JsonProcessingException {
        PublicKey publicKey = TestUtils.generateKeysFromP2048bitsG2().getPublicKey();
        BallotDTO ballotDTO = SecurityUtils.generateBallot(1, 3, "voter_id", publicKey);

        // Post to edge
        edgeTarget.path("postBallot").request().post(Entity.entity(ballotDTO, MediaType.APPLICATION_JSON));

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        // Assert that peers agree on content
        List<PersistedBallot> lastSeenBallotList = new ArrayList<>();
        for (JerseyWebTarget target : peerTargets.values()) {
            String ballotsString = target.path("getBallots").request().get(String.class);
            SignedEntity<BallotWrapper> signedBallotList = mapper.readValue(
                    ballotsString,
                    new TypeReference<SignedEntity<BallotWrapper>>() {
                    });
            List<PersistedBallot> fetchedBallotList = unpack(signedBallotList);
            fetchedBallotList.sort(Comparator.comparing(BallotDTO::getId));

            if (!lastSeenBallotList.isEmpty()) {
                assertEquals("BB peers does not agree on ballot list length", lastSeenBallotList.size(), fetchedBallotList.size());
                for (int i = 0; i < lastSeenBallotList.size(); i++) {
                    assertTrue("BBPeers disagreed on ballot in position " + i, lastSeenBallotList.get(i).isSameBallot(fetchedBallotList.get(i)));
                }
            }

            assertEquals("Ballot list should be of size 1", 1, fetchedBallotList.size());

            lastSeenBallotList = fetchedBallotList;
        }

        assertEdgeReceivedCorrectDataFromPeers("getBallots", new TypeReference<List<SignedEntity<BallotWrapper>>>() {
                },
                (last, current) -> {
                    assertEquals("Content differed in length", last.getEntity().getContent().size(), current.getEntity().getContent().size());
                    for (int i = 0; i < last.getEntity().getContent().size(); i++) {
                        assertTrue("BBPeers disagreed on ballot in position " + i, last.getEntity().getContent().get(i).isSameBallot(current.getEntity().getContent().get(i)));
                    }
                });
    }

    @Test
    public void testPostAndRetrievePublicInfo() throws InterruptedException, JsonProcessingException {
        PublicKey publicKey = TestUtils.generateKeysFromP2048bitsG2().getPublicKey();

        SignedEntity<PartialPublicInfo> partialPublicInfo = new SignedEntity<>(new PartialPublicInfo(
                1, publicKey, valueOf(124121),
                Arrays.asList(new Candidate(1, "name", "desc"), new Candidate(2, "name2", "desc2")),
                6584198494L, cert
        ), secretKey);

        // Post to edge
        edgeTarget.path("publicInfo").request().post(Entity.entity(partialPublicInfo, MediaType.APPLICATION_JSON));

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        // Assert that peers agree on content
        List<SignedEntity<PartialPublicInfo>> lastSeenPartialInfoList = new ArrayList<>();
        for (JerseyWebTarget target : peerTargets.values()) {
            String publicInfoString = target.path("publicInfo").request()
                    .get(String.class);
            SignedEntity<PublicInfoWrapper> signedPublicInfo = mapper.readValue(
                    publicInfoString,
                    new TypeReference<SignedEntity<PublicInfoWrapper>>() {
                    });
            List<SignedEntity<PartialPublicInfo>> fetchedPublicInfoList = unpack(signedPublicInfo);


            if (!lastSeenPartialInfoList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on partial public info lists", lastSeenPartialInfoList, fetchedPublicInfoList);
            }

            assertEquals("Partial public info list should be of size 1", 1, fetchedPublicInfoList.size());

            lastSeenPartialInfoList = fetchedPublicInfoList;
        }

        assertEdgeReceivedCorrectDataFromPeers("publicInfo", new TypeReference<List<SignedEntity<PublicInfoWrapper>>>() {
        });
    }

    @Test
    public void testPostAndRetrieveResultList() throws InterruptedException, IOException {
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

        // Post to edge
        edgeTarget.path("result").request().post(Entity.entity(partialResultList, MediaType.APPLICATION_JSON));

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        // Assert that peers agree on content
        List<SignedEntity<PartialResultList>> lastSeenResultList = new ArrayList<>();
        for (JerseyWebTarget target : peerTargets.values()) {
            String resultListString = target.path("result").request()
                    .get(String.class);
            SignedEntity<PartialResultWrapper> signedResults = mapper.readValue(
                    resultListString,
                    new TypeReference<SignedEntity<PartialResultWrapper>>() {
                    });
            List<SignedEntity<PartialResultList>> fetchedResultList = unpack(signedResults);

            if (!lastSeenResultList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on result lists", lastSeenResultList, fetchedResultList);
            }

            assertEquals("Result list should be of size 1", 1, fetchedResultList.size());

            lastSeenResultList = fetchedResultList;
        }

        assertEdgeReceivedCorrectDataFromPeers("result", new TypeReference<List<SignedEntity<PartialResultWrapper>>>() {
        });
    }

    @Test
    public void testPostAndRetrieveCommitment() throws InterruptedException, IOException {
        SignedEntity<CommitmentDTO> commitmentDTO
                = new SignedEntity<>(new CommitmentDTO(new BigInteger[]{valueOf(584), valueOf(56498), valueOf(650)}, 1, "BAR"), secretKey);
        edgeTarget.path("commitments").request().post(Entity.entity(commitmentDTO, MediaType.APPLICATION_JSON));

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        // Assert that peers agree on content
        List<SignedEntity<CommitmentDTO>> lastSeenCommitmentList = new ArrayList<>();
        for (JerseyWebTarget target : peerTargets.values()) {
            String commitmentsString = target.path("commitments").request().get(String.class);
            SignedEntity<CommitmentWrapper> signedCommits = mapper.readValue(
                    commitmentsString,
                    new TypeReference<SignedEntity<CommitmentWrapper>>() {
                    });
            List<SignedEntity<CommitmentDTO>> fetchedCommitmentList = unpack(signedCommits);

            if (!lastSeenCommitmentList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on commitment lists", lastSeenCommitmentList, fetchedCommitmentList);
            }

            assertEquals("Commitment list should be of size 1", 1, fetchedCommitmentList.size());

            lastSeenCommitmentList = fetchedCommitmentList;
        }

        assertEdgeReceivedCorrectDataFromPeers("commitments", new TypeReference<List<SignedEntity<CommitmentWrapper>>>() {
        });
    }

    @Test
    public void testPostAndRetrievePedersenComplaint() throws InterruptedException, IOException {
        SignedEntity<PedersenComplaintDTO> complaint = new SignedEntity<>(new PedersenComplaintDTO(1, 2), secretKey);
        edgeTarget.path("pedersenComplain").request().post(Entity.entity(complaint, MediaType.APPLICATION_JSON));

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        List<SignedEntity<PedersenComplaintDTO>> lastSeenComplaintsList = new ArrayList<>();
        for (JerseyWebTarget target : peerTargets.values()) {
            String complaintsString = target.path("pedersenComplaints").request()
                    .get(String.class);
            SignedEntity<PedersenComplaintWrapper> signedPedersenComplaints = mapper.readValue(
                    complaintsString,
                    new TypeReference<SignedEntity<PedersenComplaintWrapper>>() {
                    });
            List<SignedEntity<PedersenComplaintDTO>> fetchedComplaintList = unpack(signedPedersenComplaints);

            if (!lastSeenComplaintsList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on complaints lists", lastSeenComplaintsList, fetchedComplaintList);
            }

            assertEquals("Complaints list should be of size 1", 1, fetchedComplaintList.size());

            lastSeenComplaintsList = fetchedComplaintList;
        }

        assertEdgeReceivedCorrectDataFromPeers("pedersenComplaints", new TypeReference<List<SignedEntity<PedersenComplaintWrapper>>>() {
        });
    }

    @Test
    public void testPostAndRetrieveFeldmanComplaint() throws InterruptedException, IOException {
        SignedEntity<FeldmanComplaintDTO> complaint = new SignedEntity<>(new FeldmanComplaintDTO(1, 2, valueOf(123), valueOf(456)), secretKey);
        edgeTarget.path("feldmanComplain").request().post(Entity.entity(complaint, MediaType.APPLICATION_JSON));

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        List<SignedEntity<FeldmanComplaintDTO>> lastSeenComplaintsList = new ArrayList<>();
        for (JerseyWebTarget target : peerTargets.values()) {
            String complaintsString = target.path("feldmanComplaints").request().get(String.class);
            SignedEntity<FeldmanComplaintWrapper> signedFeldmanComplaints = mapper.readValue(
                    complaintsString,
                    new TypeReference<SignedEntity<FeldmanComplaintWrapper>>() {
                    });
            List<SignedEntity<FeldmanComplaintDTO>> fetchedComplaintList = unpack(signedFeldmanComplaints);

            if (!lastSeenComplaintsList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on complaints lists", lastSeenComplaintsList, fetchedComplaintList);
            }

            assertEquals("Complaints list should be of size 1", 1, fetchedComplaintList.size());

            lastSeenComplaintsList = fetchedComplaintList;
        }

        assertEdgeReceivedCorrectDataFromPeers("feldmanComplaints", new TypeReference<List<SignedEntity<FeldmanComplaintWrapper>>>() {
        });
    }

    @Test
    public void testPostAndRetrieveComplaintResolves() throws InterruptedException, IOException {
        SignedEntity<ComplaintResolveDTO> complaintResolve = new SignedEntity<>(
                new ComplaintResolveDTO(
                        1,
                        2,
                        new PartialSecretMessageDTO(valueOf(123), valueOf(345), 2, 1)),
                secretKey);
        edgeTarget.path("resolveComplaint").request().post(Entity.entity(complaintResolve, MediaType.APPLICATION_JSON));

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        List<SignedEntity<ComplaintResolveDTO>> lastSeenResolvesList = new ArrayList<>();
        for (JerseyWebTarget target : peerTargets.values()) {
            String complaintResolvesString = target.path("complaintResolves").request().get(String.class);
            SignedEntity<ComplaintResolveWrapper> signedResolves = mapper.readValue(
                    complaintResolvesString,
                    new TypeReference<SignedEntity<ComplaintResolveWrapper>>() {
                    });
            List<SignedEntity<ComplaintResolveDTO>> fetchedResolvesList = unpack(signedResolves);

            if (!lastSeenResolvesList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on complaint resolves lists", lastSeenResolvesList, fetchedResolvesList);
            }

            assertEquals("Complaint resolves list should be of size 1", 1, fetchedResolvesList.size());

            lastSeenResolvesList = fetchedResolvesList;
        }

        assertEdgeReceivedCorrectDataFromPeers("complaintResolves", new TypeReference<List<SignedEntity<ComplaintResolveWrapper>>>() {
        });
    }

    @Test
    public void testPostAndRetrieveDACertificate() throws InterruptedException, IOException {
        SignedEntity<CertificateDTO> certificateDTO = new SignedEntity<>(new CertificateDTO("certificateString", 1), secretKey);
        edgeTarget.path("certificates").request().post(Entity.entity(certificateDTO, MediaType.APPLICATION_JSON));

        // Allow consensus protocol to be run
        Thread.sleep(CONSENSUS_WAIT_TIMEOUT);

        List<SignedEntity<CertificateDTO>> lastSeenCertificateList = new ArrayList<>();
        for (JerseyWebTarget target : peerTargets.values()) {
            String certificateString = target.path("certificates").request().get(String.class);
            SignedEntity<CertificatesWrapper> signedCertificates = mapper.readValue(
                    certificateString,
                    new TypeReference<SignedEntity<CertificatesWrapper>>() {
                    });
            List<SignedEntity<CertificateDTO>> fetchedCertificateList = unpack(signedCertificates);

            if (!lastSeenCertificateList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on certificate lists", lastSeenCertificateList, fetchedCertificateList);
            }

            assertEquals("Certificate list should be of size 1", 1, fetchedCertificateList.size());

            lastSeenCertificateList = fetchedCertificateList;
        }

        assertEdgeReceivedCorrectDataFromPeers("certificates", new TypeReference<List<SignedEntity<CertificatesWrapper>>>() {
        });
    }

    @Test
    public void shouldAgreeOnMixedContent() throws InterruptedException, IOException {
        testPostAndRetrieveBallot();

        testPostAndRetrieveDACertificate();

        testPostAndRetrieveComplaintResolves();

        testPostAndRetrieveFeldmanComplaint();

        testPostAndRetrievePedersenComplaint();

        testPostAndRetrieveCommitment();

        testPostAndRetrieveResultList();

        testPostAndRetrievePublicInfo();

        testPostAndRetrieveBallot();
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

        bulletinBoardEdge.terminate();
        for (BulletinBoardPeer peer : bulletinBoardPeers.values()) {
            peer.terminate();
        }

        for (Thread thread : peerThreads) {
            thread.join();
        }
    }
}
