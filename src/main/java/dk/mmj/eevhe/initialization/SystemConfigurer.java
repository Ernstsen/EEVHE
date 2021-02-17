package dk.mmj.eevhe.initialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.Configuration;
import dk.mmj.eevhe.Application;
import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParametersImpl;
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
        logger.info("Starting key generation");
        KeyGenerationParametersImpl params = new KeyGenerationParametersImpl(1024, 50);
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
    public static class SystemConfiguration implements Configuration {

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
            this.outputFolderPath = candidateListPath;
            this.daAddresses = daAddresses;
            this.endTime = endTime;
        }
    }
}
