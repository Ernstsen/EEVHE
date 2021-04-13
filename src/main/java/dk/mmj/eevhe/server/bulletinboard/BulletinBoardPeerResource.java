package dk.mmj.eevhe.server.bulletinboard;


import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;
import java.util.*;

import static dk.mmj.eevhe.server.bulletinboard.BulletinBoardConstants.*;

@Path("/")
public class BulletinBoardPeerResource {
    private final static Logger logger = LogManager.getLogger(BulletinBoardPeerResource.class);
    //    private final ServerState state = ServerState.getInstance();
    private final BBState state = BBState.getInstance();

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
    @SuppressWarnings("unchecked")
    public List<SignedEntity<PartialPublicInfo>> getPublicInfos() {
        List<SignedEntity<PartialPublicInfo>> list = state.getSignedPartialPublicInfos();

        if (list == null) {
            logger.warn("Attempt to fetch public infos before they were created");
            throw new NotFoundException();
        }

        return list;
    }

    @POST
    @Path("postBallot")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public void postBallot(BallotDTO ballot) {
        PersistedBallot persistedBallot = new PersistedBallot(ballot);

        if (state.hasVoted(persistedBallot)) {
            logger.warn("Voter with id=" + persistedBallot.getId() + " attempted to vote more than once");
            throw new NotAllowedException("A vote has already been registered with this ID");
        }

        BulletinBoardPeer.executeConsensusProtocol(
                new BBPackage<>(persistedBallot),
                "addBallot",
                MethodType.methodType(void.class, PersistedBallot.class)
        );
    }

    /**
     * @return List of {@link BigInteger} which is partial decryptions
     */
    @GET
    @Path("result")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public ResultList getResult() {
        try {
            return state.getResults();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @POST
    @Path("result")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postResult(SignedEntity<PartialResultList> partialDecryptions) {
        BulletinBoardPeer.executeConsensusProtocol(
                new BBPackage<>(partialDecryptions),
                "addResult",
                MethodType.methodType(void.class, SignedEntity.class)
        );
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("getBallots")
    @Produces(MediaType.APPLICATION_JSON)
    public BallotList getBallots() {
        BallotList ballotList = state.getBallots();

        if (ballotList.getBallots() == null || ballotList.getBallots().isEmpty()) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return ballotList;
    }

    @POST
    @Path("commitments")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCommitments(SignedEntity<CommitmentDTO> commitment) {
        logger.debug("Received commitment from DA with id=" + commitment.getEntity().getId() +
                ", for protocol=" + commitment.getEntity().getProtocol());

        BulletinBoardPeer.executeConsensusProtocol(
                new BBPackage<>(commitment),
                "addSignedCommitment",
                MethodType.methodType(void.class, SignedEntity.class)
        );
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("commitments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<CommitmentDTO>> getCommitments() {
        List<SignedEntity<CommitmentDTO>> list = state.getSignedCommitments();

        if (list == null) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return list;
    }

    @POST
    @Path("pedersenComplain")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPedersenComplaint(SignedEntity<PedersenComplaintDTO> complaint) {
        BulletinBoardPeer.executeConsensusProtocol(
                new BBPackage<>(complaint),
                "addSignedPedersenComplaint",
                MethodType.methodType(void.class, SignedEntity.class)
        );
    }

    @POST
    @Path("feldmanComplain")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postFeldmanComplaint(SignedEntity<FeldmanComplaintDTO> complaint) {
        BulletinBoardPeer.executeConsensusProtocol(
                new BBPackage<>(complaint),
                "addSignedFeldmanComplaint",
                MethodType.methodType(void.class, SignedEntity.class)
        );
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("pedersenComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<PedersenComplaintDTO>> getPedersenComplaints() {
        List<SignedEntity<PedersenComplaintDTO>> list = state.getSignedPedersenComplaints();

        return list != null ? list : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("feldmanComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<FeldmanComplaintDTO>> getFeldmanComplaints() {
        List<SignedEntity<FeldmanComplaintDTO>> list = state.getSignedFeldmanComplaints();

        return list != null ? list : new ArrayList<>();
    }

    @POST
    @Path("resolveComplaint")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resolveComplaint(SignedEntity<ComplaintResolveDTO> resolveDTO) {
        BulletinBoardPeer.executeConsensusProtocol(
                new BBPackage<>(resolveDTO),
                "addSignedComplaintResolve",
                MethodType.methodType(void.class, SignedEntity.class)
        );
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("complaintResolves")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<ComplaintResolveDTO>> getComplaintResolves() {
        List<SignedEntity<ComplaintResolveDTO>> list = state.getSignedComplaintResolves();

        return list != null ? list : new ArrayList<>();
    }

    @POST
    @Path("publicInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPublicInfo(SignedEntity<PartialPublicInfo> info) {
        BulletinBoardPeer.executeConsensusProtocol(
                new BBPackage<>(info),
                "addSignedPartialPublicInfo",
                MethodType.methodType(void.class, SignedEntity.class)
        );
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("publicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<PartialPublicInfo>> getPublicInfo() {
        List<SignedEntity<PartialPublicInfo>> list = state.getSignedPartialPublicInfos();

        return list != null ? list : new ArrayList<>();
    }

    @POST
    @Path("certificates")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCertificate(SignedEntity<CertificateDTO> certificate) {
        BulletinBoardPeer.executeConsensusProtocol(
                new BBPackage<>(certificate),
                "addSignedCertificate",
                MethodType.methodType(void.class, SignedEntity.class)
        );
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("certificates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<CertificateDTO>> getCertificate() {
        List<SignedEntity<CertificateDTO>> list = state.getSignedCertificates();

        return list != null ? list : new ArrayList<>();
    }


    @GET
    @Path("getCurrentTime")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCurrentTime() {
        return Long.toString(new Date().getTime());
    }
}