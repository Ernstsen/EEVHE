package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.ExtendedKeyGenerationParameters;
import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.ComplaintResolveDTO;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.entities.PedersenComplaintDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.connectors.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.connectors.interfaces.PeerCommunicator;
import dk.mmj.eevhe.protocols.interfaces.VSS;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.crypto.PedersenVSSUtils.computeCoefficientCommitments;
import static dk.mmj.eevhe.crypto.PedersenVSSUtils.verifyCommitmentRespected;

/**
 * Pedersen-DKG protocol
 * <br>
 * Described in article:
 * Gennaro, R., Jarecki, S., Krawczyk, H. et al. Secure Distributed Key Generation for Discrete-Log Based Cryptosystems.
 * J Cryptology 20, 51–83 (2007). <a href="https://doi.org/10.1007/s00145-006-0347-3">https://doi.org/10.1007/s00145-006-0347-3</a>
 */
public class PedersenVSS extends AbstractVSS implements VSS {
    static final String PEDERSEN = "PedersenVSS";
    private final Set<Integer> honestParties = new HashSet<>();
    private final BigInteger e;
    private final int t;
    private final BigInteger[] pol1;
    private final BigInteger[] pol2;
    private final Map<Integer, BigInteger[]> commitments = new HashMap<>();


    public PedersenVSS(Broadcaster broadcaster, IncomingChannel incoming,
                       Map<Integer, PeerCommunicator> peerCommunicatorMap,
                       int id, ExtendedKeyGenerationParameters params, String logPrefix, BigInteger[] pol1, BigInteger[] pol2) {
        super(broadcaster, incoming, peerCommunicatorMap, id, params, logPrefix);
        this.e = params.getGroupElement();
        this.t = ((peerMap.size()) / 2);
        this.pol1 = pol1 == null ? SecurityUtils.generatePolynomial(t, q) : pol1;
        this.pol2 = pol2 == null ? SecurityUtils.generatePolynomial(t, q) : pol2;
    }

    @Override
    public void startProtocol() {
        logger.info("Initialized PedersenVSS");

        honestParties.add(id);
        honestParties.addAll(peerMap.keySet());

        logger.debug("Calculating coefficient commitments.");
        //Calculates commitments
        BigInteger[] commitment = computeCoefficientCommitments(g, e, p, pol1, pol2);

        logger.info("Broadcasting commitments");
        broadcaster.commit(new CommitmentDTO(commitment, id, PEDERSEN));

        commitments.put(id, commitment);
        secrets.put(id,
                new PartialSecretMessageDTO(
                        SecurityUtils.evaluatePolynomial(pol1, id),
                        SecurityUtils.evaluatePolynomial(pol2, id), id, id));

        logger.info("Sending partial secrets to peers");
        logger.debug("Has " + peerMap.size() + " peers");
        for (Integer target : peerMap.keySet()) {
            BigInteger val1 = SecurityUtils.evaluatePolynomial(pol1, target);
            BigInteger val2 = SecurityUtils.evaluatePolynomial(pol2, target);
            PeerCommunicator communicator = this.peerMap.get(target);

            communicator.sendSecret(new PartialSecretMessageDTO(val1, val2, target, id));
        }

        logger.debug("Finished first step of protocol");
    }

