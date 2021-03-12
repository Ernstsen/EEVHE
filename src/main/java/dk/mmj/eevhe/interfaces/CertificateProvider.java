package dk.mmj.eevhe.interfaces;

import java.util.Map;

public interface CertificateProvider {

    /**
     * @return map between daIds and the map as a .pem formatted string
     */
    Map<Integer, String> generateCertMap();
}
