package dk.mmj.eevhe.initialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.SignatureHelper;
import dk.mmj.eevhe.entities.DecryptionAuthorityInfo;
import dk.mmj.eevhe.entities.DecryptionAuthorityInput;
import org.apache.commons.compress.utils.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.CertException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentVerifierProviderBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

public class TestSystemConfigurer {

    static {
        //Register BouncyCastle as SecurityProvider
        Security.addProvider(new BouncyCastleProvider());
    }

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

            Path da1zip = dirPath.resolve("DA1.zip");

            assertTrue("Should have created zip for DA1", Files.exists(dirPath.resolve("DA1.zip")));
            assertTrue("Should have created zip for DA2", Files.exists(dirPath.resolve("DA2.zip")));
            assertTrue("Should have created zip for DA3", Files.exists(dirPath.resolve("DA3.zip")));

            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(da1zip))) {
                ZipEntry nextEntry = zis.getNextEntry();
                assertNotNull("Should contain two entries", nextEntry);
                assertEquals("First entry should be secret key", nextEntry.getName(), "sk.pem");

                byte[] skBytes = IOUtils.toByteArray(zis);
                AsymmetricKeyParameter sk = PrivateKeyFactory.createKey(skBytes);
                assertNotNull(sk);

                nextEntry = zis.getNextEntry();
                assertNotNull("Should contain two entries", nextEntry);
                assertEquals("First entry should be certificate", nextEntry.getName(), "cert.pem");

                byte[] certBytes = IOUtils.toByteArray(zis);
                X509CertificateHolder cert = CertificateHelper.readCertificate(certBytes);
                assertTrue("certificate should be valid until after endTime", cert.isValidOn(new Date(configEndTime + 500)));
                assertEquals("Unexpected issuer", new X500Name("CN=EEVHE_Configurer"), cert.getIssuer());

                assertIsSigned(cert);
                assertSignAndVerify(sk, certBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not throw exception. " + e.getMessage());
        }
    }

    private void assertIsSigned(X509CertificateHolder cert) throws IOException, OperatorCreationException, CertException {
        AsymmetricKeyParameter pk = CertificateHelper.getPublicKeyFromCertificate(Paths.get("certs/test_glob.pem"));

        ContentVerifierProvider build = new BcRSAContentVerifierProviderBuilder(new DefaultDigestAlgorithmIdentifierFinder())
                .build(pk);
        assertTrue("Certificate should have been signed by global cert", cert.isSignatureValid(build));
    }

    private void assertSignAndVerify(AsymmetricKeyParameter sk, byte[] certBytes) throws IOException {
        String msg = "This is some message that i want signed";
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        List<byte[]> signedMessage = Collections.singletonList(msgBytes);
        byte[] signature = SignatureHelper.sign(sk, signedMessage);

        AsymmetricKeyParameter pk = CertificateHelper.getPublicKeyFromCertificate(certBytes);
        assertTrue("Signature should pass", SignatureHelper.verifySignature(pk, signedMessage, signature));
    }

    @After
    public void tearDown() throws IOException {
        Path path = Paths.get(conf);
        if (Files.exists(path)) {
            Files.delete(path.resolve("common_input.json"));
            Files.delete(path.resolve("DA1.zip"));
            Files.delete(path.resolve("DA2.zip"));
            Files.delete(path.resolve("DA3.zip"));
            Files.delete(path);
        }
    }

}
