package dk.mmj.eevhe.initialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.Application;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParametersImpl;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.entities.DecryptionAuthorityInfo;
import dk.mmj.eevhe.entities.DecryptionAuthorityInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Helper class for creating configuration file(s) to be used in running the system
 */
public class SystemConfigurer implements Application {
    private static final Logger logger = LogManager.getLogger(SystemConfigurer.class);
    private final long endTime;
    private final Path outputFolderPath;
    private final Map<Integer, String> daAddresses;
    private final Path skFilePath;
    private final Path certFilePath;

    public SystemConfigurer(SystemConfiguration config) {
        this.endTime = config.endTime;
        this.outputFolderPath = config.outputFolderPath;
        this.skFilePath = config.skFilePath;
        this.certFilePath = config.skFilePath;
        this.daAddresses = config.daAddresses;
    }

    @Override
    public void run() {
        createIfNotExists(outputFolderPath);

        ObjectMapper mapper = new ObjectMapper();
        logger.info("Starting key-param generation");
        ExtendedKeyGenerationParameters params = new ExtendedKeyGenerationParametersImpl(1024, 50);
        String gHex = new String(Hex.encode(params.getGenerator().toByteArray()));
        String pHex = new String(Hex.encode(params.getPrimePair().getP().toByteArray()));
        String eHex = new String(Hex.encode(params.getGroupElement().toByteArray()));

        List<DecryptionAuthorityInfo> daInfos = daAddresses.entrySet()
                .stream().map(e -> new DecryptionAuthorityInfo(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        String certPem;
        try {
            certPem = new String(Files.readAllBytes(certFilePath));
        } catch (IOException e) {
            logger.error("Failed to read certificate file");
            return;
        }
        DecryptionAuthorityInput daInput = new DecryptionAuthorityInput(pHex, gHex, eHex, endTime, daInfos, certPem);

        logger.info("Writing file");
        try (OutputStream ous = Files.newOutputStream(outputFolderPath.resolve("common_input.json"))) {
            mapper.writeValue(ous, daInput);
        } catch (IOException e) {
            logger.error("Failed to write common input to file. Terminating", e);
            return;
        }

        logger.info("Loading global private key");
        AsymmetricKeyParameter globalSk;
        try {
            globalSk = KeyHelper.readKey(skFilePath);
        } catch (IOException e) {
            logger.error("Failed to load secretKey from disk", e);
            return;
        }

        AlgorithmIdentifier sha256WithRSASignature = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA");
        AlgorithmIdentifier digestSha = new DefaultDigestAlgorithmIdentifierFinder().find("SHA-256");

        logger.info("Writing certificates");
        try {
            for (DecryptionAuthorityInfo daInfo : daInfos) {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "BC");
                gen.initialize(2048, new SecureRandom());

                KeyPair keyPair = gen.generateKeyPair();
                PublicKey pk = keyPair.getPublic();
                PrivateKey sk = keyPair.getPrivate();

                X509v3CertificateBuilder cb = new X509v3CertificateBuilder(
                        new X500Name("CN=EEVHE_Configurer"),
                        BigInteger.valueOf(daInfo.getId()),
                        new Date(), new Date(endTime + (60 * 1000)),
                        new X500Name("CN=DA" + daInfo.getId()),
                        new SubjectPublicKeyInfo(sha256WithRSASignature, pk.getEncoded())
                );

                ContentSigner signer = new BcRSAContentSignerBuilder(
                        sha256WithRSASignature,
                        digestSha
                ).build(globalSk);

                X509CertificateHolder certificate = cb.build(signer);

                Path targetFile = outputFolderPath.resolve("DA" + daInfo.getId() + ".zip");
                try (ZipOutputStream ous = new ZipOutputStream(Files.newOutputStream(targetFile))) {
                    ous.putNextEntry(new ZipEntry("sk.pem"));
                    ous.write(sk.getEncoded());
                    ous.putNextEntry(new ZipEntry("cert.pem"));
                    CertificateHelper.writeCertificate(ous, certificate);
                }
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | OperatorCreationException | IOException e) {
            logger.error("Failed to create certificates for DAs!", e);
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
        private final Path skFilePath;
        private final Path certFilePath;

        /**
         * Constructor for the Trusted Dealer configuration
         *
         * @param candidateListPath path to json file containing candidate information
         * @param skFilePath        path to certificate private key as .pem file
         * @param certFilePath      path to certificate as .pem file
         * @param daAddresses       Map linking ids to addresses for DAs
         * @param endTime           When the vote comes to an end. ms since January 1, 1970, 00:00:00 GMT
         */
        SystemConfiguration(
                Path candidateListPath,
                Path skFilePath,
                Path certFilePath,
                Map<Integer, String> daAddresses,
                long endTime) {
            super(SystemConfigurer.class);
            this.outputFolderPath = candidateListPath;
            this.skFilePath = skFilePath;
            this.certFilePath = certFilePath;
            this.daAddresses = daAddresses;
            this.endTime = endTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public Map<Integer, String> getDaAddresses() {
            return daAddresses;
        }

        public Path getOutputFolderPath() {
            return outputFolderPath;
        }

        public Path getSkFilePath() {
            return skFilePath;
        }

        public Path getCertFilePath() {
            return certFilePath;
        }
    }
}
