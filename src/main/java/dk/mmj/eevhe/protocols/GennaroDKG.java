package dk.mmj.eevhe.protocols;

import dk.mmj.eevhe.crypto.FeldmanVSSUtils;
import dk.mmj.eevhe.crypto.PedersenVSSUtils;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.keygeneration.KeyGenerationParameters;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.interfaces.Broadcaster;
import dk.mmj.eevhe.protocols.interfaces.IncomingChannel;
import dk.mmj.eevhe.protocols.interfaces.PeerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pedersen-DKG protocol
 * <br>
 * Described in article:
 * Gennaro, R., Jarecki, S., Krawczyk, H. et al. Secure Distributed Key Generation for Discrete-Log Based Cryptosystems.
 * J Cryptology 20, 51–83 (2007). <a href="https://doi.org/10.1007/s00145-006-0347-3">https://doi.org/10.1007/s00145-006-0347-3</a>
 */
public class GennaroDKG implements DKG {
    private final Broadcaster broadcaster;
    private final IncomingChannel incoming;
    private final Map<Integer, PeerCommunicator> peerMap;
    private final Map<Integer, PartialSecretMessageDTO> secrets = new HashMap<>();
    private final Map<Integer, BigInteger[]> commitments = new HashMap<>();
    private final Logger logger;
    private final int id;
    private final BigInteger g;
    private final BigInteger q;
    private final BigInteger p;
    private final BigInteger e;
    private BigInteger[] pol1;
    private BigInteger[] pol2;

    public GennaroDKG(Broadcaster broadcaster, IncomingChannel incoming,
                      Map<Integer, PeerCommunicator> peerCommunicatorMap,
                      int id, KeyGenerationParameters params, String logPrefix) {
        this.broadcaster = broadcaster;
        this.incoming = incoming;
        this.peerMap = peerCommunicatorMap;
        this.id = id;
        this.g = params.getGenerator();
        this.q = params.getPrimePair().getQ();
        this.p = params.getPrimePair().getP();
        this.e = PedersenVSSUtils.generateElementInSubgroup(g, p);

        logger = LogManager.getLogger(GennaroDKG.class.getName() + ". " + logPrefix + ":");
    }

    @Override
    public void startProtocol() {
        logger.info("Initialized PedersenDKG");

        //We chose our polynomials
        int t = ((peerMap.size()) / 2);
        pol1 = SecurityUtils.generatePolynomial(t, q);
        pol2 = SecurityUtils.generatePolynomial(t, q);

        logger.debug("Calculating coefficient commitments.");
        //Calculates commitments
        BigInteger[] commitment = PedersenVSSUtils.computeCoefficientCommitments(g, e, p, pol1, pol2);

        logger.info("Broadcasting commitments");
        broadcaster.commit(new CommitmentDTO(commitment, id));

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
    public boolean handleReceivedValues() {
        logger.info("Checking received secret values, for DA with id=" + id);

        List<PartialSecretMessageDTO> incomingSecrets = incoming.receiveSecrets();

        for (PartialSecretMessageDTO secret : incomingSecrets) {
            secrets.put(secret.getSender(), secret);
        }

        boolean hasReceivedFromAll = secrets.size() == peerMap.size() + 1;//Peers + self
        //TODO: When should we just ignore the missing value(s)? Otherwise risk corruption killing election by non-participation
        if (!hasReceivedFromAll) {
            logger.info("Has not received all commitments - should retry");
            return false;
        }

        logger.info("reading commitments");
        final List<CommitmentDTO> commitments = broadcaster.getCommitments();

        logger.info("Verifying secret shares, using commitments");
        for (CommitmentDTO commitment : commitments) {
            this.commitments.put(commitment.getId(), commitment.getCommitment());
        }


        for (Map.Entry<Integer, PartialSecretMessageDTO> entry : new ArrayList<>(secrets.entrySet())) {
            int id = entry.getKey();

            BigInteger partialSecret1 = entry.getValue().getPartialSecret1();
            BigInteger partialSecret2 = entry.getValue().getPartialSecret2();

            BigInteger[] commitment = this.commitments.get(entry.getKey());
            if (commitment == null) {
                logger.error("Peer with id=" + id + ", had no corresponding commitment!");
                continue;//TODO: What to do?
            }

            boolean matches = PedersenVSSUtils.verifyCommitmentRespected(
                    g, e, partialSecret1, partialSecret2, commitment, BigInteger.valueOf(this.id), p, q);
            if (!matches) {
                logger.info("" + this.id + ": Sending complaint about DA=" + id);
                final ComplaintDTO complaint = new ComplaintDTO(this.id, id);
                secrets.remove(id); //remove secret, as it's garbage
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
                BigInteger complaintValue1 = SecurityUtils.evaluatePolynomial(pol1, complaint.getSenderId());
                BigInteger complaintValue2 = SecurityUtils.evaluatePolynomial(pol2, complaint.getSenderId());
                PartialSecretMessageDTO complaintResolve = new PartialSecretMessageDTO(
                        complaintValue1, complaintValue2, complaint.getSenderId(), id);
                ComplaintResolveDTO complaintResolveDTO = new ComplaintResolveDTO(
                        complaint.getSenderId(), this.id, complaintResolve);
                broadcaster.resolveComplaint(complaintResolveDTO);
            }
        }
    }

    @Override
    public void applyResolves() {
        logger.info("Checking for complaint resolves");
        List<ComplaintResolveDTO> resolves = broadcaster.getResolves();

        for (ComplaintResolveDTO resolve : resolves) {
            if (resolve.getComplaintSenderId() != id) {
                continue;
            }

            logger.info("Applying resolve: " + resolve);
            int resolverId = resolve.getComplaintResolverId();
            BigInteger[] commit = commitments.get(resolverId);
            boolean resolveIsVerifiable = PedersenVSSUtils.verifyCommitmentRespected(
                    g, e, resolve.getValue().getPartialSecret1(), resolve.getValue().getPartialSecret2(), commit, BigInteger.valueOf(id), p, q);
            if (resolveIsVerifiable) {
                secrets.put(resolverId, resolve.getValue());
                logger.debug("Resolve was applied: " + resolve);
            } else {
                //Disqualify
                logger.warn("Resolve from id=" + resolverId + " could not be verified. Disqualifying and using ID instead");
                //TODO: Can we do this? Is this the right form?
                // secrets.put(resolverId, null);
            }
        }

    }

    @Override
    public PartialKeyPair createKeys() {
        logger.info("Combining values, to make keys");
        BigInteger[] uVals = secrets.values().stream().map(PartialSecretMessageDTO::getPartialSecret1).toArray(BigInteger[]::new);
        BigInteger[] firstCommits = commitments.values().stream().map(arr -> arr[0]).toArray(BigInteger[]::new);


        return FeldmanVSSUtils.generateKeyPair(g, q, uVals, firstCommits);
    }
}