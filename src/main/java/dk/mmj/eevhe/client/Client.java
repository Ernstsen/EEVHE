package dk.mmj.eevhe.client;

import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.Application;
import dk.mmj.eevhe.entities.Candidate;
import dk.mmj.eevhe.entities.PartialPublicInfo;
import dk.mmj.eevhe.entities.PublicKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyWebTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;

public abstract class Client implements Application {
    private static final String PUBLIC_KEY_NAME = "rsa";
    private static final Logger logger = LogManager.getLogger(Client.class);
    protected JerseyWebTarget target;
    private PublicKey publicKey;
    private PartialPublicInfo publicInfo;

    public Client(ClientConfiguration<?> configuration) {
        target = configureWebTarget(logger, configuration.targetUrl);
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
        List<PartialPublicInfo> publicInfos = FetchingUtilities.getPublicInfos(logger, target);

        HashMap<PublicKey, Integer> pkCount = new HashMap<>();
        for (PartialPublicInfo info : publicInfos) {
            pkCount.compute(info.getPublicKey(), (pk, v) -> v != null ? v + 1 : 1);
        }

        int t = publicInfos.size() / 2;
        for (Map.Entry<PublicKey, Integer> e : pkCount.entrySet()) {
            if (e.getValue() > t) {
                return this.publicKey = e.getKey();
            }
        }

        logger.error("Failed to find valid Public-key");
        return null;
    }

    /**
     * @return the list of candidates in the election
     */
    protected List<Candidate> getCandidates() {
        PartialPublicInfo info = fetchPublicInfo();

        return info.getCandidates();
    }

    protected PartialPublicInfo fetchPublicInfo() {
        if (publicInfo != null) {
            return publicInfo;
        }

        return publicInfo = FetchingUtilities.fetchPublicInfo(logger, PUBLIC_KEY_NAME, target);
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
