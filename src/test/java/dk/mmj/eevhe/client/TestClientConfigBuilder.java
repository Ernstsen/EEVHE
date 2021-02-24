package dk.mmj.eevhe.client;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.AbstractConfigTest;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

@SuppressWarnings("ConstantConditions")
public class TestClientConfigBuilder extends AbstractConfigTest {

    @Test
    public void parametersAreRespectedFetcher() {
        boolean read = true;//As standard is false
        boolean force = true;//As standard is false
        String serverString = "https://wioaougwnwoiengw.com:68541";
        ClientConfigBuilder builder = new ClientConfigBuilder();

        try {
            String args = "--read=" + read + " --forceCalculations=" + force + " --server=" + serverString;
            Client.ClientConfiguration config = (Client.ClientConfiguration)
                    new SingletonCommandLineParser(builder).parse(args.split(" "));

            assertTrue("Should be ResultsFetcher, as read was true", config instanceof ResultFetcher.ResultFetcherConfiguration);

            ResultFetcher.ResultFetcherConfiguration fetchConfig = (ResultFetcher.ResultFetcherConfiguration) config;

            assertEquals("force parameter not respected", force, fetchConfig.isForceCalculations());
            assertEquals("Server parameter not respected", serverString, config.getTargetUrl());
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void noExceptionOnUnrecognized() {
        boolean read = true;//As standard is false
        boolean force = true;//As standard is false
        String serverString = "https://wioaougwnwoiengw.com:68541";
        ClientConfigBuilder builder = new ClientConfigBuilder();

        try {
            String args = "--read=" + read + " --forceCalcuflations=" + force + " --server=" + serverString;
            Client.ClientConfiguration config = (Client.ClientConfiguration)
                    new SingletonCommandLineParser(builder).parse(args.split(" "));

            assertTrue("Should be ResultsFetcher, as read was true", config instanceof ResultFetcher.ResultFetcherConfiguration);

            ResultFetcher.ResultFetcherConfiguration fetchConfig = (ResultFetcher.ResultFetcherConfiguration) config;

            assertFalse("force parameter not default", fetchConfig.isForceCalculations());
            assertEquals("Server parameter not respected", serverString, config.getTargetUrl());
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void parametersAreRespectedVoter() {
        String targetUrl = "https://wioaougwnwoieqwengw.com:68541";
        String id = "wiugnewoenavnauwnev";
        int vote = 4;
        int multi = 200;

        ClientConfigBuilder builder = new ClientConfigBuilder();

        try {
            String args = "--server=" + targetUrl + " --id=" + id + " --vote=" + vote
                    + " --multi=" + multi;
            Client.ClientConfiguration config = (Client.ClientConfiguration)
                    new SingletonCommandLineParser(builder).parse(args.split(" "));

            assertTrue("Should be voter as read was not set", config instanceof Voter.VoterConfiguration);

            Voter.VoterConfiguration voterConfig = (Voter.VoterConfiguration) config;

            assertEquals("Target URL parameter not respected", targetUrl, voterConfig.getTargetUrl());
            assertEquals("Id parameter not respected", id, voterConfig.getId());
            assertEquals("Multi parameter not respected", multi, voterConfig.getMulti().intValue());
            assertEquals("Vote paremeter not respected", vote, voterConfig.getVote().intValue());

        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Override
    protected List<String> getParams() {
        return new ClientConfigBuilder().getParameters();
    }

    @Override
    protected String getHelp() {
        return new ClientConfigBuilder().help();
    }
}
