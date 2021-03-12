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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;

public abstract class Client implements Application {
    private static final Logger logger = LogManager.getLogger(Client.class);
    protected final AsymmetricKeyParameter cert;
    protected JerseyWebTarget target;
    private PublicKey publicKey;
    private PartialPublicInfo publicInfo;
    private List<PartialPublicInfo> publicInfos;

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
    protected List<Candidate> getCandidates() {
        PartialPublicInfo info = fetchPublicInfo();
        if (info == null) {
            return null;
        }
        return info.getCandidates();
    }

    protected PartialPublicInfo fetchPublicInfo() {
        if (publicInfo != null) {
            return publicInfo;
        }

        List<PartialPublicInfo> publicInfos = FetchingUtilities.getPublicInfos(logger, target, cert);

        HashMap<PublicKey, Integer> pkCount = new HashMap<>();
        for (PartialPublicInfo info : publicInfos) {
            pkCount.compute(info.getPublicKey(), (pk, v) -> v != null ? v + 1 : 1);
        }

        int t = publicInfos.size() / 2;
        for (Map.Entry<PublicKey, Integer> e : pkCount.entrySet()) {
            if (e.getValue() > t) {
                PublicKey key = e.getKey();
                return this.publicInfo = publicInfos.stream()
                        .filter(i -> key.equals(i.getPublicKey()))
                        .findAny().orElse(null);
            }
        }

        logger.error("Failed to find valid Public-Information");
        return null;
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
