package dk.mmj.eevhe.server.bulletinboard;


import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.util.*;

import static dk.mmj.eevhe.server.bulletinboard.BulletinBoard.*;

@Path("/")
public class BulletinBoardEdgeResource {
    private final static Logger logger = LogManager.getLogger(BulletinBoardEdgeResource.class);
    private final ServerState state = ServerState.getInstance();

    @GET
    @Path("type")
    @Produces(MediaType.TEXT_HTML)
    public String getType() {
        logger.info("Received request for server type");
        return "<b>ServerType:</b> Bulletin Board";
    }

    @GET
    @Path("getPublicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<SignedEntity<PartialPublicInfo>> getPublicInfos() {
        List<SignedEntity<PartialPublicInfo>> list = state.get(PUBLIC_INFO, List.class);

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
        Set<String> hasVoted = state.get(HAS_VOTED, HashSet.class);
        String voterId = ballot.getId();


        if (hasVoted.contains(voterId)) {
            logger.warn("Voter with id=" + voterId + " attempted to vote more than once");
            throw new NotAllowedException("A vote has already been registered with this ID");
        }
        addToList(BALLOTS, new PersistedBallot(ballot));
        hasVoted.add(voterId);
    }

    /**
     * @return List of {@link BigInteger} which is partial decryptions
     */
    @GET
    @Path("result")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<SignedEntity<PartialResultList>> getResult() {
        try {
            return state.get(RESULT, List.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @POST
    @Path("result")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postResult(SignedEntity<PartialResultList> partialDecryptions) {
        addToList(RESULT, partialDecryptions);
    }

    private void addToList(String key, Object element) {
        state.putInList(key, element);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("getBallots")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PersistedBallot> getBallots() {
        List<PersistedBallot> list = state.get(BALLOTS, List.class);

        if (list == null || list.isEmpty()) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return list;
    }

    @POST
    @Path("commitments")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCommitments(SignedEntity<CommitmentDTO> commitment) {
        logger.debug("Received commitment from DA with id=" + commitment.getEntity().getId() +
                ", for protocol=" + commitment.getEntity().getProtocol());
        addToList(COEFFICIENT_COMMITMENT, commitment);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("commitments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<CommitmentDTO>> getCommitments() {
        List<SignedEntity<CommitmentDTO>> list = state.get(COEFFICIENT_COMMITMENT, List.class);

        if (list == null) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return list;
    }

    @POST
    @Path("pedersenComplain")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPedersenComplaint(SignedEntity<PedersenComplaintDTO> complaint) {
        addToList(PEDERSEN_COMPLAINTS, complaint);
    }

    @POST
    @Path("feldmanComplain")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postFeldmanComplaint(SignedEntity<FeldmanComplaintDTO> complaint) {
        addToList(FELDMAN_COMPLAINTS, complaint);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("pedersenComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<PedersenComplaintDTO>> getPedersenComplaints() {
        List<SignedEntity<PedersenComplaintDTO>> list = state.get(PEDERSEN_COMPLAINTS, List.class);

        return list != null ? list : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("feldmanComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<FeldmanComplaintDTO>> getFeldmanComplaints() {
        List<SignedEntity<FeldmanComplaintDTO>> list = state.get(FELDMAN_COMPLAINTS, List.class);

        return list != null ? list : new ArrayList<>();
    }

    @POST
    @Path("resolveComplaint")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resolveComplaint(SignedEntity<ComplaintResolveDTO> resolveDTO) {
        addToList(RESOLVED_COMPLAINTS, resolveDTO);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("complaintResolves")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<ComplaintResolveDTO>> getComplaintResolves() {
        List<SignedEntity<ComplaintResolveDTO>> list = state.get(RESOLVED_COMPLAINTS, List.class);

        return list != null ? list : new ArrayList<>();
    }

    @POST
    @Path("publicInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPublicInfo(SignedEntity<PartialPublicInfo> info) {
        addToList(PUBLIC_INFO, info);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("publicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<PartialPublicInfo>> getPublicInfo() {
        List<SignedEntity<PartialPublicInfo>> list = state.get(PUBLIC_INFO, List.class);

        return list != null ? list : new ArrayList<>();
    }

    @POST
    @Path("certificates")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCertificate(SignedEntity<CertificateDTO> cert) {
        addToList(CERTIFICATE, cert);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("certificates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<CertificateDTO>> getCertificate() {
        List<SignedEntity<CertificateDTO>> list = state.get(CERTIFICATE, List.class);

        return list != null ? list : new ArrayList<>();
    }

    @GET
    @Path("getCurrentTime")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCurrentTime() {
        return Long.toString(new Date().getTime());
    }

    @GET
    @Path("getPeerCertificateList")
    @Produces(MediaType.TEXT_HTML)
    public List<SignedEntity<String>> getPeerCertificateList() {
        return state.get(BulletinBoardEdge.CERTIFICATE_LIST, List.class);
    }
}
