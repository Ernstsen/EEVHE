package dk.mmj.eevhe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestMain {
    private static final Logger logger = LogManager.getLogger(TestMain.class);

    @Test
    public void testBulletinBoard() throws InterruptedException {
        Thread thread = new Thread(() -> Main.main(new String[]{"--bulletinBoard"}));
        thread.start();


        Thread.sleep(5_000);

        JerseyWebTarget target = configureWebTarget(logger, "https://localhost:8080");

        Response resp = target.path("type").request().buildGet().invoke();

        if (resp.getStatus() != 200) {
            fail("BulletinBoard did not return 200 on type request");
        }

        assertEquals("Wrong type returned", "<b>ServerType:</b> Bulletin Board", resp.readEntity(String.class));
    }
}
