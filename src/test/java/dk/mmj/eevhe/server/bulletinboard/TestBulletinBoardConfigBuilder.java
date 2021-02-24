package dk.mmj.eevhe.server.bulletinboard;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.AbstractConfigTest;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestBulletinBoardConfigBuilder extends AbstractConfigTest {


    @Test
    public void portIsRespected() {
        int port = new Random().nextInt();

        BulletinBoardConfigBuilder builder = new BulletinBoardConfigBuilder();

        try {
            BulletinBoard.BulletinBoardConfiguration config = (BulletinBoard.BulletinBoardConfiguration)
                    new SingletonCommandLineParser(builder).parse(new String[]{"--port=" + port});

            assertEquals("Port parameter not respected", port, config.getPort());

        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void noExceptionOnUnrecognized() {
        BulletinBoardConfigBuilder builder = new BulletinBoardConfigBuilder();

        try {
            new SingletonCommandLineParser(builder).parse(new String[]{"--part=" + 564});
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Override
    protected List<String> getParams() {
        return new BulletinBoardConfigBuilder().getParameters();
    }

    @Override
    protected String getHelp() {
        return new BulletinBoardConfigBuilder().help();
    }
}
