package dk.mmj.eevhe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.After;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.nio.file.Files;
import java.nio.file.Paths;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestMain {
    private static final Logger logger = LogManager.getLogger(TestMain.class);

    @Test
    public void testBulletinBoard() throws InterruptedException {
        Main.main(new String[]{"--configuration", "--bb_peer_addresses", "-1_https://localhost:18081"});

//        TODO: Fix:
        Thread thread = new Thread(() -> Main.main(new String[]{"--bulletinBoardPeer", "--id=1"}));
        thread.start();


        Thread.sleep(5_000);

        JerseyWebTarget target = configureWebTarget(logger, "https://localhost:8080");

        Response resp = target.path("type").request().get();

        assertEquals("BulletinBoard did not return 200 on type request", 200, resp.getStatus());

        assertTrue("Wrong type returned", resp.readEntity(String.class).contains("<b>ServerType:</b> Bulletin Board Peer"));
    }

    @After
    public void tearDown() throws Exception {
        Files.delete(Paths.get("./conf/BB_peer1.zip"));
        Files.delete(Paths.get("./conf/BB_input.json"));
    }
}
