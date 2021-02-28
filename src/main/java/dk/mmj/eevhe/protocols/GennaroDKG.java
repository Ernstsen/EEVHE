package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.PartialKeyPair;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.entities.PublicKey;
import dk.mmj.eevhe.protocols.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.interfaces.PeerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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
    private BigInteger partialSecret;


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
        partialSecret = pol1[0];

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

        Set<Integer> honestParties = feldmanVSS.getHonestParties();
        // If we aren't disqualified, we can compute the partial secret- and public-values.
        // TODO: // Change if statement logic since one self shouldn't be in it
        if (honestParties.contains(id)) {
            BigInteger partialSecretKey = feldmanVSS.output();
            BigInteger partialPublicKey = g.modPow(partialSecret, p);

            // Computes Y = prod_i y_i mod p
            //TODO: Move the following somewhere else??
            List<CommitmentDTO> commitments = broadcaster.getCommitments();
            List<BigInteger> partialPublicKeys = new ArrayList<>();
            for (CommitmentDTO commitment : commitments) {
                if (honestParties.contains(commitment.getId())) {
                    partialPublicKeys.add(commitment.getCommitment()[0]);
                }
            }
            BigInteger publicKey = partialPublicKeys
                    .stream().reduce(BigInteger::multiply).orElse(BigInteger.ZERO).mod(p);

            return new PartialKeyPair(partialSecretKey, partialPublicKey, new PublicKey(publicKey, g, q));
        }
        return null;
    }

    // TODO: This should not be accessible outside of testing?
    public BigInteger getPartialSecret() {
        return partialSecret;
    }
}
