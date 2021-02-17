package dk.mmj.eevhe.server.bulletinboard;


import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.ServerState;
import dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthority;
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

    @POST
    @Path("postPublicInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    public void initialize(PublicInformationEntity info) {
        addToList(PUBLIC_INFO, info);
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
        List list = state.get(key, List.class);

        if (list == null) {
            list = new ArrayList();
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
    public void postCommitments(BigInteger[] commitment){
        addToList(COEFFICIENT_COMMITMENT, commitment);
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("postCommitments")
    public List<BigInteger[]> getCommitments(){
        List<BigInteger[]> list = state.get(COEFFICIENT_COMMITMENT, List.class);

        if (list == null) {
            throw new NotFoundException("Voting has not been initialized");
        }

        return list;
    }


    @GET
    @Path("getCurrentTime")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCurrentTime() {
        return Long.toString(new Date().getTime());
    }
}
