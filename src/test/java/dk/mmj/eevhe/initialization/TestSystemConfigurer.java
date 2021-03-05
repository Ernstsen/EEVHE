package dk.mmj.eevhe.initialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.mmj.eevhe.entities.DecryptionAuthorityInfo;
import dk.mmj.eevhe.entities.DecryptionAuthorityInput;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestSystemConfigurer {

    private String conf;

    @Before
    public void setUp() throws Exception {
        conf = "testConf";
    }

    @Test
    public void testMeaningfulOutput() {
        int duration = 4;
        String params = "--addresses -1_https://localhost:8081 -2_https://localhost:8082 -3_https://localhost:8083 "
                + "--outputFolder=" + conf + " --time -min=" + duration;

        SystemConfigurerConfigBuilder builder = new SystemConfigurerConfigBuilder();

        try {
            SingletonCommandLineParser<SystemConfigurer.SystemConfiguration> parser = new SingletonCommandLineParser<>(builder);
            SystemConfigurer.SystemConfiguration config = parser.parse(params.split(" "));

            long configEndTime = config.getEndTime();

            SystemConfigurer instance = config.produceInstance();
            assertNotNull("Should be able to construct configurer", instance);

            instance.run();

            Path dirPath = Paths.get(conf);
            File dirFile = dirPath.toFile();
            assertTrue("testConf folder does not exist", dirFile.exists());

            DecryptionAuthorityInput output = new ObjectMapper()
                    .readValue(dirPath.resolve("common_input.json").toFile(), DecryptionAuthorityInput.class);

            List<DecryptionAuthorityInfo> infos = output.getInfos();
            Map<Integer, String> addresses = infos.stream()
                    .collect(Collectors.toMap(DecryptionAuthorityInfo::getId, DecryptionAuthorityInfo::getAddress));

            assertEquals("Wrong address for id=" + 1, addresses.get(1), "https://localhost:8081");
            assertEquals("Wrong address for id=" + 2, addresses.get(2), "https://localhost:8082");
            assertEquals("Wrong address for id=" + 3, addresses.get(3), "https://localhost:8083");

            assertEquals("Wrong endTime", configEndTime, output.getEndTime());

            //Assert now exception is thrown
            BigInteger p = new BigInteger(Hex.decode(output.getpHex()));
            assertTrue("p should be prime", p.isProbablePrime(50));
            new BigInteger(Hex.decode(output.getgHex()));
            new BigInteger(Hex.decode(output.geteHex()));

        } catch (Exception e) {
            fail("Should not throw exception. " + e.getMessage());
        }
    }

    @After
    public void tearDown() throws IOException {
        Path path = Paths.get(conf);
        if (Files.exists(path)) {
           Files.delete(path.resolve("common_input.json"));
            Files.delete(path);
        }
    }

}
