package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.PedersenVSSUtils;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.FeldmanComplaintDTO;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.VSS;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.crypto.FeldmanVSSUtils.computeCoefficientCommitments;
import static dk.mmj.eevhe.crypto.FeldmanVSSUtils.verifyCommitmentRespected;
import static dk.mmj.eevhe.protocols.PedersenVSS.PEDERSEN;

public class GennaroFeldmanVSS extends AbstractVSS implements VSS {
    static final String FELDMAN = "FeldmanVSS";
    protected final Map<Integer, BigInteger[]> feldmanCommitments = new HashMap<>();
    protected final Map<Integer, BigInteger[]> pedersenCommitments = new HashMap<>();
    final BigInteger[] polynomial;
    private final BigInteger e;
    protected final Set<Integer> honestParties = new HashSet<>();


    public GennaroFeldmanVSS(Broadcaster broadcaster, IncomingChannel incoming,
                             Map<Integer, PeerCommunicator> peerCommunicatorMap,
                             int id, ExtendedKeyGenerationParameters params, String logPrefix,
                             BigInteger[] polynomial, Map<Integer, PartialSecretMessageDTO> secrets) {
        super(broadcaster, incoming, peerCommunicatorMap, id, params, logPrefix);
        this.e = params.getGroupElement();
        if (secrets != null) {
            this.secrets = secrets;
        }
        this.polynomial = polynomial;
    }

    @Override
    public void startProtocol() {
        logger.info("Initialized GennaroFeldmanVSS");

        honestParties.add(id);
        honestParties.addAll(peerMap.keySet());

        logger.debug("Calculating coefficient commitments");
        BigInteger[] commitment = computeCoefficientCommitments(g, p, polynomial);

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

        honestParties.removeIf(i -> !this.feldmanCommitments.containsKey(i) || !this.pedersenCommitments.containsKey(i));

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
                honestParties.remove(senderId);
                continue;
            }

            boolean matches = verifyCommitmentRespected(g, partialSecret, feldmanCommitment, BigInteger.valueOf(id), p, q);
            if (!matches) {
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
        logger.info("Fetching complaints");
        List<FeldmanComplaintDTO> complaints = broadcaster.getFeldmanComplaints();

        logger.debug("Received " + complaints.size() + " complaints");
        for (FeldmanComplaintDTO complaint : complaints) {
            // Check 1
            BigInteger[] pedersenCommitment = pedersenCommitments.get(complaint.getTargetId());
            BigInteger[] feldmanCommitment = feldmanCommitments.get(complaint.getTargetId());
            BigInteger partialSecret1 = complaint.getVal1();
            BigInteger partialSecret2 = complaint.getVal2();

            BigInteger complainerId = BigInteger.valueOf(complaint.getSenderId());
            boolean matches1 = PedersenVSSUtils.verifyCommitmentRespected(g, e,
                    partialSecret1, partialSecret2, pedersenCommitment, complainerId, p, q);
            // Check 2
            boolean matches2 = verifyCommitmentRespected(g, partialSecret1, feldmanCommitment, complainerId, p, q);

            if (matches1 && !matches2) {
                logger.info("Removing party with ID " + complaint.getTargetId() + " from honest parties");
                honestParties.remove(complaint.getTargetId());
            }
        }
    }

    @Override
    public void applyResolves() {
    }

    @Override
    public BigInteger output() {
        logger.info("Combining values, to make partial secret key");
        // Returns value x_i
        return secrets.values().stream()
                .filter(e -> honestParties.contains(e.getSender()))
                .map(PartialSecretMessageDTO::getPartialSecret1)
                .reduce(BigInteger::add).orElse(BigInteger.ZERO).mod(q);
    }

    public Set<Integer> getHonestParties() {
        return honestParties;
    }

}
