package dk.mmj.evhe.server.bulletinboard;


import dk.mmj.evhe.entities.*;
import dk.mmj.evhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dk.mmj.evhe.server.bulletinboard.BulletinBoard.*;

@Path("/")
public class BulletinBoardResource {
    private static final String ID_LIST = "idList";
    private static final String PUBLIC_INFO = "publicInfo";
    private static Logger logger = LogManager.getLogger(BulletinBoardResource.class);
    private ServerState state = ServerState.getInstance();

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
        if (publicKey == null) {//TODO: Only once?
            logger.warn("A submitted key CANNOT be null");
            throw new NotAllowedException("Key was null");

        }

        state.put(PUBLIC_KEY, publicKey);
    }

    @POST
    @Path("postPublicInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public void initialize(PublicInformationEntity info) {
        List<PublicInformationEntity> list = state.get(PUBLIC_INFO, List.class);
        if (list == null) {
            list = new ArrayList<>();
            state.put(PUBLIC_INFO, list);
        }
        list.add(info);
    }

    @GET
    @Path("getPublicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public PublicInfoList getPublicInfos() {
        List<PublicInformationEntity> list = state.get(PUBLIC_INFO, List.class);
        if (list == null) {
            logger.warn("Attempt to fetch public infos before they were created");
            throw new NotFoundException();
        }
        return new PublicInfoList(list);
    }

    @POST
    @Path("vote")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public void Vote(VoteDTO vote) {
        if (state.get(RESULT, String.class) != null) {
            logger.warn("A vote as attempted to be cast after voting had been terminated");
            throw new NotAllowedException("Voting has been terminated");
        }

        Set hasVoted = state.get(HAS_VOTED, HashSet.class);
        ArrayList idList = state.get(ID_LIST, ArrayList.class);
        Boolean isTest = state.get(IS_TEST, Boolean.class);
        String voterId = vote.getId();

        if (!isTest && !idList.contains(voterId)) {
            logger.warn("Unrecognized voter with id=" + voterId);
            throw new NotAllowedException("Vote was attempted with unrecognized id=" + voterId);
        }

        if (hasVoted.contains(voterId)) {//TODO: Incompatible with BB?
            logger.warn("Voter with id=" + voterId + " attempted to vote more than once");
            throw new NotAllowedException("A vote has already been registered with this ID");
        }

        List votes = state.get(VOTES, ArrayList.class);
        votes.add(new PersistedVote(vote));
        hasVoted.add(voterId);
    }

    @GET
    @Path("result")
    @Produces(MediaType.TEXT_HTML)
    public String getResult() {
        String result = state.get(RESULT, String.class);
        if (result == null) {
            return "<h3> Voting has not yet finished </h3>";
        }
        return "<h3> Voting has finished </h3> <br/>" + result;
    }

    @POST
    @Path("result")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postResult(String result) {
        state.put(RESULT, result);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("getVotes")
    @Produces(MediaType.APPLICATION_JSON)
    public VoteList getVotes() {
        List<PersistedVote> list = state.get(VOTES, List.class);
        if (list == null) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return new VoteList(list);
    }
}
