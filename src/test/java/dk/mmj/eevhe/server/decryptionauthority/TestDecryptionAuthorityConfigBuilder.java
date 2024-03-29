package dk.mmj.eevhe.server.decryptionauthority;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.AbstractConfigTest;
import dk.mmj.eevhe.entities.Candidate;
import dk.mmj.eevhe.entities.DecryptionAuthorityInput;
import dk.mmj.eevhe.entities.PeerInfo;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class TestDecryptionAuthorityConfigBuilder extends AbstractConfigTest {
    private String confPath;
    private final List<File> files = new ArrayList<>();
    private final int id = 54;

    @Before
    public void setup() throws IOException {
        DecryptionAuthorityInput input = new DecryptionAuthorityInput("02", "02", "02", 124,
                Arrays.asList(
                        new PeerInfo(1, "https://localhost:8081"),
                        new PeerInfo(2, "https://localhost:8082")
                ), new String(Files.readAllBytes(Paths.get("certs/test_glob.pem"))));

        confPath = "temp_conf/";
        File file = new File(confPath);

        //noinspection ResultOfMethodCallIgnored
        file.mkdirs();


        File common = new File(file, "common_input.json");
        new ObjectMapper().writeValue(common, input);
        files.add(common);

        File candidates = new File(file, "candidates.json");
        new ObjectMapper().writeValue(candidates, Arrays.asList(
                new Candidate(0, "name", "desc"),
                new Candidate(1, "name2", "desc2")
        ));
        files.add(candidates);

        File zip = new File(file, "DA" + id + ".zip");

        try (ZipOutputStream ous = new ZipOutputStream(new FileOutputStream(zip))) {
            ous.putNextEntry(new ZipEntry("sk.pem"));
            IOUtils.copy(Files.newInputStream(Paths.get("certs/test_glob_key.pem")), ous);
            ous.putNextEntry(new ZipEntry("cert.pem"));
            IOUtils.copy(Files.newInputStream(Paths.get("certs/test_glob.pem")), ous);
        }

        files.add(zip);
        files.add(file);
    }

    @Test
    public void returnsNullWithNoId() {

        DecryptionAuthorityConfigBuilder builder = new DecryptionAuthorityConfigBuilder();

        try {
            DecryptionAuthority.DecryptionAuthorityConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(new String[]{"--port=984"});

            assertNull("Config should be null when no ID is given", config);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
        }
    }

    @Test
    public void parametersAreRespected() {
        Random rand = new Random();
        int port = rand.nextInt();
        String bulletinBoard = "BBPath";
        int corrupt = rand.nextInt();

        DecryptionAuthorityConfigBuilder builder = new DecryptionAuthorityConfigBuilder();

        try {
            String args = "--port=" + port + " --id=" + id + " --bb=" + bulletinBoard + " --conf="
                    + confPath + " --timeCorrupt=" + corrupt;
            DecryptionAuthority.DecryptionAuthorityConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(args.split(" "));

            assertEquals("Port parameter not respected", port, config.getPort());
            assertEquals("Id parameter not respected", id, config.getId());
            assertEquals("BulletinBoard not respected", bulletinBoard, config.getBulletinBoard());
            assertEquals("ConfPath not respected", confPath, config.getConfPath());
            assertEquals("TimeCorrupt not respected", corrupt, config.getTimeCorrupt());

            DecryptionAuthority da = config.produceInstance();
            assertNotNull("Failed to produce instance", da);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
        }
    }

    @Test
    public void bb2IsRespected() {
        Random rand = new Random();
        int port = rand.nextInt();
        String bulletinBoard = "BBPath";

        DecryptionAuthorityConfigBuilder builder = new DecryptionAuthorityConfigBuilder();

        try {
            String args = "--port=" + port + " --id=" + id + " --bulletinBoard=" + bulletinBoard;
            DecryptionAuthority.DecryptionAuthorityConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(args.split(" "));

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
            DecryptionAuthority.DecryptionAuthorityConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(args.split(" "));

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

    @After
    public void tearDown() {
        for (File file : files) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            } catch (Exception ignored) {
            }
        }
    }
}
