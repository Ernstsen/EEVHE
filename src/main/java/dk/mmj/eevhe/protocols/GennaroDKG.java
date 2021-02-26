package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.entities.PartialKeyPair;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.interfaces.PeerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.crypto.PedersenVSSUtils.generateElementInSubgroup;

public class GennaroDKG {
    private final Broadcaster broadcaster;
    private final IncomingChannel incoming;
    private final Map<Integer, PeerCommunicator> peerMap;
    private final Logger logger;
    private final int id;
    private final ExtendedKeyGenerationParameters params;
    private final String logPrefix;
    private final BigInteger g;
    private final BigInteger q;
    private final BigInteger p;
    private final BigInteger e;
    private final int t;
    private BigInteger[] pol1;
    private PedersenVSS pedersenVSS;


    /**
     * @param broadcaster         Broadcaster instance
     * @param incoming            Incoming messages being sent to Peer
     * @param peerCommunicatorMap A mapping of DA ID's and a PeerCommunicator
     * @param id                  DA ID
     * @param params              Key Generation Parameters: (g, p, q)
     * @param logPrefix           Prefix used for logging
     */
    public GennaroDKG(Broadcaster broadcaster, IncomingChannel incoming,
                      Map<Integer, PeerCommunicator> peerCommunicatorMap,
                      int id, ExtendedKeyGenerationParameters params, String logPrefix) {
        this.broadcaster = broadcaster;
        this.incoming = incoming;
        this.peerMap = peerCommunicatorMap;
        this.id = id;
        this.params = params;
        this.logPrefix = logPrefix;
        this.g = params.getGenerator();
        this.q = params.getPrimePair().getQ();
        this.p = params.getPrimePair().getP();
        this.e = generateElementInSubgroup(g, p);
        this.t = ((peerMap.size()) / 2);

        logger = LogManager.getLogger(PedersenVSS.class.getName() + ". " + logPrefix + ":");

    }

    /**
     * Generation phase of the Gennaro-DKG.
     * <p>
     * - Initializes two random polynomials
     * - Initializes an instance of Pedersen-VSS
     * - Generates and distributes secret shares
     * - Receives and checks shares from other peers
     * - Resolves complaints
     */
    public void generationPhase() {
        pol1 = SecurityUtils.generatePolynomial(t, q);
        BigInteger[] pol2 = SecurityUtils.generatePolynomial(t, q);

        pedersenVSS = new PedersenVSS(broadcaster, incoming, peerMap, id, params, logPrefix, pol1, pol2);

        pedersenVSS.startProtocol();
        pedersenVSS.handleReceivedValues();
        pedersenVSS.handleComplaints();
        pedersenVSS.applyResolves();

    }

    /**
     * Extraction phase of the Gennaro-DKG.
     * <p>
     * - Initializes an instance of Feldman-VSS with the honest peers from the generation phase
     * - Receives and checks the secret shares again
     * - Resolves complaints
     *
     * @return PartialKeyPair (partialSecretKey, partialPublicKey, publicKey)
     */
    public PartialKeyPair extractionPhase() {
        final Set<Integer> honestPartiesPedersen = pedersenVSS.getHonestParties();
        Map<Integer, PeerCommunicator> honestPeers = peerMap.entrySet()
                .stream().filter(e -> honestPartiesPedersen.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final Map<Integer, PartialSecretMessageDTO> secretsPedersen = pedersenVSS.getSecrets();
        FeldmanVSS feldmanVSS = new FeldmanVSS(broadcaster, incoming,
                honestPeers, id, params, logPrefix, pol1, secretsPedersen);

        feldmanVSS.startProtocol();
        feldmanVSS.handleReceivedValues();
        feldmanVSS.handleComplaints();
        feldmanVSS.applyResolves();

        // TODO: Reconstruct keys
        final Map<Integer, PartialSecretMessageDTO> secretsFeldman = feldmanVSS.getSecrets();

        // TODO: IMPLEMENT PROTOCOL
        return null;
    }
}
