package dk.mmj.eevhe.client;

import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.Application;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.entities.Candidate;
import dk.mmj.eevhe.entities.PartialPublicInfo;
import dk.mmj.eevhe.entities.PublicKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.glassfish.jersey.client.JerseyWebTarget;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.mmj.eevhe.client.FetchingUtilities.getBBPeerCertificates;
import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;

public abstract class Client implements Application {
    private static final Logger logger = LogManager.getLogger(Client.class);
    protected final AsymmetricKeyParameter cert;
    protected JerseyWebTarget target;
    private PublicKey publicKey;
    private PartialPublicInfo publicInfo;
    private List<PartialPublicInfo> publicInfos;
    private List<String> certificates;

    public Client(ClientConfiguration<?> configuration) {
        target = configureWebTarget(logger, configuration.targetUrl);
        try {
            cert = CertificateHelper.getPublicKeyFromCertificate(Paths.get("certs/test_glob.pem"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load election certificate");
        }
    }

    /**
     * Fetches the public key by requesting it from the public servers "/publicKey" path.
     *
     * @return the response containing the Public Key.
     */
    protected PublicKey getPublicKey() {
        if (publicKey != null) {
            return publicKey;
        }

        return publicKey = fetchPublicInfo().getPublicKey();
    }

    /**
     * @return the list of candidates in the election
     */
    public List<Candidate> getCandidates() {
        PartialPublicInfo info = fetchPublicInfo();
        if (info == null) {
            return null;
        }
        return info.getCandidates();
    }

    /**
     * Fetches list of BB-peer certificates
     *
     * @return list of valid bb-peer certificates
     */
    protected List<String> getCertificates() {
        if(certificates != null){
            return certificates;
        }
        return certificates = getBBPeerCertificates(logger, target.path("certificates"), cert);
    }

    protected PartialPublicInfo fetchPublicInfo() {
        if (publicInfo != null) {
            return publicInfo;
        }

        List<PartialPublicInfo> publicInfos = FetchingUtilities.getPublicInfos(logger, target, cert);
        if (publicInfos == null) {
            return null;
        }

        HashMap<String, Integer> pkCount = new HashMap<>();
        for (PartialPublicInfo info : publicInfos) {
            pkCount.compute(
                    toComparable(info),
                    (pk, v) -> v != null ? v + 1 : 1
            );
        }

        int t = publicInfos.size() / 2;
        for (Map.Entry<String, Integer> e : pkCount.entrySet()) {
            if (e.getValue() > t) {
                String key = e.getKey();
                return this.publicInfo = publicInfos.stream()
                        .filter(info -> key.equals(toComparable(info)))
                        .findAny().orElse(null);
            }
        }

        logger.error("Failed to find valid Public-Information");
        return null;
    }

    /**
     * Extracts shared fields to a string, used in determining whether PartialPublicInfos agree
     *
     * @param info the information entity
     * @return deterministically computed string containing all information from the info which is shared between
     * information entities
     */
    private String toComparable(PartialPublicInfo info) {
        return info.getPublicKey().toString() + Arrays.toString(info.getCandidates().toArray()) + info.getEndTime();
    }

    protected List<PartialPublicInfo> fetchPublicInfos() {
        if (publicInfos != null) {
            return publicInfos;
        }

        return publicInfos = FetchingUtilities.getPublicInfos(logger, target, cert);
    }


    static abstract class ClientConfiguration<T extends Client> extends AbstractInstanceCreatingConfiguration<T> {
        private final String targetUrl;

        ClientConfiguration(Class<T> clazz, String targetUrl) {
            super(clazz);
            this.targetUrl = targetUrl;
        }

        public String getTargetUrl() {
            return targetUrl;
        }
    }
}
