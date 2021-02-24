package dk.mmj.eevhe.server.decryptionauthority;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.AbstractConfigTest;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TestDecryptionAuthorityConfigBuilder extends AbstractConfigTest {

    @Test
    public void returnsNullWithNoId() {

        DecryptionAuthorityConfigBuilder builder = new DecryptionAuthorityConfigBuilder();

        try {
            DecryptionAuthority.DecryptionAuthorityConfiguration config = (DecryptionAuthority.DecryptionAuthorityConfiguration)
                    new SingletonCommandLineParser(builder).parse(new String[]{"--port=984"});

            assertNull("Config should be null when no ID is given", config);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void parametersAreRespected() {
        Random rand = new Random();
        int port = rand.nextInt();
        int id = rand.nextInt();
        String bulletinBoard = "BBPath";
        String confPath = "ConfigPath";
        boolean integrationTest = true;//As standard is false
        int corrupt = rand.nextInt();

        DecryptionAuthorityConfigBuilder builder = new DecryptionAuthorityConfigBuilder();

        try {
            String args = "--port=" + port + " --id=" + id + " --bb=" + bulletinBoard + " --conf="
                    + confPath + " --timeCorrupt=" + corrupt + " --integrationTest=" + integrationTest;
            DecryptionAuthority.DecryptionAuthorityConfiguration config = (DecryptionAuthority.DecryptionAuthorityConfiguration)
                    new SingletonCommandLineParser(builder).parse(args.split(" "));

            assertEquals("Port parameter not respected", port, config.getPort());
            assertEquals("Id parameter not respected", id, config.getId());
            assertEquals("BulletinBoard not respected", bulletinBoard, config.getBulletinBoard());
            assertEquals("ConfPath not respected", confPath, config.getConfPath());
            assertTrue("IntegrationTest not respected", config.isIntegrationTest());
            assertEquals("TimeCorrupt not respected", corrupt, config.getTimeCorrupt());

        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void bb2IsRespected() {
        Random rand = new Random();
        int port = rand.nextInt();
        int id = rand.nextInt();
        String bulletinBoard = "BBPath";

        DecryptionAuthorityConfigBuilder builder = new DecryptionAuthorityConfigBuilder();

        try {
            String args = "--port=" + port + " --id=" + id + " --bulletinBoard=" + bulletinBoard;
            DecryptionAuthority.DecryptionAuthorityConfiguration config = (DecryptionAuthority.DecryptionAuthorityConfiguration)
                    new SingletonCommandLineParser(builder).parse(args.split(" "));

            assertEquals("Port parameter not respected", port, config.getPort());
            assertEquals("Id parameter not respected", id, config.getId());
            assertEquals("BulletinBoard not respected", bulletinBoard, config.getBulletinBoard());

        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void noExceptionOnUnrecognized() {
        Random rand = new Random();
        int port = rand.nextInt();
        int id = rand.nextInt();
        String bulletinBoard = "BBPath";

        DecryptionAuthorityConfigBuilder builder = new DecryptionAuthorityConfigBuilder();

        try {
            String args = "--port=" + port + " --id=" + id + " --bulletisnBoard=" + bulletinBoard;
            DecryptionAuthority.DecryptionAuthorityConfiguration config = (DecryptionAuthority.DecryptionAuthorityConfiguration)
                    new SingletonCommandLineParser(builder).parse(args.split(" "));

            assertEquals("Port parameter not respected", port, config.getPort());
            assertEquals("Id parameter not respected", id, config.getId());

        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Override
    protected List<String> getParams() {
        List<String> parameters = new DecryptionAuthorityConfigBuilder().getParameters();
        parameters.remove("integrationTest=");//ONLY for internal use!
        return parameters;
    }

    @Override
    protected String getHelp() {
        return new DecryptionAuthorityConfigBuilder().help();
    }
}
