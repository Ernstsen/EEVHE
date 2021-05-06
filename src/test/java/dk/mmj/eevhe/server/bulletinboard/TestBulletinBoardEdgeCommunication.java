package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.client.SSLHelper;
import dk.mmj.eevhe.entities.BBInput;
import dk.mmj.eevhe.entities.BBPeerInfo;
import dk.mmj.eevhe.entities.PeerInfo;
import dk.mmj.eevhe.server.ServerState;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.Before;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestBulletinBoardEdgeCommunication {
    private static final Logger logger = LogManager.getLogger(TestBulletinBoardPeer.class);
    private static final int port = 28081;
    private final List<File> files = new ArrayList<>();
    private BulletinBoardEdge bulletinBoardEdge;
    private final ObjectMapper mapper = new ObjectMapper();
    private String confPath;
    private Thread thread;
    private JerseyWebTarget target;
    private static final List<Integer> bulletinBoardPeerIds = Arrays.asList(1, 2, 3, 4);
    private final Map<Integer, BulletinBoardPeer> bulletinBoardPeers = new HashMap<>();
    private final Map<Integer, JerseyWebTarget> targets = new HashMap<>();
    private ArrayList<Thread> threads;


    private void buildTempFiles() throws IOException {
        File folder = new File(confPath);

        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();

        File common = new File(folder, "BB_input.json");

        String certString = new String(Files.readAllBytes(Paths.get("certs/test_glob.pem")));

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
        confPath = "temp_conf/";

        ServerState.getInstance().reset();

        buildTempFiles();

        BulletinBoardEdge.BulletinBoardEdgeConfiguration config = new BulletinBoardEdge.BulletinBoardEdgeConfiguration(port, confPath, 1);
        bulletinBoardEdge = new BulletinBoardEdge(config);

        thread = new Thread(bulletinBoardEdge);
        thread.start();
        Thread.sleep(2_000);

        target = SSLHelper.configureWebTarget(logger, "https://localhost:" + port);

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
}
