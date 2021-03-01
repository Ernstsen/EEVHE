package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParameters;
import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.ComplaintDTO;
import dk.mmj.eevhe.entities.ComplaintResolveDTO;
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

public class FeldmanVSS extends AbstractVSS implements VSS {
    private static final String FELDMAN = "FeldmanVSS";
    private final BigInteger[] polynomial;
    private final Set<Integer> honestParties = new HashSet<>();

    public FeldmanVSS(Broadcaster broadcaster, IncomingChannel incoming,
                      Map<Integer, PeerCommunicator> peerCommunicatorMap,
                      int id, KeyGenerationParameters params, String logPrefix,
                      BigInteger[] polynomial, Map<Integer, PartialSecretMessageDTO> secrets) {
        super(broadcaster, incoming, peerCommunicatorMap, id, params, logPrefix);

        int t = ((peerMap.size()) / 2);
        if (secrets != null) {
            this.secrets = secrets;
        }
        this.polynomial = polynomial == null ? SecurityUtils.generatePolynomial(t, q) : polynomial;
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

        if (secrets.isEmpty()) {
            logger.info("Computing and distributing secret shares");
            commitments.put(id, commitment);
            secrets.put(id,
                    new PartialSecretMessageDTO(
                            SecurityUtils.evaluatePolynomial(polynomial, id),
                            null, id, id));

            logger.info("Sending partial secrets to peers");
            logger.debug("Has " + peerMap.size() + " peers");
            for (Integer target : peerMap.keySet()) {
                BigInteger val = SecurityUtils.evaluatePolynomial(polynomial, target);
                PeerCommunicator communicator = this.peerMap.get(target);

                communicator.sendSecret(new PartialSecretMessageDTO(val, null, target, id));
            }
        }

        logger.debug("Finished first step of protocol");
    }

    @Override
    public boolean handleReceivedValues() {
        logger.info("Checking received secret values");

        List<PartialSecretMessageDTO> incomingSecrets = incoming.receiveSecrets();

        for (PartialSecretMessageDTO secret : incomingSecrets) {
            secrets.put(secret.getSender(), secret);
        }

        boolean hasReceivedFromAll = secrets.keySet().containsAll(honestParties);//Peers + self
        //TODO: When should we just ignore the missing value(s)? Otherwise risk corruption killing election by non-participation
        if (!hasReceivedFromAll) {
            logger.info("Has not received all secrets - should retry");
            return false;
        }

        logger.info("Reading commitments");
        final List<CommitmentDTO> commitments = broadcaster.getCommitments().stream()
                .filter(c -> FELDMAN.equals(c.getProtocol()))
                .collect(Collectors.toList());

        logger.info("Verifying secret shares, using commitments");
        for (CommitmentDTO commitment : commitments) {
            this.commitments.put(commitment.getId(), commitment.getCommitment());
        }

        for (Map.Entry<Integer, PartialSecretMessageDTO> entry : new ArrayList<>(secrets.entrySet())) {
            int id = entry.getKey();
            BigInteger partialSecret = entry.getValue().getPartialSecret1();


            BigInteger[] commitment = this.commitments.get(id);
            if (commitment == null) {
                logger.error("Peer with id=" + id + ", had no corresponding commitment!");
                continue;//TODO: What to do?
            }

            boolean matches = verifyCommitmentRespected(g, partialSecret, commitment, BigInteger.valueOf(this.id), p, q);
            if (!matches) {
                logger.info("" + this.id + ": Sending complaint about Peer with ID=" + id);
                final ComplaintDTO complaint = new ComplaintDTO(this.id, id);
                secrets.remove(id); //remove secret, as it's garbage
                honestParties.remove(id);
                broadcaster.complain(complaint);
            }
        }

        return true;
    }

    @Override
    public void handleComplaints() {
        logger.info("Fetching complaints");
        List<ComplaintDTO> complaints = broadcaster.getComplaints();

        logger.debug("Received " + complaints.size() + " complaints");
        for (ComplaintDTO complaint : complaints) {
            if (complaint.getTargetId() == id) {
                logger.info("Found complaint about self. Resolving.");
                BigInteger complaintValue1 = SecurityUtils.evaluatePolynomial(polynomial, complaint.getSenderId());
                PartialSecretMessageDTO complaintResolve = new PartialSecretMessageDTO(
                        complaintValue1, null, complaint.getSenderId(), id);
                ComplaintResolveDTO complaintResolveDTO = new ComplaintResolveDTO(
                        complaint.getSenderId(), this.id, complaintResolve);
                broadcaster.resolveComplaint(complaintResolveDTO);
            }
        }

    }

    @Override
    public void applyResolves() {
        //TODO: We dont use hand complaint and apply resolves in Gennaro.
        logger.info("Checking for complaint resolves");
        List<ComplaintResolveDTO> resolves = broadcaster.getResolves();

        for (ComplaintResolveDTO resolve : resolves) {
            int resolverId = resolve.getComplaintResolverId();
            BigInteger[] commit = commitments.get(resolverId);
            boolean resolveIsVerifiable = verifyCommitmentRespected(
                    g, resolve.getValue().getPartialSecret1(),
                    commit, BigInteger.valueOf(id), p, q);

            if (resolveIsVerifiable && resolve.getComplaintSenderId() == id) {
                logger.info("Applying resolve: " + resolve);
                secrets.put(resolverId, resolve.getValue());
            }
        }
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

    public Map<Integer, PartialSecretMessageDTO> getSecrets() {
        return secrets;
    }
}
