package dk.mmj.eevhe.client;

import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.Application;
import dk.mmj.eevhe.entities.Candidate;
import dk.mmj.eevhe.entities.PartialPublicInfo;
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

        PublicKey publishedPK = publicInfos.get(0).getPublicKey();

        BigInteger h = publicInfos.stream().map(PartialPublicInfo::getPartialPublicKey)
                .reduce(BigInteger::multiply).orElse(BigInteger.ONE).mod(publishedPK.getP());

        return this.publicKey = new PublicKey(h, publishedPK.getG(), publishedPK.getQ());
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

        ClientConfiguration(Class<T> clazz,String targetUrl) {
            super(clazz);
            this.targetUrl = targetUrl;
        }

        public String getTargetUrl() {
            return targetUrl;
        }
    }
}
