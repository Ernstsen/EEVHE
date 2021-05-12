package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGBroadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGIncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGPeerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Parent class for VSS protocols, containing some necessary state, to avoid duplicate boilerplate code
 */
public class AbstractVSS {
    protected final DKGBroadcaster broadcaster;
    protected final DKGIncomingChannel incoming;
    protected final Map<Integer, DKGPeerCommunicator> peerMap;
    protected final int id;
    protected final BigInteger g;
    protected final BigInteger q;
    protected final BigInteger p;
    protected final BigInteger e;
    protected final Logger logger;
    protected Map<Integer, PartialSecretMessageDTO> secrets = new HashMap<>();


    public AbstractVSS(DKGBroadcaster broadcaster, DKGIncomingChannel incoming,
                       Map<Integer, DKGPeerCommunicator> peerCommunicatorMap,
                       int id, ExtendedKeyGenerationParameters params, String logPrefix) {
        this.broadcaster = broadcaster;
        this.incoming = incoming;
        this.peerMap = peerCommunicatorMap;
        this.id = id;
        this.g = params.getGenerator();
        this.q = params.getPrimePair().getQ();
        this.p = params.getPrimePair().getP();
        this.e = params.getGroupElement();
        logger = LogManager.getLogger(getClass().getName() + " " + logPrefix + ":");
    }
}
