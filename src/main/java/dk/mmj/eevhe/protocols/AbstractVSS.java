package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParameters;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.interfaces.PeerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class AbstractVSS {
    protected final Broadcaster broadcaster;
    protected final IncomingChannel incoming;
    protected final Map<Integer, PeerCommunicator> peerMap;
    protected final Map<Integer, PartialSecretMessageDTO> secrets = new HashMap<>();
    protected final Map<Integer, BigInteger[]> commitments = new HashMap<>();
    protected final int id;
    protected final BigInteger g;
    protected final BigInteger q;
    protected final BigInteger p;
    protected final Logger logger;


    public AbstractVSS(Broadcaster broadcaster, IncomingChannel incoming,
                       Map<Integer, PeerCommunicator> peerCommunicatorMap,
                       int id, KeyGenerationParameters params, String logPrefix, String className){
        this.broadcaster = broadcaster;
        this.incoming = incoming;
        this.peerMap = peerCommunicatorMap;
        this.id = id;
        this.g = params.getGenerator();
        this.q = params.getPrimePair().getQ();
        this.p = params.getPrimePair().getP();

        logger = LogManager.getLogger(className + ". " + logPrefix + ":");
    }
}
