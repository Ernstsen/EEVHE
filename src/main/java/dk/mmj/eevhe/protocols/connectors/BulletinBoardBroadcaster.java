package dk.mmj.eevhe.protocols.connectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.ComplaintDTO;
import dk.mmj.eevhe.entities.ComplaintResolveDTO;
import dk.mmj.eevhe.protocols.interfaces.Broadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

public class BulletinBoardBroadcaster implements Broadcaster {
    private final Logger logger = LogManager.getLogger(BulletinBoardBroadcaster.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final WebTarget target;

    public BulletinBoardBroadcaster(WebTarget target) {
        this.target = target;
    }

    @Override
    public void commit(CommitmentDTO commitmentDTO) {
        Entity<CommitmentDTO> commitmentsEntity = Entity.entity(commitmentDTO, MediaType.APPLICATION_JSON);
        Response postCommitments = target.path("commitments").request().post(commitmentsEntity);
        if (postCommitments.getStatus() != 204) {
            logger.error("Failed to post commitment! Received status: " + postCommitments.getStatus());
        }
    }

    @Override
    public List<CommitmentDTO> getCommitments() {
        String commitmentsString = target.path("commitments").request().get(String.class);
        TypeReference<List<CommitmentDTO>> commitmentListType = new TypeReference<List<CommitmentDTO>>() {
        };

        try {
            return mapper.readValue(commitmentsString, commitmentListType);
        } catch (IOException e) {
            logger.error("Failed to read commitments from BulletinBoard!", e);
            throw new RuntimeException("Failed to get commitments from BulletinBoard", e);
        }
    }

    @Override
    public void complain(ComplaintDTO complaint) {
        logger.info("Sending complaint:" + complaint);
        Entity<ComplaintDTO> entity = Entity.entity(complaint, MediaType.APPLICATION_JSON);
        final Response response = target.path("complain").request().post(entity);
        if (response.getStatus() != 204) {
            logger.error("Failed to post complaint! Received status: " + response.getStatus());
        }
    }

    @Override
    public List<ComplaintDTO> getComplaints() {
        String complaintsString = target.path("complaints").request().get(String.class);
        try {
            return mapper.readValue(complaintsString, new TypeReference<List<ComplaintDTO>>() {
            });
        } catch (IOException e) {
            logger.error("Failed to read complaint list from Bulletin Board", e);
            throw new RuntimeException("Unable to fetch complaints from BulletinBoard", e);
        }
    }

    @Override
    public void resolveComplaint(ComplaintResolveDTO complaintResolveDTO) {
        final Response response = target.path("resolveComplaint").request().post(Entity.entity(complaintResolveDTO, MediaType.APPLICATION_JSON));
        if (response.getStatus() != 204) {
            logger.error("Failed to post complaint resolve! Received status: " + response.getStatus());
        }
    }

    @Override
    public List<ComplaintResolveDTO> getResolves() {
        String complaintResolvesString = target.path("complaintResolves").request().get(String.class);
        try {
            return mapper.readValue(complaintResolvesString, new TypeReference<List<ComplaintResolveDTO>>() {
            });
        } catch (IOException e) {
            logger.error("Failed to read resolved complaints from BulletinBoard. Failing", e);
            throw new RuntimeException("Failed to get complaint resolves from BulletinBoard", e);
        }
    }
}
