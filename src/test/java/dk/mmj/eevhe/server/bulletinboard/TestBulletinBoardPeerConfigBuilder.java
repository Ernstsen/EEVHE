package dk.mmj.eevhe.server.bulletinboard;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.AbstractConfigTest;
import dk.mmj.eevhe.Main;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TestBulletinBoardPeerConfigBuilder extends AbstractConfigTest {


    @Test
    public void portIsRespected() {
        Random random = new Random();
        int port = random.nextInt();
        int id = random.nextInt();

        BulletinBoardPeerConfigBuilder builder = new BulletinBoardPeerConfigBuilder();

        try {
            BulletinBoardPeer.BulletinBoardPeerConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(new String[]{"--bulletinBoardPeer","--port=" + port, "--id=" + id, "--conf=certs/"});

            assertEquals("Port parameter not respected", port, config.getPort());
            assertEquals("Configuration path not respected", "certs/", config.getConfPath());
            assertEquals("Id not respected", id, config.getId());

        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void canProduce() {
        Main.main(new String[]{"--configuration", "--bb_peer_addresses", "-1_https://localhost:18081"});

        Random random = new Random();
        int port = random.nextInt();
        int id = 1;

        BulletinBoardPeerConfigBuilder builder = new BulletinBoardPeerConfigBuilder();

        try {
            BulletinBoardPeer.BulletinBoardPeerConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(new String[]{"--port=" + port, "--id=" + id});

            assertEquals("Port parameter not respected", port, config.getPort());

            BulletinBoardPeer inst = config.produceInstance();
            assertNotNull("Failed to produce instance", inst);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void mustSupplyId() {

        Random random = new Random();
        int port = random.nextInt();

        BulletinBoardPeerConfigBuilder builder = new BulletinBoardPeerConfigBuilder();

        try {
            BulletinBoardPeer.BulletinBoardPeerConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(new String[]{"--port=" + port});

            assertNull("as no id was supplied, configuration should be null", config);
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
        return new BulletinBoardPeerConfigBuilder().getParameters();
    }

    @Override
    protected String getHelp() {
        return new BulletinBoardPeerConfigBuilder().help();
    }
}
