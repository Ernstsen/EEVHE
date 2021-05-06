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
import org.junit.Test;

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
import static org.junit.Assert.assertTrue;

public class TestBulletinBoardEdge {
    private static final Logger logger = LogManager.getLogger(TestBulletinBoardPeer.class);
    private static final int port = 28081;
    private final List<File> files = new ArrayList<>();
    private BulletinBoardEdge bulletinBoardEdge;
    private final ObjectMapper mapper = new ObjectMapper();
    private String confPath;
    private Thread thread;
    private JerseyWebTarget target;

    private void buildTempFiles() throws IOException {
        File folder = new File(confPath);

        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();

        File common = new File(folder, "BB_input.json");

        BBInput input = new BBInput(
                new ArrayList<>(),
                Collections.singletonList(new PeerInfo(1, "https://localhost:28081")));

        mapper.writeValue(common, input);
        files.add(common);

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
    }

    @Test
    public void getPort() {
        assertEquals("did not use supplied port", port, bulletinBoardEdge.getPort());
    }

    @Test
    public void serverTypeAndTime() {
        String type = target.path("type").request().get(String.class);
        assertEquals("Wrong type string returned", "<b>ServerType:</b> Bulletin Board Edge", type);

        Long time = target.path("getCurrentTime").request().get(Long.class);
        long now = new Date().getTime();
        assertTrue("Time should have passed since fetching time there: " + time + ", now:" + now, time <= now);
    }
}
