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

public class GennaroFeldmanVSS extends AbstractVSS implements VSS {
    static final String FELDMAN = "FeldmanVSS";
    private final BigInteger[] polynomial;
    private final BigInteger e;
    private final Set<Integer> honestParties = new HashSet<>();

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
        logger.info("Initialized FeldmanVSS");

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
        final List<CommitmentDTO> commitments = broadcaster.getCommitments().stream()
                .filter(c -> FELDMAN.equals(c.getProtocol()))
                .collect(Collectors.toList());

        logger.info("Verifying secret shares, using commitments");
        for (CommitmentDTO commitment : commitments) {
            this.commitments.put(commitment.getId(), commitment.getCommitment());
        }

        for (Map.Entry<Integer, PartialSecretMessageDTO> entry : new ArrayList<>(secrets.entrySet())) {
            int senderId = entry.getKey();
            BigInteger partialSecret = entry.getValue().getPartialSecret1();


            BigInteger[] commitment = this.commitments.get(senderId);
            if (commitment == null) {
                logger.error("Peer with id=" + senderId + ", had no corresponding commitment!");
                continue;//TODO: What to do?
            }

            boolean matches = verifyCommitmentRespected(g, partialSecret, commitment, BigInteger.valueOf(id), p, q);
            if (!matches) {
                logger.info("" + id + ": Sending complaint about Peer with ID=" + senderId);
                PartialSecretMessageDTO secret = secrets.get(senderId);
                final FeldmanComplaintDTO complaint = new FeldmanComplaintDTO(id, senderId,
                        secret.getPartialSecret1(), secret.getPartialSecret2());
                broadcaster.feldmanComplain(complaint);
            }
        }

        return true;
    }

    @Override
    public void handleComplaints() {
        logger.info("Fetching complaints");
        List<FeldmanComplaintDTO> complaints = broadcaster.getFeldmanComplaints();

        logger.debug("Received " + complaints.size() + " complaints");
        for (FeldmanComplaintDTO complaint : complaints) {
            // Check 1
            BigInteger[] commitment = commitments.get(complaint.getTargetId());
            BigInteger partialSecret1 = complaint.getVal1();
            BigInteger partialSecret2 = complaint.getVal2();

            BigInteger complainerId = BigInteger.valueOf(complaint.getSenderId());
            boolean matches1 = PedersenVSSUtils.verifyCommitmentRespected(g, e,
                    partialSecret1, partialSecret2, commitment, complainerId, p, q);
            // Check 2
            boolean matches2 = verifyCommitmentRespected(g, partialSecret1, commitment, complainerId, p, q);

            if (matches1 && !matches2) {
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
                .map(PartialSecretMessageDTO::getPartialSecret1)
                .reduce(BigInteger::add).orElse(BigInteger.ZERO).mod(q);
    }

    public Set<Integer> getHonestParties() {
        return honestParties;
    }

}
