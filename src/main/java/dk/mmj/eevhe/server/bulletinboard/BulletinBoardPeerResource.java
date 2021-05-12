package dk.mmj.eevhe.server.bulletinboard;


import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.entities.wrappers.*;
import dk.mmj.eevhe.protocols.agreement.mvba.Communicator;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import javax.servlet.ServletConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handles requests from Edges
 */
@Path("/")
public class BulletinBoardPeerResource {
    private final static Logger logger = LogManager.getLogger(BulletinBoardPeerResource.class);
    @Context
    ServletConfig servletConfig;

    private BulletinBoardState getState() {
        ServerState serverState = ServerState.getInstance();
        String id = getId();

        return serverState.computeIfAbsent("bbState." + id, s -> new BulletinBoardState());
    }

    private String getId() {
        return servletConfig.getInitParameter("id");
    }

    @SuppressWarnings("unchecked")
    private Consumer<BulletinBoardUpdatable> getConsumer() {
        ServerState serverState = ServerState.getInstance();
        return (Consumer<BulletinBoardUpdatable>) serverState.get("executeConsensusProtocol." + getId(), Consumer.class);
    }

    private AsymmetricKeyParameter getSecretKey() {
        ServerState serverState = ServerState.getInstance();
        return serverState.get(BulletinBoardPeer.SECRET_KEY + "." + getId(), AsymmetricKeyParameter.class);
    }

    @GET
    @Path("type")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<String> getType() {
        logger.info("Received request for server type");

        return new SignedEntity<>("<b>ServerType:</b> Bulletin Board Peer", getSecretKey());
    }

    @POST
    @Path("postBallot")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postBallot(BallotDTO ballot) {
//        TODO: Issue med Timestamp i PersistedBallot i forhold til MVBA protokol - send evt. ballot rundt i stedet for
//        TODO: Consensus om timestamp?? unpleasant
        PersistedBallot persistedBallot = new PersistedBallot(ballot);

        BulletinBoardState state = getState();

        if (state.hasVoted(persistedBallot)) {
            logger.warn("Voter with id=" + persistedBallot.getId() + " attempted to vote more than once");
            throw new NotAllowedException("A vote has already been registered with this ID");
        }

        getConsumer().accept(persistedBallot);
    }

