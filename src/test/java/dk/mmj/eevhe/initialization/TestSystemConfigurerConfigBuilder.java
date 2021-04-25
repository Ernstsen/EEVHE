package dk.mmj.eevhe.initialization;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.AbstractConfigTest;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestSystemConfigurerConfigBuilder extends AbstractConfigTest {

    @Test
    public void parametersAreRespected() {
        int duration = 4;
        String params = "--addresses -1_https://localhost:8081 -2_https://localhost:8082 -3_https://localhost:8083 "
                + "--bb_peer_addresses -1_https://localhost:18081 -2_https://localhost:18082 -3_https://localhost:18083 "
                + "--outputFolder=conf --time -min=" + duration + " -hour=" + duration + " -day=" + duration
                + " --outputFolder=conqweff/ --certKey=certs/test_glob_key.pem --cert=certs/test_glob.pem";

        SystemConfigurerConfigBuilder builder = new SystemConfigurerConfigBuilder();

        try {
            SingletonCommandLineParser<SystemConfigurer.SystemConfiguration> parser = new SingletonCommandLineParser<>(builder);
            SystemConfigurer.SystemConfiguration config = parser.parse(params.split(" "));
            long currTime = new Date().getTime();

            Map<Integer, String> daAddresses = config.getDaAddresses();
            assertNotNull("Should have registered addresses of DAs", daAddresses);

            assertEquals("Unexpected address for DA=1", "https://localhost:8081", daAddresses.get(1));
            assertEquals("Unexpected address for DA=2", "https://localhost:8082", daAddresses.get(2));
            assertEquals("Unexpected address for DA=3", "https://localhost:8083", daAddresses.get(3));

            Map<Integer, String> bbPeerAddresses = config.getBbPeerAddresses();
            assertNotNull("Should have registered addresses of BB Peers", bbPeerAddresses);

            assertEquals("Unexpected address for BB Peer=1", "https://localhost:18081", bbPeerAddresses.get(1));
            assertEquals("Unexpected address for BB Peer=2", "https://localhost:18082", bbPeerAddresses.get(2));
            assertEquals("Unexpected address for BB Peer=3", "https://localhost:18083", bbPeerAddresses.get(3));

            int minute = 60 * 1_000;
            int hour = minute * 60;
            int day = hour * 24;

            long estEndTime = currTime + (duration * minute) + (duration * hour) + (duration * day);
            assertTrue("Time should be " + duration + " minutes, days and hours",
                    estEndTime <= config.getEndTime() + 5 && estEndTime >= config.getEndTime() - 5
            );

            assertEquals("Did not respect output path", "conqweff", config.getOutputFolderPath().getFileName().toString());
            assertEquals("Did not respect cert key path", Paths.get("certs/test_glob_key.pem"), config.getSkFilePath());
            assertEquals("Did not respect cert path", Paths.get("certs/test_glob.pem"), config.getCertFilePath());

            assertNotNull("Should be able to construct configurer", config.produceInstance());
        } catch (WrongFormatException | NoSuchBuilderException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void noCrashOnUnknown() {
        String params = "--addreesses -1_https://localhost:8081 -2_https://localhost:8082 -3_https://localhost:8083 "
                        + "--bb_peer_addreesses -1_https://localhost:18081 -2_https://localhost:18082 -3_https://localhost:18083 ";

        SystemConfigurerConfigBuilder builder = new SystemConfigurerConfigBuilder();

        try {
            SingletonCommandLineParser<SystemConfigurer.SystemConfiguration> parser = new SingletonCommandLineParser<>(builder);
            SystemConfigurer.SystemConfiguration config = parser.parse(params.split(" "));

            Map<Integer, String> daAddresses = config.getDaAddresses();
            assertNotNull("Should have registered addresses of DAs", daAddresses);

            Map<Integer, String> bbPeerAddresses = config.getBbPeerAddresses();
            assertNotNull("Should have registered addresses of BB Peers", bbPeerAddresses);

            assertNotNull("Should be able to construct configurer", config.produceInstance());
        } catch (WrongFormatException | NoSuchBuilderException e) {
            fail("Should not throw exception");
        }
    }

    @Override
    protected List<String> getParams() {
        return new SystemConfigurerConfigBuilder().getParameters();
    }

    @Override
    protected String getHelp() {
        return new SystemConfigurerConfigBuilder().help();
    }
}
