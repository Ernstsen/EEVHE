package dk.mmj.eevhe.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.entities.PublicInfoList;
import dk.mmj.eevhe.entities.PublicInformationEntity;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.signers.RSADigestSigner;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.util.encoders.Base64;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility class for fetching information from the <i>BulletinBoard</i>
 */
public class FetchingUtilities {

    /**
     * Fetches a list of  {@link PublicInformationEntity}s from the BulletinBoard, supplied as a {@link WebTarget}.
     * <br></br>
     * Returns the first entity signed by the <i>Trusted Dealer</i>, null if none is found
     *
     * @param logger        logger for reporting errors
     * @param publicKeyName name of the file in the <i>RSA</i> folder, containing the public-key
     * @param target        webTarget pointing at the <i>BulletinBoard</i>
     * @return a public information signed by the Trusted Dealer, if any is found. Null otherwise
     */
    public static PublicInformationEntity fetchPublicInfo(Logger logger, String publicKeyName, WebTarget target) {
        Response response = target.path("getPublicInfo").request().buildGet().invoke();
        String responseString = response.readEntity(String.class);

        PublicInfoList publicInfoList;
        try {
            publicInfoList = new ObjectMapper().readValue(responseString, PublicInfoList.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize public informations list retrieved from bulletin board", e);
            System.exit(-1);
            return null;//Never happens
        }

        Optional<PublicInformationEntity> any = publicInfoList.getInformationEntities().stream()
                .filter(getVerifier(logger, publicKeyName))
                .findAny();

        if (!any.isPresent()) {
            logger.error("No public information retrieved from the server was signed by the trusted dealer. Terminating");
            System.exit(-1);
            return null;//Never happens
        }
        return any.get();
    }

    /**
     * Creates a predicate, returning true if the public keys loads without error,
     * and a given {@link PublicInformationEntity} is signed with the corresponding secret-key
     *
     * @param logger        logger for reporting errors
     * @param publicKeyName name of the file in the <i>RSA</i> folder, containing the public-key
     * @return Predicate returning true if a publicInformationEntity is verified, false otherwise
     */
    private static Predicate<PublicInformationEntity> getVerifier(Logger logger, String publicKeyName) {
        AsymmetricKeyParameter pk = loadPublicKey(logger, publicKeyName);

        if (pk == null) {
            return (info) -> false;
        }

        return informationEntity -> {
            RSADigestSigner digest = new RSADigestSigner(new SHA256Digest());
            digest.init(false, pk);
            informationEntity.updateSigner(digest);
            byte[] encodedSignature = informationEntity.getSignature().getBytes();

            return digest.verifySignature(Base64.decode(encodedSignature));
        };
    }

    /**
     * Loads public-key from disk
     *
     * @param logger        logger for reporting errors
     * @param publicKeyName name of the file in the <i>RSA</i> folder, containing the public-key
     * @return publicKey loaded from given file
     */
    private static AsymmetricKeyParameter loadPublicKey(Logger logger, String publicKeyName) {
        File keyFile = Paths.get("rsa").resolve(publicKeyName).toFile();
        if (!keyFile.exists()) {
            logger.error("Unable to locate RSA public key from Trusted Dealer");
            return null;
        }

        try {
            byte[] bytes = new byte[2048];
            int len = IOUtils.readFully(new FileInputStream(keyFile), bytes);
            byte[] actualBytes = Arrays.copyOfRange(bytes, 0, len);

            return PublicKeyFactory.createKey(Base64.decode(actualBytes));
        } catch (IOException e) {
            logger.error("Failed to load key from disk", e);
            return null;
        }
    }


}
