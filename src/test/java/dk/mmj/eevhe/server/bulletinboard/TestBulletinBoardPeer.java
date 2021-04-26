package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.client.SSLHelper;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.agreement.AgreementHelper;
import dk.mmj.eevhe.protocols.agreement.broadcast.BrachaBroadcastManager;
import dk.mmj.eevhe.protocols.agreement.broadcast.BroadcastManager;
import dk.mmj.eevhe.server.ServerState;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.*;

public class TestBulletinBoardPeer {
    private static final Logger logger = LogManager.getLogger(TestBulletinBoardPeer.class);
    private static final int port = 18081;
    private BulletinBoardPeer bulletinBoardPeer;
    private ObjectMapper mapper = new ObjectMapper();
    private final List<File> files = new ArrayList<>();
    private String confPath;

    private void buildTempFiles() throws IOException {
        File file = new File(confPath);

        //noinspection ResultOfMethodCallIgnored
        file.mkdirs();

        File common = new File(file, "BB_input.json");

        String certString = new String(Files.readAllBytes(Paths.get("certs/test_glob.pem")));

        BBInput input = new BBInput(
                Arrays.asList(new BBPeerInfo(1, "https://localhost:18081", certString)),
                new ArrayList<>());

        mapper.writeValue(common, input);
        files.add(common);

        File zip = new File(file, "BB_Peer1.zip");
        try (ZipOutputStream ous = new ZipOutputStream(new FileOutputStream(zip))) {
            ous.putNextEntry(new ZipEntry("sk.pem"));
            IOUtils.copy(Files.newInputStream(Paths.get("certs/test_glob_key.pem")), ous);
        }

        files.add(zip);
        files.add(file);
    }

    @Before
    public void setUp() throws Exception {
        confPath = "temp_conf/";

        ServerState.getInstance().reset();

        buildTempFiles();

        BulletinBoardPeer.BulletinBoardPeerConfiguration config = new BulletinBoardPeer.BulletinBoardPeerConfiguration(port, confPath, 1);
        bulletinBoardPeer = new BulletinBoardPeer(config);

        Field agreementHelperField = BulletinBoardPeer.class.getDeclaredField("agreementHelper");
        agreementHelperField.setAccessible(true);
        AgreementHelper agreementHelper = (AgreementHelper) agreementHelperField.get(bulletinBoardPeer);

        Field broadcastManagerField = AgreementHelper.class.getDeclaredField("broadcastManager");
        broadcastManagerField.setAccessible(true);
        BrachaBroadcastManager broadcastManager = (BrachaBroadcastManager) broadcastManagerField.get(agreementHelper);

        Field tField = BrachaBroadcastManager.class.getDeclaredField("t");
        tField.setAccessible(true);
        tField.set(broadcastManager, -1);
    }

    @Test
    public void getPort() {
        assertEquals("did not use supplied port", port, bulletinBoardPeer.getPort());
    }

    @Test
    public void serverTypeAndTime() throws InterruptedException {
        Thread thread = new Thread(bulletinBoardPeer);
        thread.start();
        Thread.sleep(2_000);

        JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);
        String type = target.path("type").request().get(String.class);
        assertEquals("Wrong type string returned", "<b>ServerType:</b> Bulletin Board Peer", type);

        Long time = target.path("getCurrentTime").request().get(Long.class);
        long now = new Date().getTime();
        assertTrue("Time should have passed since fetching time there: " + time + ", now:" + now, time <= now);


        bulletinBoardPeer.terminate();
        thread.join();
    }

    @Test
    public void postAndRetrieve() throws InterruptedException, IOException {
        Thread thread = new Thread(bulletinBoardPeer);
        thread.start();
        Thread.sleep(2_000);

        JerseyWebTarget target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);

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
        assertEquals("should be successful post", 204,
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

        String resultListString = target.path("result").request()
                .get(String.class);
        List<SignedEntity<PartialResultList>> fetchedResultList = mapper.readValue(resultListString, new TypeReference<List<SignedEntity<PartialResultList>>>() {
        });
        assertEquals("Unexpected list size", 1, fetchedResultList.size());
        assertEquals("Fetched result did not match posted one", partialResultList, fetchedResultList.get(0));

        String ballotsString = target.path("getBallots").request()
                .get(String.class);
        List<PersistedBallot> fetchedBallotList = mapper.readValue(ballotsString, new TypeReference<List<PersistedBallot>>() {
        });
        assertEquals("Unexpected list size", 2, fetchedBallotList.size());
        PersistedBallot persistedBallot = fetchedBallotList.get(0);
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

        bulletinBoardPeer.terminate();
        thread.join();
    }

    @After
    public void tearDown() {
        for (File file : files) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            } catch (Exception ignored) {
            }
        }
    }
}
