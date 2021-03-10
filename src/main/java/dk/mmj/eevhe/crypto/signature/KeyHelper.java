package dk.mmj.eevhe.crypto.signature;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper class for key-related functionality
 */
public class KeyHelper {


    /**
     * Reads key from byte[]
     *
     * @param data Base64 encoded data - SecretKey on .pem form
     * @return Private/Secret key
     * @throws IOException if one is thrown by {@link PrivateKeyFactory}
     */
    public static AsymmetricKeyParameter readKey(byte[] data) throws IOException {
        byte[] decode = Base64.decode(new String(data)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", ""));
        return PrivateKeyFactory.createKey(decode);
    }

    /**
     * Reads key from file
     *
     * @param keyFile SecretKey on .pem form
     * @return Private/Secret key
     * @throws IOException if one is thrown by {@link PrivateKeyFactory}
     */

    public static AsymmetricKeyParameter readKey(Path keyFile) throws IOException {
        byte[] data = Files.readAllBytes(keyFile);
        return readKey(data);
    }
}
