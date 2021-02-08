package dk.mmj.eevhe.client;

import dk.eSoftware.commandLineParser.Configuration;
import dk.mmj.eevhe.Application;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.entities.Candidate;
import dk.mmj.eevhe.entities.PublicInformationEntity;
import dk.mmj.eevhe.entities.PublicKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyWebTarget;

import java.math.BigInteger;
import java.util.List;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;

public abstract class Client implements Application {
    private static final String PUBLIC_KEY_NAME = "rsa";
    private static final Logger logger = LogManager.getLogger(Client.class);
    protected JerseyWebTarget target;
    private PublicKey publicKey;
    private PublicInformationEntity publicInfo;

    public Client(ClientConfiguration configuration) {
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
        PublicInformationEntity info = fetchPublicInfo();

        BigInteger h = SecurityUtils.combinePartials(info.getPublicKeys(), info.getP());

        return publicKey = new PublicKey(h, info.getG(), info.getQ());
    }

    /**
     * @return the list of candidates in the election
     */
    protected List<Candidate> getCandidates() {
        PublicInformationEntity info = fetchPublicInfo();

        return info.getCandidates();
    }

    protected PublicInformationEntity fetchPublicInfo() {
        if (publicInfo != null) {
            return publicInfo;
        }

        return publicInfo = FetchingUtilities.fetchPublicInfo(logger, PUBLIC_KEY_NAME, target);
    }


    static class ClientConfiguration implements Configuration {
        private final String targetUrl;

        ClientConfiguration(String targetUrl) {
            this.targetUrl = targetUrl;
        }
    }
}
