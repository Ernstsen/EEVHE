package dk.mmj.eevhe.crypto.signature;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper class for certificate-related functionality
 */
public class CertificateHelper {

    /**
     * Reads a X.509 certificateHolder from a bytearray
     *
     * @param bytes the data
     * @return the certificateHolder
     * @throws IOException if creating the CertificateHolder fails
     */
    public static X509CertificateHolder readCertificate(byte[] bytes) throws IOException {
        byte[] decode = Base64.decode(new String(bytes).replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", ""));
        return new X509CertificateHolder(decode);
    }

    /**
     * Reads a X.509 certificateHolder from a bytearray
     *
     * @param file the cert .pem file
     * @return the certificateHolder
     * @throws IOException if creating the CertificateHolder fails
     */
    private static X509CertificateHolder readCertificate(Path file) throws IOException {
        byte[] data = Files.readAllBytes(file);
        return readCertificate(data);
    }

    /**
     * Writes a X.509 certificateHolder to an outputStream as a .pem file
     *
     * @param output            the stream to write the certificate to
     * @param certificateHolder the certificate to serialize
     * @throws IOException if creating the CertificateHolder fails
     */
    public static void writeCertificate(OutputStream output, X509CertificateHolder certificateHolder) throws IOException {
        output.write(certificateToPem(certificateHolder).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Writes a cert to a .pem formatted string
     *
     * @param certificateHolder certificate to be writtes as .pem
     * @return Certificate om .pem format
     * @throws IOException if unable to get Encoded cert from certificateHolder
     */
    public static String certificateToPem(X509CertificateHolder certificateHolder) throws IOException {
        byte[] encoded = Base64.encode(certificateHolder.getEncoded());

        return "-----BEGIN CERTIFICATE-----\n" + System.lineSeparator()
                + new String(encoded).replaceAll("(.{65})", "$1" + System.lineSeparator())
                + "\n-----END CERTIFICATE-----";
    }

    /**
     * Loads the public-key from a certificate in a .pem file
     *
     * @param certificatePath path to the certificate, as a .pem file
     * @return public key from the certificate
     * @throws IOException if reading certificate fails
     */
    public static AsymmetricKeyParameter getPublicKeyFromCertificate(Path certificatePath) throws IOException {
        X509CertificateHolder cert = readCertificate(certificatePath);
        return getPublicKeyFromCertificate(cert);
    }


    /**
     * Extracts the public key from a certificate
     *
     * @param cert the certificate
     * @return public key from the certificate
     * @throws IOException if reading certificate fails
     */
    public static AsymmetricKeyParameter getPublicKeyFromCertificate(X509CertificateHolder cert) throws IOException {
        try {
            return PublicKeyFactory.createKey(cert.getSubjectPublicKeyInfo());
        } catch (Exception e) {
            return PublicKeyFactory.createKey(cert.getSubjectPublicKeyInfo().getPublicKeyData().getBytes());
        }
    }

    /**
     * Loads the public-key from a certificate in a .pem file
     *
     * @param data data containing the certificate in .pem format
     * @return public key from the certificate
     * @throws IOException if reading certificate fails
     */
    public static AsymmetricKeyParameter getPublicKeyFromCertificate(byte[] data) throws IOException {
        X509CertificateHolder cert = readCertificate(data);
        return getPublicKeyFromCertificate(cert);
    }
}
