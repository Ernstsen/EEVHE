package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.connectors.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.DKG;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;

public class GennaroDKG implements DKG<PartialKeyPair> {
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
    private final int t;
    BigInteger[] pol1;
    PedersenVSS pedersenVSS;
    BigInteger partialSecret;
    private Set<Integer> honestPartiesPedersen;
    private Set<Integer> honestPartiesFeldman;
    private PartialKeyPair output;


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
        this.t = ((peerMap.size()) / 2);

        logger = LogManager.getLogger(PedersenVSS.class.getName() + ". " + logPrefix + ":");
    }


    @Override
    public List<Step> getSteps() {
        ArrayList<Step> steps = new ArrayList<>();
        steps.addAll(generationPhase());
        steps.addAll(extractionPhase());
        return steps;
    }

    @Override
    public PartialKeyPair output() {
        return output;
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
    List<Step> generationPhase() {
        BigInteger[] pol2 = SecurityUtils.generatePolynomial(t, q);
        pol1 = SecurityUtils.generatePolynomial(t, q);
        partialSecret = pol1[0];
        this.pedersenVSS = new PedersenVSS(broadcaster, incoming, peerMap, id, params, logPrefix, pol1, pol2);

        return Arrays.asList(
                new Step(pedersenVSS::startProtocol, 0, SECONDS),
                new Step(pedersenVSS::handleReceivedValues, 10, SECONDS),
                new Step(pedersenVSS::handleComplaints, 10, SECONDS),
                new Step(pedersenVSS::applyResolves, 10, SECONDS)
        );
    }

    /**
     * Extraction phase of the Gennaro-DKG.
     * <p>
     * - Initializes an instance of Feldman-VSS with the honest peers from the generation phase
     * - Receives and checks the secret shares again
     * - Resolves complaints
     * - Generates output
     */
    List<Step> extractionPhase() {
        honestPartiesPedersen = pedersenVSS.getHonestParties();
        honestPartiesFeldman = new HashSet<>();
        final Map<Integer, PeerCommunicator> honestPeers = new HashMap<>();
        final Map<Integer, PartialSecretMessageDTO> secretsPedersen = pedersenVSS.getSecrets();

        GennaroFeldmanVSS feldmanVSS = new GennaroFeldmanVSS(broadcaster, incoming,
                honestPeers, id, params, logPrefix, pol1, secretsPedersen);

        return Arrays.asList(
                new Step(
                        () -> peerMap.entrySet()
                                .stream().filter(e -> honestPartiesPedersen.contains(e.getKey()))
                                .forEach(e -> honestPeers.put(e.getKey(), e.getValue())),
                        0, SECONDS
                ),
                new Step(feldmanVSS::startProtocol, 0, SECONDS),
                new Step(feldmanVSS::handleReceivedValues, 10, SECONDS),
                new Step(feldmanVSS::handleComplaints, 10, SECONDS),
                new Step(() -> honestPartiesFeldman.addAll(feldmanVSS.getHonestParties()), 0, SECONDS),
                new Step(() -> this.setResult(computeKeyPair(broadcaster, honestPartiesFeldman, feldmanVSS)), 0, SECONDS)
        );
    }

    PartialKeyPair computeKeyPair(Broadcaster broadcaster, Set<Integer> honestParties, GennaroFeldmanVSS feldmanVSS) {
        logger.info("Computing PartialKeyPair");
        BigInteger partialSecretKey = feldmanVSS.output();
        BigInteger partialPublicKey = g.modPow(partialSecretKey, p);

        // Computes Y = prod_i y_i mod p
        List<CommitmentDTO> commitments = broadcaster.getCommitments().stream()
                .filter(c -> GennaroFeldmanVSS.FELDMAN.equals(c.getProtocol()))
                .collect(Collectors.toList());

        List<BigInteger> partialPublicKeys = new ArrayList<>();
        for (CommitmentDTO commitment : commitments) {
            if (honestParties.contains(commitment.getId())) {
                partialPublicKeys.add(commitment.getCommitment()[0]);
            }
        }

        BigInteger publicKey = partialPublicKeys
                .stream().reduce(BigInteger::multiply).orElse(BigInteger.ZERO).mod(p);
        return new PartialKeyPair(
                new PartialSecretKey(partialSecretKey, p),
                partialPublicKey,
                new PublicKey(publicKey, g, q)
        );
    }

    private void setResult(PartialKeyPair res) {
        logger.info("Output has ben set");
        this.output = res;
    }

    public Set<Integer> getHonestPartiesPedersen() {
        return honestPartiesPedersen;
    }

    public Set<Integer> getHonestPartiesFeldman() {
        return honestPartiesFeldman;
    }
}
