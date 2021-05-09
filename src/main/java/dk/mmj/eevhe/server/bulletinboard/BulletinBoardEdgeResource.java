package dk.mmj.eevhe.server.bulletinboard;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.entities.wrappers.BallotWrapper;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyWebTarget;

import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.util.*;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;
import static dk.mmj.eevhe.server.bulletinboard.BulletinBoard.*;

@Path("/")
public class BulletinBoardEdgeResource {
    private final static Logger logger = LogManager.getLogger(BulletinBoardEdgeResource.class);
    private final ServerState state = ServerState.getInstance();
    private final List<JerseyWebTarget> targets = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    private List<String> getPeerAddresses() {
        return state.get(BulletinBoardEdge.PEER_ADDRESSES, List.class);
    }

    private List<JerseyWebTarget> getTargets() {
        if (targets.isEmpty()) {
            for (String peerAddress : getPeerAddresses()) {
                targets.add(configureWebTarget(logger, peerAddress));
            }
        }

        return targets;
    }

    /**
     * Waits for threads to finish for maximum 10 seconds
     *
     * @param threads List of threads
     */
    private void waitWithTimeout(List<Thread> threads) {
        long timeout = System.currentTimeMillis() + 10_000;
        for (Thread thread : threads) {
            try {
                thread.join(timeout - System.currentTimeMillis());
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Fetches list of objects from Bulletin Board Peers using given REST endpoint
     *
     * @param path          REST endpoint
     * @param typeReference Type reference needed for casting
     * @param <T>           Generic type parameter
     * @return Fetched object of type T
     */
    private <T> List<T> fetchFromPeers(String path, TypeReference<T> typeReference) {
        List<T> result = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();

        for (JerseyWebTarget target : getTargets()) {
            Thread thread = new Thread(() -> {
                String responseString = target.path(path).request().get(String.class);
                T responseObject = null;
                try {
                    responseObject = mapper.readValue(responseString, typeReference);
                    result.add(responseObject);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to query " + path + " from bulletin board peer: " + target, e);
                }
            });
            thread.start();

            threads.add(thread);
        }

        waitWithTimeout(threads);

        return result;
    }

    /**
     * Posts object of type T to all Bulletin Board Peers using given REST endpoint
     *
     * @param path   REST endpoint
     * @param entity JAX-RS entity containing object of type T to be posted
     * @param <T>    Type of object to be posted
     */
    private <T> void postToPeers(String path, Entity<T> entity) {
        List<Thread> threads = new ArrayList<>();

        for (JerseyWebTarget target : getTargets()) {
            Thread thread = new Thread(() -> target.path(path).request().post(entity));
            thread.start();

            threads.add(thread);
        }
    }

    @GET
    @Path("type")
    @Produces(MediaType.TEXT_HTML)
    public String getType() {
        logger.info("Received request for server type");
        return "<b>ServerType:</b> Bulletin Board Edge";
    }

    @GET
    @Path("getPublicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<SignedEntity<List<SignedEntity<PartialPublicInfo>>>> getPublicInfos() {
        return fetchFromPeers("getPublicInfo", new TypeReference<SignedEntity<List<SignedEntity<PartialPublicInfo>>>>() {
        });
    }

    @POST
    @Path("postBallot")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public void postBallot(BallotDTO ballot) {
        postToPeers("postBallot", Entity.entity(ballot, MediaType.APPLICATION_JSON));
    }

    /**
     * @return List of {@link BigInteger} which is partial decryptions
     */
    @GET
    @Path("result")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<SignedEntity<List<SignedEntity<PartialResultList>>>> getResult() {
        return fetchFromPeers("result", new TypeReference<SignedEntity<List<SignedEntity<PartialResultList>>>>() {
        });
    }

    @POST
    @Path("result")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postResult(SignedEntity<PartialResultList> partialDecryptions) {
        postToPeers("result", Entity.entity(partialDecryptions, MediaType.APPLICATION_JSON));
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("getBallots")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<BallotWrapper>> getBallots() {
        return fetchFromPeers("getBallots", new TypeReference<SignedEntity<BallotWrapper>>() {
        });
    }

    @GET
    @Path("getBallot/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<PersistedBallot>> getBallot(@PathParam("id") String id) {
        return fetchFromPeers("getBallot/" + id, new TypeReference<SignedEntity<PersistedBallot>>() {
        });
    }

    @POST
    @Path("commitments")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCommitments(SignedEntity<CommitmentDTO> commitment) {
        postToPeers("commitments", Entity.entity(commitment, MediaType.APPLICATION_JSON));
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("commitments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<List<SignedEntity<CommitmentDTO>>>> getCommitments() {
        return fetchFromPeers("commitments", new TypeReference<SignedEntity<List<SignedEntity<CommitmentDTO>>>>() {
        });
    }

    @POST
    @Path("pedersenComplain")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPedersenComplaint(SignedEntity<PedersenComplaintDTO> complaint) {
        postToPeers("pedersenComplain", Entity.entity(complaint, MediaType.APPLICATION_JSON));
    }

    @POST
    @Path("feldmanComplain")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postFeldmanComplaint(SignedEntity<FeldmanComplaintDTO> complaint) {
        postToPeers("feldmanComplain", Entity.entity(complaint, MediaType.APPLICATION_JSON));
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("pedersenComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<List<SignedEntity<PedersenComplaintDTO>>>> getPedersenComplaints() {
        return fetchFromPeers("pedersenComplaints", new TypeReference<SignedEntity<List<SignedEntity<PedersenComplaintDTO>>>>() {
        });
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("feldmanComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<List<SignedEntity<FeldmanComplaintDTO>>>> getFeldmanComplaints() {
        return fetchFromPeers("feldmanComplaints", new TypeReference<SignedEntity<List<SignedEntity<FeldmanComplaintDTO>>>>() {
        });
    }

    @POST
    @Path("resolveComplaint")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resolveComplaint(SignedEntity<ComplaintResolveDTO> resolveDTO) {
        postToPeers("resolveComplaint", Entity.entity(resolveDTO, MediaType.APPLICATION_JSON));
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("complaintResolves")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<List<SignedEntity<ComplaintResolveDTO>>>> getComplaintResolves() {
        return fetchFromPeers("complaintResolves", new TypeReference<SignedEntity<List<SignedEntity<ComplaintResolveDTO>>>>() {
        });
    }

    @POST
    @Path("publicInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPublicInfo(SignedEntity<PartialPublicInfo> info) {
        postToPeers("publicInfo", Entity.entity(info, MediaType.APPLICATION_JSON));
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("publicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<List<SignedEntity<PartialPublicInfo>>>> getPublicInfo() {
        return fetchFromPeers("publicInfo", new TypeReference<SignedEntity<List<SignedEntity<PartialPublicInfo>>>>() {
        });
    }

    @POST
    @Path("certificates")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCertificate(SignedEntity<CertificateDTO> cert) {
        postToPeers("certificates", Entity.entity(cert, MediaType.APPLICATION_JSON));
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("certificates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<List<SignedEntity<CertificateDTO>>>> getCertificate() {
        return fetchFromPeers("certificates", new TypeReference<SignedEntity<List<SignedEntity<CertificateDTO>>>>() {
        });
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
    public List<SignedEntity<List<String>>> getPeerCertificateList() {
        return fetchFromPeers("getPeerCertificateList", new TypeReference<SignedEntity<List<String>>>() {
        });
    }
}
