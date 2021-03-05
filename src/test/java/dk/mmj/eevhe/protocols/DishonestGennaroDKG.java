package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.entities.PartialKeyPair;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.*;

import static java.util.concurrent.TimeUnit.SECONDS;

public class DishonestGennaroDKG extends GennaroDKG {
    private final Broadcaster broadcaster;
    private final IncomingChannel incoming;
    private final Map<Integer, PeerCommunicator> peerMap;
    private final Logger logger;
    private final int id;
    private final ExtendedKeyGenerationParameters params;
    private final String logPrefix;
    private final boolean wrongCommitment;
    private final boolean complainAgainstHonestParty;
    private PartialKeyPair output;


    /**
     * @param broadcaster         Broadcaster instance
     * @param incoming            Incoming messages being sent to Peer
     * @param peerCommunicatorMap A mapping of DA ID's and a PeerCommunicator
     * @param id                  DA ID
     * @param params              Key Generation Parameters: (g, p, q)
     * @param logPrefix           Prefix used for logging
     */
    public DishonestGennaroDKG(Broadcaster broadcaster, IncomingChannel incoming,
                               Map<Integer, PeerCommunicator> peerCommunicatorMap,
                               int id, ExtendedKeyGenerationParameters params, String logPrefix,
                               boolean wrongCommitment, boolean complainAgainstHonestParty) {
        super(broadcaster, incoming, peerCommunicatorMap, id, params, logPrefix);
        this.broadcaster = broadcaster;
        this.incoming = incoming;
        this.peerMap = peerCommunicatorMap;
        this.id = id;
        this.params = params;
        this.logPrefix = logPrefix;
        this.wrongCommitment = wrongCommitment;
        this.complainAgainstHonestParty = complainAgainstHonestParty;

        logger = LogManager.getLogger(PedersenVSS.class.getName() + ". " + logPrefix + ":");
    }

    @Override
    public List<Step> getSteps() {
        return super.getSteps();
    }

    @Override
    public PartialKeyPair output() {
        return output;
    }

    @Override
    List<Step> generationPhase() {
        return super.generationPhase();
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
        BigInteger[] pol1 = super.pol1;
        PedersenVSS pedersenVSS = super.pedersenVSS;

        final Set<Integer> honestPartiesPedersen = pedersenVSS.getHonestParties();
        final Map<Integer, PeerCommunicator> honestPeers = new HashMap<>();
        final Set<Integer> honestParties = new HashSet<>();
        final Map<Integer, PartialSecretMessageDTO> secretsPedersen = pedersenVSS.getSecrets();

        DishonestGennaroFeldmanVSS feldmanVSS = new DishonestGennaroFeldmanVSS(broadcaster, incoming,
                honestPeers, id, params, logPrefix, pol1, secretsPedersen, wrongCommitment, complainAgainstHonestParty);

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
                new Step(() -> honestParties.addAll(feldmanVSS.getHonestParties()), 0, SECONDS),
                new Step(() -> this.setResult(computeKeyPair(broadcaster, honestParties, feldmanVSS)), 0, SECONDS)
        );
    }

    @Override
    PartialKeyPair computeKeyPair(Broadcaster broadcaster, Set<Integer> honestParties, GennaroFeldmanVSS feldmanVSS) {
        return super.computeKeyPair(broadcaster, honestParties, feldmanVSS);
    }

    private void setResult(PartialKeyPair res) {
        logger.info("Output has ben set");
        this.output = res;
    }
}
