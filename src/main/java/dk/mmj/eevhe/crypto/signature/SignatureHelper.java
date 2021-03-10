package dk.mmj.eevhe.crypto.signature;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.signers.RSADigestSigner;

import java.util.List;

/**
 * Helper class for signature-related functionality
 */
public class SignatureHelper {

    /**
     * Signs a list of elements
     *
     * @param sk       the secretKey used to create signature
     * @param elements list of elements, as byteArrays, to sign
     * @return computed signature
     */
    public static byte[] sign(AsymmetricKeyParameter sk, List<byte[]> elements) {
        RSADigestSigner signer = new RSADigestSigner(new SHA256Digest());
        signer.init(true, sk);
        for (byte[] element : elements) {
            signer.update(element, 0, element.length);
        }
        try {
            return signer.generateSignature();
        } catch (CryptoException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * Verifies a signature
     *
     * @param pk        publicKey with relation to the secretKey used to create signature
     * @param elements  elements that were signed
     * @param signature the signature to be verified
     * @return True if the signature is valid, False otherwise
     */
    public static boolean verifySignature(AsymmetricKeyParameter pk, List<byte[]> elements, byte[] signature) {
        RSADigestSigner signer = new RSADigestSigner(new SHA256Digest());
        signer.init(false, pk);
        for (byte[] element : elements) {
            signer.update(element, 0, element.length);
        }

        return signer.verifySignature(signature);
    }

}
