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
public class BulletinBoardResource {
    private final static String PUBLIC_INFO = "publicInfo";
    private final static Logger logger = LogManager.getLogger(BulletinBoardResource.class);
    private final ServerState state = ServerState.getInstance();

    @GET
    @Path("type")
    @Produces(MediaType.TEXT_HTML)
    public String getType() {
        logger.info("Received request for server type");
        return "<b>ServerType:</b> Bulletin Board";
    }

    @GET
    @Path("publicKey")
    @Produces(MediaType.APPLICATION_JSON)
    public PublicKey getPublicKey() {
        PublicKey publicKey = state.get(PUBLIC_KEY, PublicKey.class);

        if (publicKey == null) {
            logger.warn("A request was made for a public key but none was found");
            throw new NotFoundException("Currently the server has no public key");
        }

        return publicKey;
    }

    @POST
    @Path("publicKey")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setPublicKey(PublicKey publicKey) {
        if (publicKey == null) {
            logger.warn("A submitted key CANNOT be null");
            throw new NotAllowedException("Key was null");

        }

        state.put(PUBLIC_KEY, publicKey);
    }

    @GET
    @Path("getPublicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<PartialPublicInfo> getPublicInfos() {
        List<PartialPublicInfo> list = state.get(PUBLIC_INFO, List.class);

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

        List<PersistedBallot> votes = state.get(BALLOTS, ArrayList.class);
        votes.add(new PersistedBallot(ballot));
        hasVoted.add(voterId);
    }

    /**
     * @return List of {@link BigInteger} which is partial decryptions
     */
    @GET
    @Path("result")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public ResultList getResult() {
        return new ResultList(state.get(RESULT, List.class));
    }

    @POST
    @Path("result")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postResult(PartialResultList partialDecryptions) {
        addToList(RESULT, partialDecryptions);
    }

    @SuppressWarnings("unchecked")
    private void addToList(String key, Object element) {
        List<Object> list = state.get(key, List.class);

        if (list == null) {
            list = new ArrayList<>();
            state.put(key, list);
        }

        list.add(element);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("getBallots")
    @Produces(MediaType.APPLICATION_JSON)
    public BallotList getBallots() {
        List<PersistedBallot> list = state.get(BALLOTS, List.class);

        if (list == null) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return new BallotList(list);
    }

    @POST
    @Path("commitments")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCommitments(CommitmentDTO commitment) {
        addToList(COEFFICIENT_COMMITMENT, commitment);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("commitments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CommitmentDTO> getCommitments() {
        List<CommitmentDTO> list = state.get(COEFFICIENT_COMMITMENT, List.class);

        if (list == null) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return list;
    }

    @POST
    @Path("complain")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postComplaint(ComplaintDTO complaint) {
        addToList(COMPLAINTS, complaint);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("complaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ComplaintDTO> getComplaints() {
        List<ComplaintDTO> list = state.get(COMPLAINTS, List.class);

        return list != null ? list : new ArrayList<>();
    }

    @POST
    @Path("resolveComplaint")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resolveComplaint(ComplaintResolveDTO resolveDTO) {
        addToList(RESOLVED_COMPLAINTS, resolveDTO);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("complaintResolves")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ComplaintResolveDTO> getComplaintResolves() {
        List<ComplaintResolveDTO> list = state.get(RESOLVED_COMPLAINTS, List.class);

        return list != null ? list : new ArrayList<>();
    }

    @POST
    @Path("publicInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPublicInfo(PartialPublicInfo info) {
        addToList(PUBLIC_INFO, info);
    }

    @GET
    @Path("publicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PartialPublicInfo> getPublicInfo() {
        List<PartialPublicInfo> list = state.get(PUBLIC_INFO, List.class);

        return list != null ? list : new ArrayList<>();
    }


    @GET
    @Path("getCurrentTime")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCurrentTime() {
        return Long.toString(new Date().getTime());
    }
}
