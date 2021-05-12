package dk.mmj.eevhe.server.bulletinboard;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.entities.wrappers.*;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyWebTarget;

import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;

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
                try {
                    Response response = target.path(path).request().get();
                    if (response.getStatus() >= 400) {
                        logger.error("Received non-200 status:" + response.getStatus() + " from target: " + target.path(path));
                        return;
                    }
                    String responseString = response.readEntity(String.class);
                    T responseObject;
                    responseObject = mapper.readValue(responseString, typeReference);
                    result.add(responseObject);
                } catch (JsonProcessingException e) {
                    logger.error("Failed to query " + path + " from bulletin board peer: " + target, e);
                } catch (Exception e) {
                    logger.error("Exception occured while attempting to query " + path + " from bulletin board peer", e);
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
        for (JerseyWebTarget target : getTargets()) {
            new Thread(() -> target.path(path).request().post(entity)).start();
        }
    }

    @GET
    @Path("type")
    @Produces(MediaType.TEXT_HTML)
    public String getType() {
        logger.info("Received request for server type");
        return "<b>ServerType:</b> Bulletin Board Edge";
    }

    @POST
    @Path("postBallot")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postBallot(BallotDTO ballot) {
        postToPeers("postBallot", Entity.entity(ballot, MediaType.APPLICATION_JSON));
    }

    /**
     * @return List of {@link BigInteger} which is partial decryptions
     */
    @GET
    @Path("result")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<PartialResultWrapper>> getResult() {
        return fetchFromPeers("result", new TypeReference<SignedEntity<PartialResultWrapper>>() {
        });
    }

    @POST
    @Path("result")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postResult(SignedEntity<PartialResultList> partialDecryptions) {
        postToPeers("result", Entity.entity(partialDecryptions, MediaType.APPLICATION_JSON));
    }

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

    @GET
    @Path("commitments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<CommitmentWrapper>> getCommitments() {
        return fetchFromPeers("commitments", new TypeReference<SignedEntity<CommitmentWrapper>>() {
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

    @GET
    @Path("pedersenComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<PedersenComplaintWrapper>> getPedersenComplaints() {
        return fetchFromPeers("pedersenComplaints", new TypeReference<SignedEntity<PedersenComplaintWrapper>>() {
        });
    }

    @GET
    @Path("feldmanComplaints")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<FeldmanComplaintWrapper>> getFeldmanComplaints() {
        return fetchFromPeers("feldmanComplaints", new TypeReference<SignedEntity<FeldmanComplaintWrapper>>() {
        });
    }

    @POST
    @Path("resolveComplaint")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resolveComplaint(SignedEntity<ComplaintResolveDTO> resolveDTO) {
        postToPeers("resolveComplaint", Entity.entity(resolveDTO, MediaType.APPLICATION_JSON));
    }

    @GET
    @Path("complaintResolves")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<ComplaintResolveWrapper>> getComplaintResolves() {
        return fetchFromPeers("complaintResolves", new TypeReference<SignedEntity<ComplaintResolveWrapper>>() {
        });
    }

    @POST
    @Path("publicInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postPublicInfo(SignedEntity<PartialPublicInfo> info) {
        postToPeers("publicInfo", Entity.entity(info, MediaType.APPLICATION_JSON));
    }

    @GET
    @Path("publicInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<PublicInfoWrapper>> getPublicInfo() {
        return fetchFromPeers("publicInfo", new TypeReference<SignedEntity<PublicInfoWrapper>>() {
        });
    }

    @POST
    @Path("certificates")
    @Consumes(MediaType.APPLICATION_JSON)
    public void postCertificate(SignedEntity<CertificateDTO> cert) {
        postToPeers("certificates", Entity.entity(cert, MediaType.APPLICATION_JSON));
    }

    @GET
    @Path("certificates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<CertificatesWrapper>> getCertificate() {
        return fetchFromPeers("certificates", new TypeReference<SignedEntity<CertificatesWrapper>>() {
        });
    }

    @GET
    @Path("getCurrentTime")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCurrentTime() {
        return Long.toString(new Date().getTime());
    }

    @GET
    @Path("peerCertificates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SignedEntity<StringListWrapper>> getPeerCertificateList() {
        return fetchFromPeers("peerCertificates", new TypeReference<SignedEntity<StringListWrapper>>() {
        });
    }
}
