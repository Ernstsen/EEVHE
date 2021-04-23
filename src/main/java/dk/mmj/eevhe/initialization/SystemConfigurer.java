package dk.mmj.eevhe.initialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.Application;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParametersImpl;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.entities.BBInput;
import dk.mmj.eevhe.entities.BBPeerInfo;
import dk.mmj.eevhe.entities.DecryptionAuthorityInput;
import dk.mmj.eevhe.entities.PeerInfo;
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
import java.security.*;
import java.util.ArrayList;
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
    private final Map<Integer, String> bbPeerAddresses;
    private final Path skFilePath;
    private final Path certFilePath;

    public SystemConfigurer(SystemConfiguration config) {
        this.endTime = config.endTime;
        this.outputFolderPath = config.outputFolderPath;
        this.skFilePath = config.skFilePath;
        this.certFilePath = config.certFilePath;
        this.daAddresses = config.daAddresses;
        this.bbPeerAddresses = config.bbPeerAddresses;
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

        List<PeerInfo> daInfos = daAddresses.entrySet()
                .stream().map(e -> new PeerInfo(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        String certPem;
        try {
            certPem = new String(Files.readAllBytes(certFilePath));
        } catch (IOException e) {
            logger.error("Failed to read certificate file");
            return;
        }
        DecryptionAuthorityInput daInput = new DecryptionAuthorityInput(pHex, gHex, eHex, endTime, daInfos, certPem);

        logger.info("Writing common_input.json file");
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

        ArrayList<BBPeerInfo> bbPeerInfos = new ArrayList<>();

        logger.info("Writing certificates");
        try {
            Date notAfter = new Date(endTime + (60 * 1000));
            for (PeerInfo daInfo : daInfos) {
                KeyPair keyPair = KeyHelper.generateRSAKeyPair();
                PublicKey pk = keyPair.getPublic();
                PrivateKey sk = keyPair.getPrivate();

                X509CertificateHolder certificate = generateCertificate(globalSk, notAfter, daInfo.getId(), pk, "DA");

                Path targetFile = outputFolderPath.resolve("DA" + daInfo.getId() + ".zip");
                try (ZipOutputStream ous = new ZipOutputStream(Files.newOutputStream(targetFile))) {
                    ous.putNextEntry(new ZipEntry("sk.pem"));
                    KeyHelper.writeKey(ous, sk.getEncoded());
                    ous.putNextEntry(new ZipEntry("cert.pem"));
                    CertificateHelper.writeCertificate(ous, certificate);
                }
            }

            for (Integer id : bbPeerAddresses.keySet()) {
                KeyPair keyPair = KeyHelper.generateRSAKeyPair();
                PublicKey pk = keyPair.getPublic();
                PrivateKey sk = keyPair.getPrivate();

                X509CertificateHolder certificate = generateCertificate(globalSk, notAfter, id, pk, "BB_peer");

                Path targetFile = outputFolderPath.resolve("BB_peer" + id + ".zip");
                try (ZipOutputStream ous = new ZipOutputStream(Files.newOutputStream(targetFile))) {
                    ous.putNextEntry(new ZipEntry("sk.pem"));
                    KeyHelper.writeKey(ous, sk.getEncoded());
                    bbPeerInfos.add(new BBPeerInfo(id, bbPeerAddresses.get(id), CertificateHelper.certificateToPem(certificate)));
                }
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | OperatorCreationException | IOException e) {
            logger.error("Failed to create certificates for DAs!", e);
            return;
        }

        BBInput bbInput = new BBInput(bbPeerInfos, new ArrayList<>());
        logger.info("Writing BB_input.json file");
        try (OutputStream ous = Files.newOutputStream(outputFolderPath.resolve("BB_input.json"))) {
            mapper.writeValue(ous, bbInput);
        } catch (IOException e) {
            logger.error("Failed to write BB input to file. Terminating", e);
            return;
        }

        logger.info("Finished.");
    }

    /**
     * Certificate generation method
     *
     * @param globalSk Secret key for global signing certificate
     * @param notAfter Expiration time for generated certificate
     * @param id       ID of certificate owner
     * @param pk       Public key for certificate
     * @param prefix   Prefix for certificate name
     * @return Signed certificate
     * @throws OperatorCreationException If generation fails
     */
    private X509CertificateHolder generateCertificate(
            AsymmetricKeyParameter globalSk,
            Date notAfter,
            int id,
            PublicKey pk,
            String prefix) throws OperatorCreationException {
        AlgorithmIdentifier sha256WithRSASignature = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA");
        AlgorithmIdentifier digestSha = new DefaultDigestAlgorithmIdentifierFinder().find("SHA-256");

        X509v3CertificateBuilder cb = new X509v3CertificateBuilder(
                new X500Name("CN=EEVHE_Configurer"),
                BigInteger.valueOf(id),
                new Date(), notAfter,
                new X500Name("CN=" + prefix + id),
                new SubjectPublicKeyInfo(sha256WithRSASignature, pk.getEncoded())
        );

        ContentSigner signer = new BcRSAContentSignerBuilder(
                sha256WithRSASignature,
                digestSha
        ).build(globalSk);

        X509CertificateHolder certificate = cb.build(signer);
        return certificate;
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
        private final Map<Integer, String> bbPeerAddresses;
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
         * @param bbPeerAddresses   Map linking ids to addresses for BB Peers
         * @param endTime           When the vote comes to an end. ms since January 1, 1970, 00:00:00 GMT
         */
        SystemConfiguration(
                Path candidateListPath,
                Path skFilePath,
                Path certFilePath,
                Map<Integer, String> daAddresses,
                Map<Integer, String> bbPeerAddresses,
                long endTime) {
            super(SystemConfigurer.class);
            this.outputFolderPath = candidateListPath;
            this.skFilePath = skFilePath;
            this.certFilePath = certFilePath;
            this.daAddresses = daAddresses;
            this.bbPeerAddresses = bbPeerAddresses;
            this.endTime = endTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public Map<Integer, String> getDaAddresses() {
            return daAddresses;
        }

        public Map<Integer, String> getBbPeerAddresses() {
            return bbPeerAddresses;
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
