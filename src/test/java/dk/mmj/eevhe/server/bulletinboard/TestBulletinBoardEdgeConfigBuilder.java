package dk.mmj.eevhe.server.bulletinboard;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.AbstractConfigTest;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TestBulletinBoardEdgeConfigBuilder extends AbstractConfigTest {


    @Test
    public void portIsRespected() {
        int port = new Random().nextInt();

        BulletinBoardEdgeConfigBuilder builder = new BulletinBoardEdgeConfigBuilder();

        try {
            BulletinBoardEdge.BulletinBoardEdgeConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(new String[]{"--port=" + port, "--id=foo", "--conf=certs/"});

            assertEquals("Port parameter not respected", port, config.getPort());
            assertEquals("Configuration path not respected", "certs/", config.getConfPath());
            assertEquals("Id not respected", "foo", config.getId());

            BulletinBoardEdge inst = config.produceInstance();
            assertNotNull("Failed to produce instance", inst);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void noExceptionOnUnrecognized() {
        BulletinBoardEdgeConfigBuilder builder = new BulletinBoardEdgeConfigBuilder();

        try {
            new SingletonCommandLineParser<>(builder).parse(new String[]{"--part=" + 564});
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Override
    protected List<String> getParams() {
        return new BulletinBoardEdgeConfigBuilder().getParameters();
    }

    @Override
    protected String getHelp() {
        return new BulletinBoardEdgeConfigBuilder().help();
    }
}
