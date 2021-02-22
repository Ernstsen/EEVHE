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
    private final Map<Integer, BigInteger> secrets1 = new HashMap<>();
    private final Map<Integer, BigInteger> secrets2 = new HashMap<>();
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
        secrets1.put(id, SecurityUtils.evaluatePolynomial(pol1, id));
        secrets2.put(id, SecurityUtils.evaluatePolynomial(pol2, id));

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
            secrets1.put(secret.getSender(), secret.getPartialSecret1());
        }

        boolean hasReceivedFromAll = secrets1.size() == peerMap.size() + 1;//Peers + self
        if (!hasReceivedFromAll) {//TODO: When should we just ignore the missing value(s)? Otherwise risk corruption killing election by non-participation
            logger.info("Has not received all commitments - should retry");
            return false;
        }

        logger.info("reading commitments");
        final List<CommitmentDTO> commitments = broadcaster.getCommitments();

        logger.info("Verifying secret shares, using commitments");
        for (CommitmentDTO commitment : commitments) {
            this.commitments.put(commitment.getId(), commitment.getCommitment());
        }


        for (Map.Entry<Integer, BigInteger> entry : new ArrayList<>(secrets1.entrySet())) {
            int id = entry.getKey();

            BigInteger partialSecret = entry.getValue();
            BigInteger[] commitment = this.commitments.get(entry.getKey());
            if (commitment == null) {
                logger.error("Peer with id=" + id + ", had no corresponding commitment!");
                continue;//TODO: What to do?
            }

            boolean matches = FeldmanVSSUtils.verifyCommitmentRespected(g, partialSecret, commitment, BigInteger.valueOf(this.id), p, q);
            if (!matches) {
                logger.info("" + this.id + ": Sending complaint about DA=" + id);
                final ComplaintDTO complaint = new ComplaintDTO(this.id, id);
                secrets1.remove(id);//remove secret, as it's garbage
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
                BigInteger complaintValue = SecurityUtils.evaluatePolynomial(pol1, complaint.getSenderId());
                ComplaintResolveDTO complaintResolveDTO = new ComplaintResolveDTO(complaint.getSenderId(), this.id, complaintValue);
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
            boolean resolveIsVerifiable = FeldmanVSSUtils.verifyCommitmentRespected(g, resolve.getValue(), commit, BigInteger.valueOf(id), p, q);
            if (resolveIsVerifiable) {
                secrets1.put(resolverId, resolve.getValue());
                logger.debug("Resolve was applied: " + resolve);
            } else {
                //Disqualify
                logger.warn("Resolve from id=" + resolverId + " could not be verified. Disqualifying and using ID instead");
                secrets1.put(resolverId, BigInteger.valueOf(resolverId));//TODO: Can we do this? Is this the right form?
            }
        }

    }

    @Override
    public PartialKeyPair createKeys() {
        logger.info("Combining values, to make keys");
        BigInteger[] uVals = secrets1.values().toArray(new BigInteger[0]);
        BigInteger[] firstCommits = commitments.values().stream().map(arr -> arr[0]).toArray(BigInteger[]::new);


        return FeldmanVSSUtils.generateKeyPair(g, q, uVals, firstCommits);
    }
}
