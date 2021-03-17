package dk.mmj.eevhe.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.fail;

public class TestSSLHelper {

    @Test
    public void crashesOnException() {
        Logger logger = LogManager.getLogger(TestSSLHelper.class);
        try {
            SSLHelper.configureWebTarget(logger, null);
            fail("Should have thrown exception");
        } catch (Exception ignored) {
        }
    }

}