    @Override
    public void handleReceivedValues() {
        logger.info("Checking received secret values");

        List<PartialSecretMessageDTO> incomingSecrets = incoming.receiveSecrets();

        for (PartialSecretMessageDTO secret : incomingSecrets) {
            secrets.put(secret.getSender(), secret);
        }

        logger.info("Reading commitments");
        final List<CommitmentDTO> commitments = broadcaster.getCommitments().stream()
                .filter(c -> PEDERSEN.equals(c.getProtocol()))
                .collect(Collectors.toList());

        logger.info("Verifying secret shares, using commitments");
        for (CommitmentDTO commitment : commitments) {
            this.commitments.put(commitment.getId(), commitment.getCommitment());
        }

        honestParties.removeIf(i -> !this.commitments.containsKey(i));

        for (Map.Entry<Integer, PartialSecretMessageDTO> entry : new ArrayList<>(secrets.entrySet())) {
            int senderId = entry.getKey();

            BigInteger partialSecret1 = entry.getValue().getPartialSecret1();
            BigInteger partialSecret2 = entry.getValue().getPartialSecret2();

            BigInteger[] commitment = this.commitments.get(senderId);
            if (commitment == null) {
                logger.error("Peer with id=" + senderId + ", had no corresponding commitment");
                honestParties.remove(senderId);
                continue;
            }

            boolean matches = verifyCommitmentRespected(
                    g, e, partialSecret1, partialSecret2, commitment, BigInteger.valueOf(id), p, q);
            if (!matches) {
                logger.info("" + this.id + ": Sending complaint about DA=" + senderId);
                final PedersenComplaintDTO complaint = new PedersenComplaintDTO(this.id, senderId);
                broadcaster.pedersenComplain(complaint);
            }
        }

        boolean hasReceivedSecretsFromAll = secrets.size() == peerMap.size() + 1;//Peers + self
        boolean hasReceivedCommitmentsFromAll = commitments.size() == peerMap.size() + 1;//Peers + self
        if (!hasReceivedSecretsFromAll || !hasReceivedCommitmentsFromAll) {
            logger.info("Has not received information from all - should retry");
        }
    }

    @Override
    public void handleComplaints() {
        logger.info("Fetching complaints");
        List<PedersenComplaintDTO> complaints = broadcaster.getPedersenComplaints();
        Map<Integer, Integer> complaintCountMap = new HashMap<>();

        logger.debug("Received " + complaints.size() + " complaints");
        for (PedersenComplaintDTO complaint : complaints) {
            complaintCountMap.compute(complaint.getTargetId(), (key, val) -> val != null ? val + 1 : 1);

            if (complaint.getTargetId() == id) {
                logger.info("Found complaint about self. Resolving.");
                BigInteger complaintValue1 = SecurityUtils.evaluatePolynomial(pol1, complaint.getSenderId());
                BigInteger complaintValue2 = SecurityUtils.evaluatePolynomial(pol2, complaint.getSenderId());
                PartialSecretMessageDTO complaintResolve = new PartialSecretMessageDTO(
                        complaintValue1, complaintValue2, complaint.getSenderId(), id);
                ComplaintResolveDTO complaintResolveDTO = new ComplaintResolveDTO(
                        complaint.getSenderId(), this.id, complaintResolve);
                broadcaster.resolveComplaint(complaintResolveDTO);
            }
        }
        complaintCountMap.entrySet().stream()
                .filter(e -> e.getValue() > t)
                .forEach(e -> honestParties.remove(e.getKey()));
    }

    @Override
    public void applyResolves() {
        logger.info("Checking for complaint resolves");
        List<ComplaintResolveDTO> resolves = broadcaster.getResolves();
        List<PedersenComplaintDTO> complaints = broadcaster.getPedersenComplaints();

        // If a party does not send a resolve after being complained about, he is removed from honest parties.
        List<Integer> resolverIds = resolves.stream().map(ComplaintResolveDTO::getComplaintResolverId).collect(Collectors.toList());
        for (PedersenComplaintDTO complaint : complaints) {
            int complaintId = complaint.getTargetId();
            if (!resolverIds.contains(complaintId)) {
                honestParties.remove(complaintId);
            }
        }

        for (ComplaintResolveDTO resolve : resolves) {
            int resolverId = resolve.getComplaintResolverId();

            BigInteger[] commit = commitments.get(resolverId);
            boolean resolveIsVerifiable = verifyCommitmentRespected(
                    g, e, resolve.getValue().getPartialSecret1(), resolve.getValue().getPartialSecret2(),
                    commit, BigInteger.valueOf(resolve.getComplaintSenderId()), p, q);

            if (resolveIsVerifiable && resolve.getComplaintSenderId() == id) {
                logger.info("Applying resolve: " + resolve);
                secrets.put(resolverId, resolve.getValue());
            } else if (!resolveIsVerifiable) {
                logger.warn("Resolve from id=" + resolverId + " could not be verified. Party is being disqualified.");
                honestParties.remove(resolverId);
            }
        }
    }

    @Override
    public BigInteger output() {
        logger.info("Combining values, to make keys");
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
