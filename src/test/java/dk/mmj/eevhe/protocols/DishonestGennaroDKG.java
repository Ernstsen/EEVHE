package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.entities.PartialKeyPair;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGBroadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGIncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGPeerCommunicator;

import java.util.*;

import static java.util.concurrent.TimeUnit.SECONDS;

public class DishonestGennaroDKG extends GennaroDKG {
    private final boolean wrongCommitment;
    private final boolean noCommitment;
    private final boolean complainAgainstHonestParty;

    /**
     * @param broadcaster         Broadcaster instance
     * @param incoming            Incoming messages being sent to Peer
     * @param peerCommunicatorMap A mapping of DA ID's and a PeerCommunicator
     * @param id                  DA ID
     * @param params              Key Generation Parameters: (g, p, q)
     * @param logPrefix           Prefix used for logging
     */
    public DishonestGennaroDKG(DKGBroadcaster broadcaster, DKGIncomingChannel incoming,
                               Map<Integer, DKGPeerCommunicator> peerCommunicatorMap,
                               int id, ExtendedKeyGenerationParameters params, String logPrefix,
                               boolean wrongCommitment, boolean noCommitment, boolean complainAgainstHonestParty) {
        super(broadcaster, incoming, peerCommunicatorMap, id, params, logPrefix);
        this.wrongCommitment = wrongCommitment;
        this.noCommitment = noCommitment;
        this.complainAgainstHonestParty = complainAgainstHonestParty;
    }

    @Override
    public List<Step> getSteps() {
        return super.getSteps();
    }

    @Override
    public PartialKeyPair output() {
        return super.output();
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
        honestPartiesPedersen = pedersenVSS.getHonestParties();
        honestPartiesFeldman = new HashSet<>();

        final Map<Integer, DKGPeerCommunicator> honestPeers = new HashMap<>();
        final Map<Integer, PartialSecretMessageDTO> secretsPedersen = pedersenVSS.getSecrets();

        DishonestGennaroFeldmanVSS feldmanVSS = new DishonestGennaroFeldmanVSS(broadcaster, incoming,
                honestPeers, id, params, logPrefix, pol1, secretsPedersen,
                wrongCommitment, noCommitment, complainAgainstHonestParty);

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

    @Override
    PartialKeyPair computeKeyPair(DKGBroadcaster broadcaster, Set<Integer> honestParties, GennaroFeldmanVSS feldmanVSS) {
        return super.computeKeyPair(broadcaster, honestParties, feldmanVSS);
    }

    private void setResult(PartialKeyPair res) {
        logger.info("Output has ben set");
        super.output = res;
    }

    public Set<Integer> getHonestPartiesPedersen() {
        return honestPartiesPedersen;
    }

    public Set<Integer> getHonestPartiesFeldman() {
        return honestPartiesFeldman;
    }
}
