package dk.mmj.eevhe.server.bulletinboard;


import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.agreement.mvba.Communicator;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handles requests from Edges
 */
@Path("/")
public class BulletinBoardPeerResource {
    @Context
    ServletConfig servletConfig;

    private final static Logger logger = LogManager.getLogger(BulletinBoardPeerResource.class);

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

    @GET
    @Path("type")
    @Produces(MediaType.TEXT_HTML)
    public String getType() {
        logger.info("Received request for server type");

        return "<b>ServerType:</b> Bulletin Board Peer";
    }

    @GET
    @Path("getPublicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<PartialPublicInfo>> getPublicInfos() {
        List<SignedEntity<PartialPublicInfo>> list = getState().getSignedPartialPublicInfos();

        if (list.isEmpty()) {
            logger.warn("Attempt to fetch public infos before they were created");
            throw new NotFoundException();
        }

        return list;
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
    public List<SignedEntity<PartialResultList>> getResult() {
        try {
            return getState().getResults();
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
    public List<PersistedBallot> getBallots() {
        return getState().getBallots();
    }

    @GET
    @Path("getBallot/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public PersistedBallot getBallot(@PathParam("id") String id) {
        List<PersistedBallot> ballots = getState().getBallots().stream().filter(b -> b.getId().equals(id)).collect(Collectors.toList());

        if (ballots.isEmpty()) {
            throw new NotFoundException("Voter with id " + id + " has not cast a vote");
        }

        return ballots.get(0);
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
    public List<SignedEntity<CommitmentDTO>> getCommitments() {
        List<SignedEntity<CommitmentDTO>> list = getState().getSignedCommitments();

        if (list.isEmpty()) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return list;
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
    public List<SignedEntity<PedersenComplaintDTO>> getPedersenComplaints() {
        List<SignedEntity<PedersenComplaintDTO>> list = getState().getSignedPedersenComplaints();

        return list != null ? list : new ArrayList<>();
    }

    @GET
    @Path("feldmanComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<FeldmanComplaintDTO>> getFeldmanComplaints() {
        List<SignedEntity<FeldmanComplaintDTO>> list = getState().getSignedFeldmanComplaints();

        return list != null ? list : new ArrayList<>();
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
    public List<SignedEntity<ComplaintResolveDTO>> getComplaintResolves() {
        List<SignedEntity<ComplaintResolveDTO>> list = getState().getSignedComplaintResolves();

        return list != null ? list : new ArrayList<>();
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
    public List<SignedEntity<PartialPublicInfo>> getPublicInfo() {
        List<SignedEntity<PartialPublicInfo>> list = getState().getSignedPartialPublicInfos();

        return list != null ? list : new ArrayList<>();
    }

    @POST
    @Path("certificates")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCertificate(SignedEntity<CertificateDTO> certificate) {
        getConsumer().accept(certificate);
    }

    @GET
    @Path("certificates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<CertificateDTO>> getCertificate() {
        List<SignedEntity<CertificateDTO>> list = getState().getSignedCertificates();

        return list != null ? list : new ArrayList<>();
    }

    @GET
    @Path("getCurrentTime")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCurrentTime() {
        return Long.toString(new Date().getTime());
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
    @Path("getPeerCertificates")
    @Produces(MediaType.APPLICATION_JSON)
    public SignedEntity<List<String>> getPeerCertificates() {
        return ServerState.getInstance().get(BulletinBoardPeer.SIGNED_PEER_CERTIFICATES, SignedEntity.class);
    }
}
