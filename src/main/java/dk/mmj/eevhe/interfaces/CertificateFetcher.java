package dk.mmj.eevhe.interfaces;

import dk.mmj.eevhe.entities.CertificateDTO;
import dk.mmj.eevhe.entities.SignedEntity;

import java.io.IOException;
import java.util.List;

public interface CertificateFetcher {

    /**
     * Fetches certificates
     *
     * @return list of certificates on .pem form
     * @throws IOException if certificate parsing/deserialization fails
     */
    List<SignedEntity<CertificateDTO>> getCertificates() throws IOException;
}
