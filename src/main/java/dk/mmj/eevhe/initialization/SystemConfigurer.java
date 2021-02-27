package dk.mmj.eevhe.initialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.Application;
import dk.mmj.eevhe.crypto.keygeneration.PersistedKeyParameters;
import dk.mmj.eevhe.entities.DecryptionAuthorityInfo;
import dk.mmj.eevhe.entities.DecryptionAuthorityInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for creating configuration file(s) to be used in running the system
 */
public class SystemConfigurer implements Application {
    private static final Logger logger = LogManager.getLogger(SystemConfigurer.class);
    private final long endTime;
    private final Path outputFolderPath;
    private final Map<Integer, String> daAddresses;

    public SystemConfigurer(SystemConfiguration config) {
        this.endTime = config.endTime;
        this.outputFolderPath = config.outputFolderPath;
        this.daAddresses = config.daAddresses;

        createIfNotExists(outputFolderPath);
    }


    @Override
    public void run() {
        ObjectMapper mapper = new ObjectMapper();
        logger.info("Starting key-param generation");
//        KeyGenerationParametersImpl params = new KeyGenerationParametersImpl(1024, 50);
        PersistedKeyParameters params = new PersistedKeyParameters(
                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1 29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245 E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F 83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D 670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9 DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510 15728E5A 8AACAA68 FFFFFFFF FFFFFFFF",
                "2"
        );//TODO: Why can't we use generate them ourselves?
        String gHex = new String(Hex.encode(params.getGenerator().toByteArray()));
        String pHex = new String(Hex.encode(params.getPrimePair().getP().toByteArray()));

        List<DecryptionAuthorityInfo> daInfos = daAddresses.entrySet()
                .stream().map(e -> new DecryptionAuthorityInfo(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        DecryptionAuthorityInput daInput = new DecryptionAuthorityInput(pHex, gHex, endTime, daInfos);

        logger.info("Writing file");
        try (OutputStream ous = Files.newOutputStream(outputFolderPath.resolve("common_input.json"))) {
            mapper.writeValue(ous, daInput);
        } catch (IOException e) {
            logger.error("Failed to write common input to file. Terminating", e);
            System.exit(-1);
            return;
        }

        logger.info("Finished.");
    }

    private void createIfNotExists(Path path) {
        File file = path.toFile();

        if (file.exists()) {
            return;
        }

        boolean mkdirs = file.mkdirs();
        if (!mkdirs) {
            logger.warn("Unable to create dir " + path.toAbsolutePath().toString());
        }
    }

    /**
     * Configuration class for the trusted dealer
     */
    public static class SystemConfiguration extends AbstractInstanceCreatingConfiguration<SystemConfigurer> {

        private final long endTime;
        private final Map<Integer, String> daAddresses;
        private final Path outputFolderPath;

        /**
         * Constructor for the Trusted Dealer configuration
         *
         * @param candidateListPath path to json file containing candidate information
         * @param daAddresses       Map linking ids to addresses for DAs
         * @param endTime           When the vote comes to an end. ms since January 1, 1970, 00:00:00 GMT
         */
        SystemConfiguration(
                Path candidateListPath,
                Map<Integer, String> daAddresses,
                long endTime) {
            super(SystemConfigurer.class);
            this.outputFolderPath = candidateListPath;
            this.daAddresses = daAddresses;
            this.endTime = endTime;
        }
    }
}
