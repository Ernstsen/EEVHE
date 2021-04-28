package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.client.SSLHelper;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.TestUtils;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.ServerState;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @Test
    public void shouldAgreeOnSingleBallotWhenOnePeerReceivesFromEdge() throws JsonProcessingException, InterruptedException {
        PublicKey publicKey = TestUtils.generateKeysFromP2048bitsG2().getPublicKey();
        BallotDTO ballotDTO = SecurityUtils.generateBallot(1, 3, "voter_id", publicKey);

        JerseyWebTarget peerOneTarget = targets.get(1);

        String mediaType = MediaType.APPLICATION_JSON;
        assertEquals("Ballot should be successful posted", 204,
                peerOneTarget.path("postBallot").request().post(Entity.entity(ballotDTO, mediaType)).getStatus()
        );

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