    /**
     * @return List of {@link BigInteger} which is partial decryptions
     */
    @GET
    @Path("result")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<PartialResultWrapper> getResult() {
        try {
            return new SignedEntity<>(new PartialResultWrapper(getState().getResults()), getSecretKey());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @POST
    @Path("result")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postResult(SignedEntity<PartialResultList> partialDecryptions) {
        getConsumer().accept(partialDecryptions);
    }

    @GET
    @Path("getBallots")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<BallotWrapper> getBallots() {
        return new SignedEntity<>(new BallotWrapper(getState().getBallots()), getSecretKey());
    }

    @GET
    @Path("getBallot/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<PersistedBallot> getBallot(@PathParam("id") String id) {
        List<PersistedBallot> ballots = getState().getBallots().stream().filter(b -> b.getId().equals(id)).collect(Collectors.toList());

        if (ballots.isEmpty()) {
            logger.warn("Failed to locate vote with id= " + id);
            throw new NotFoundException("Voter with id " + id + " has not cast a vote");
        }

        if (ballots.size() > 1) {
            logger.warn("Expected one ballot for voter with id " + id + ", but found " + ballots.size() + " ballots.");
        }

        return new SignedEntity<>(ballots.get(0), getSecretKey());
    }

    @POST
    @Path("commitments")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCommitments(SignedEntity<CommitmentDTO> commitment) {
        logger.debug("Received commitment from DA with id=" + commitment.getEntity().getId() +
                ", for protocol=" + commitment.getEntity().getProtocol());

        getConsumer().accept(commitment);
    }

    @GET
    @Path("commitments")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<CommitmentWrapper> getCommitments() {
        List<SignedEntity<CommitmentDTO>> list = getState().getSignedCommitments();

        if (list.isEmpty()) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return new SignedEntity<>(new CommitmentWrapper(list), getSecretKey());
    }

    @POST
    @Path("pedersenComplain")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPedersenComplaint(SignedEntity<PedersenComplaintDTO> complaint) {
        getConsumer().accept(complaint);
    }

    @POST
    @Path("feldmanComplain")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postFeldmanComplaint(SignedEntity<FeldmanComplaintDTO> complaint) {
        getConsumer().accept(complaint);
    }

    @GET
    @Path("pedersenComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<PedersenComplaintWrapper> getPedersenComplaints() {
        List<SignedEntity<PedersenComplaintDTO>> list = getState().getSignedPedersenComplaints();

        PedersenComplaintWrapper wrapper = new PedersenComplaintWrapper(list != null ? list : new ArrayList<>());

        return new SignedEntity<>(wrapper, getSecretKey());
    }

    @GET
    @Path("feldmanComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<FeldmanComplaintWrapper> getFeldmanComplaints() {
        List<SignedEntity<FeldmanComplaintDTO>> list = getState().getSignedFeldmanComplaints();

        FeldmanComplaintWrapper wrapper = new FeldmanComplaintWrapper(list != null ? list : new ArrayList<>());

        return new SignedEntity<>(wrapper, getSecretKey());
    }

    @POST
    @Path("resolveComplaint")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resolveComplaint(SignedEntity<ComplaintResolveDTO> resolveDTO) {
        getConsumer().accept(resolveDTO);
    }

    @GET
    @Path("complaintResolves")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<ComplaintResolveWrapper> getComplaintResolves() {
        List<SignedEntity<ComplaintResolveDTO>> list = getState().getSignedComplaintResolves();

        ComplaintResolveWrapper wrapper = new ComplaintResolveWrapper(list != null ? list : new ArrayList<>());

        return new SignedEntity<>(wrapper, getSecretKey());
    }

    @POST
    @Path("publicInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPublicInfo(SignedEntity<PartialPublicInfo> info) {
        getConsumer().accept(info);
    }

    @GET
    @Path("publicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<PublicInfoWrapper> getPublicInfo() {
        List<SignedEntity<PartialPublicInfo>> list = getState().getSignedPartialPublicInfos();

        if (list.isEmpty()) {
            logger.warn("Attempt to fetch public infos before they were created");
            throw new NotFoundException();
        }

        return new SignedEntity<>(new PublicInfoWrapper(list), getSecretKey());
    }

    @POST
    @Path("certificates")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postDACertificate(SignedEntity<CertificateDTO> certificate) {
        getConsumer().accept(certificate);
    }

    @GET
    @Path("certificates")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<CertificatesWrapper> getDACertificate() {
        List<SignedEntity<CertificateDTO>> list = getState().getSignedCertificates();

        CertificatesWrapper wrapper = new CertificatesWrapper(list != null ? list : new ArrayList<>());

        return new SignedEntity<>(wrapper, getSecretKey());
    }

    @GET
    @Path("getCurrentTime")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<String> getCurrentTime() {
        return new SignedEntity<>(Long.toString(new Date().getTime()), getSecretKey());
    }

    @POST
    @Path("BAMessage")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postBAMessage(SignedEntity<BAMessage> message) {
        Communicator communicator = ServerState.getInstance().get("mvba.communicator." + getId(), Communicator.class);
        message.getEntity().getCommunicatorConsumer().accept(communicator, message);
    }

    @POST
    @Path("BroadcastMessage/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postBroadcastMessage(SignedEntity<String> message, @PathParam("id") String identifier) {
        ServerState serverState = ServerState.getInstance();
        BiConsumer<SignedEntity<String>, String> brachaConsumer
                = (BiConsumer<SignedEntity<String>, String>) serverState.get("bracha.consumer." + getId(), BiConsumer.class);
        brachaConsumer.accept(message, identifier);
    }

    @GET
    @Path("peerCertificates")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<StringListWrapper> getBBPeerCertificates() {
        Map<Integer, String> peerCertificates = ServerState.getInstance().get(BulletinBoardPeer.PEER_CERTIFICATES, Map.class);

        return new SignedEntity<>(new StringListWrapper(new ArrayList<>(peerCertificates.values())), getSecretKey());
    }
}
