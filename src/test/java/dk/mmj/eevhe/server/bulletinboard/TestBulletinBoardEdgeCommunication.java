package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.client.SSLHelper;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.TestUtils;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.entities.wrappers.BallotWrapper;
import dk.mmj.eevhe.entities.wrappers.Wrapper;
import dk.mmj.eevhe.server.ServerState;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestBulletinBoardEdgeCommunication {
    private static final Logger logger = LogManager.getLogger(TestBulletinBoardPeer.class);
    private static final int edgePort = 28081;
    private final List<File> files = new ArrayList<>();
    private BulletinBoardEdge bulletinBoardEdge;
    private final ObjectMapper mapper = new ObjectMapper();
    private String confPath;
    private Thread edgeThread;
    private JerseyWebTarget edgeTarget;
//    private static final List<Integer> bulletinBoardPeerIds = Arrays.asList(1, 2, 3, 4);
    private static final List<Integer> bulletinBoardPeerIds = Arrays.asList(1);
    private final Map<Integer, BulletinBoardPeer> bulletinBoardPeers = new HashMap<>();
    private final Map<Integer, JerseyWebTarget> peerTargets = new HashMap<>();
    private final int CONSENSUS_WAIT_TIMEOUT = 2000;
    private ArrayList<Thread> peerThreads;
    private AsymmetricKeyParameter pk;
    private boolean setupDone = false;

    private <T> T unpack(SignedEntity<? extends Wrapper<T>> entity) throws JsonProcessingException {
        assertTrue("Failed to verify signature", entity.verifySignature(pk));
        return entity.getEntity().getContent();
    }

    private void buildTempFiles() throws IOException {
        File folder = new File(confPath);

        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();

        File common = new File(folder, "BB_input.json");

        String certString = new String(Files.readAllBytes(Paths.get("certs/test_glob.pem")));
        pk = CertificateHelper.getPublicKeyFromCertificate(certString.getBytes(StandardCharsets.UTF_8));

        BBInput input = new BBInput(
                bulletinBoardPeerIds.stream().map(id -> new BBPeerInfo(id, "https://localhost:1808" + id, certString))
                        .collect(Collectors.toList()),
                Collections.singletonList(new PeerInfo(1, "https://localhost:28081")));

        mapper.writeValue(common, input);
        files.add(common);

        for (int id : bulletinBoardPeerIds) {
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
        ServerState.getInstance().reset();

        if(setupDone){
            return;
        }

        setupDone = true;

        confPath = "temp_conf/";

        buildTempFiles();

        BulletinBoardEdge.BulletinBoardEdgeConfiguration config = new BulletinBoardEdge.BulletinBoardEdgeConfiguration(edgePort, confPath, "1");
        bulletinBoardEdge = new BulletinBoardEdge(config);

        edgeThread = new Thread(bulletinBoardEdge);
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

    private void testPostAndRetrieveBallot() throws InterruptedException, JsonProcessingException {
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

            if (!lastSeenBallotList.isEmpty()) {
                assertEquals("Bulletin Board Peers do not agree on ballot lists", lastSeenBallotList, fetchedBallotList);
            }

            assertEquals("Ballot list should be of size 1", 1, fetchedBallotList.size());

            lastSeenBallotList = fetchedBallotList;
        }

        // Assert that edge retrieves correct data from peers
        String listOfSignedBallotsString = edgeTarget.path("getBallots").request().get(String.class);
        List<SignedEntity<BallotWrapper>> listOfSignedBallots = mapper.readValue(listOfSignedBallotsString, new TypeReference<List<SignedEntity<BallotWrapper>>>() {
        });

//        TODO: assert
    }

    @Test
    public void shouldAgreeOnSingleBallotWhenOnePeerReceivesFromEdge() throws InterruptedException, JsonProcessingException {
        testPostAndRetrieveBallot();
    }
}
