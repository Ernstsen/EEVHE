package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.FeldmanComplaintDTO;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.crypto.FeldmanVSSUtils.computeCoefficientCommitments;
import static dk.mmj.eevhe.crypto.FeldmanVSSUtils.verifyCommitmentRespected;

public class DishonestGennaroFeldmanVSS extends GennaroFeldmanVSS {
    static final String FELDMAN = "FeldmanVSS";
    private final BigInteger[] polynomial;
    private final boolean wrongCommitment;
    private final boolean noCommitment;
    private final boolean complainAgainstHonestParty;
    private final Set<Integer> honestParties = new HashSet<>();

    public DishonestGennaroFeldmanVSS(Broadcaster broadcaster, IncomingChannel incoming,
                                      Map<Integer, PeerCommunicator> peerCommunicatorMap,
                                      int id, ExtendedKeyGenerationParameters params, String logPrefix,
                                      BigInteger[] polynomial, Map<Integer, PartialSecretMessageDTO> secrets,
                                      boolean wrongCommitment, boolean noCommitment, boolean complainAgainstHonestParty) {
        super(broadcaster, incoming, peerCommunicatorMap, id, params, logPrefix, polynomial, secrets);
        this.wrongCommitment = wrongCommitment;
        this.noCommitment = noCommitment;
        this.complainAgainstHonestParty = complainAgainstHonestParty;
        if (secrets != null) {
            this.secrets = secrets;
        }
        this.polynomial = polynomial;
    }

    @Override
    public void startProtocol() {
        logger.info("Initialized Dishonest GennaroFeldmanVSS");

        honestParties.add(id);
        honestParties.addAll(peerMap.keySet());

        logger.debug("Calculating coefficient commitments");
        BigInteger[] commitment = computeCoefficientCommitments(g, p, polynomial);

        if (wrongCommitment) {
            commitment[1] = commitment[1].multiply(BigInteger.valueOf(12314)).mod(p);
        }
        if (noCommitment) {
            commitment = null;
        }

        logger.info("Broadcasting commitments");
        broadcaster.commit(new CommitmentDTO(commitment, id, FELDMAN));

        logger.debug("Finished first step of protocol");
    }

    @Override
    public boolean handleReceivedValues() {
        logger.info("Reading commitments");
        final List<CommitmentDTO> feldmanCommitments = broadcaster.getCommitments().stream()
                .filter(c -> FELDMAN.equals(c.getProtocol()))
                .collect(Collectors.toList());
        final List<CommitmentDTO> pedersenCommitments = broadcaster.getCommitments().stream()
                .filter(c -> PEDERSEN.equals(c.getProtocol()))
                .collect(Collectors.toList());

        for (CommitmentDTO commitment : feldmanCommitments) {
            this.feldmanCommitments.put(commitment.getId(), commitment.getCommitment());
        }
        for (CommitmentDTO commitment : pedersenCommitments) {
            this.pedersenCommitments.put(commitment.getId(), commitment.getCommitment());
        }

        logger.info("Verifying secret shares, using commitments");
        for (Map.Entry<Integer, PartialSecretMessageDTO> entry : new ArrayList<>(secrets.entrySet())) {
            int senderId = entry.getKey();
            if (senderId == id) {
                continue;
            }
            BigInteger partialSecret = entry.getValue().getPartialSecret1();

            BigInteger[] feldmanCommitment = this.feldmanCommitments.get(senderId);
            if (feldmanCommitment == null) {
                logger.error("Peer with id=" + senderId + ", had no corresponding commitment");
                complain(senderId);
                continue;
            }

            boolean matches = verifyCommitmentRespected(g, partialSecret, feldmanCommitment, BigInteger.valueOf(id), p, q);
            if (!matches) {
                complain(senderId);
            }

            // Complains about honest party with ID = 2
            if (complainAgainstHonestParty && senderId == 2) {
                complain(senderId);
            }
        }
        return true;
    }

    private void complain(int senderId) {
        logger.info("" + id + ": Sending complaint about Peer with ID=" + senderId);
        PartialSecretMessageDTO secret = secrets.get(senderId);
        final FeldmanComplaintDTO complaint = new FeldmanComplaintDTO(id, senderId,
                secret.getPartialSecret1(), secret.getPartialSecret2());
        broadcaster.feldmanComplain(complaint);
    }

    @Override
    public void handleComplaints() {
        super.handleComplaints();
    }

    @Override
    public void applyResolves() {
        super.applyResolves();
    }

    @Override
    public Set<Integer> getHonestParties() {
        return honestParties;
    }
}
