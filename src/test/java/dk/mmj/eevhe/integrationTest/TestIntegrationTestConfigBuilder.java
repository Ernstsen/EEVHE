package dk.mmj.eevhe.integrationTest;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.AbstractConfigTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestIntegrationTestConfigBuilder extends AbstractConfigTest {

    @Test
    public void parametersAreRespected() {
        IntegrationTestConfigBuilder builder = new IntegrationTestConfigBuilder();

        try {
            String args = "--vote -start -end -after --disabledAuthorities -1 -2 -3 --duration=1337";

            IntegrationTest.IntegrationTestConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(args.split(" "));

            assertTrue("All DAs should be disabled", config.getDecryptionAuthorities().isEmpty());
            assertEquals("Should have three scheduled votes", 3, config.getVoteDelays().size());
            assertEquals("Did not respect duration param", 1337, config.getDuration());

            IntegrationTest da = config.produceInstance();
            assertNotNull("Failed to produce instance", da);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void disableOneDA() {
        IntegrationTestConfigBuilder builder = new IntegrationTestConfigBuilder();

        try {
            String args = "--vote -start -end -after --disabledAuthorities -2 --duration=1337";

            IntegrationTest.IntegrationTestConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(args.split(" "));

            assertEquals("Only DA 2 should be disabled", Arrays.asList(1, 3), config.getDecryptionAuthorities());
            assertEquals("Should have three scheduled votes", 3, config.getVoteDelays().size());
            assertEquals("Did not respect duration param", 1337, config.getDuration());

            IntegrationTest da = config.produceInstance();
            assertNotNull("Failed to produce instance", da);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void doesNotCrash() {
        IntegrationTestConfigBuilder builder = new IntegrationTestConfigBuilder();

        try {
            String args = "--vote -start -end -aftr --disabledAuthorities -22 --duration=1337 --test";

            IntegrationTest.IntegrationTestConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(args.split(" "));

            assertEquals("No DAs should be disabled", 3, config.getDecryptionAuthorities().size());
            assertEquals("Should have two scheduled votes", 2, config.getVoteDelays().size());
            assertEquals("Did not respect duration param", 1337, config.getDuration());

            IntegrationTest da = config.produceInstance();
            assertNotNull("Failed to produce instance", da);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }


    @Override
    protected List<String> getParams() {
        return new IntegrationTestConfigBuilder().getParameters();
    }

    @Override
    protected String getHelp() {
        return new IntegrationTestConfigBuilder().help();
    }
}
