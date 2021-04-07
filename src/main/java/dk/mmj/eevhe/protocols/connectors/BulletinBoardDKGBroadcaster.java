package dk.mmj.eevhe.protocols.connectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.interfaces.CertificateProvider;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGBroadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class BulletinBoardDKGBroadcaster implements DKGBroadcaster {
    private final Logger logger = LogManager.getLogger(BulletinBoardDKGBroadcaster.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final WebTarget target;
    private final AsymmetricKeyParameter secretKey;
    private final CertificateProvider certProvider;

    /**
     * @param target       bulletinBoard to post data to
     * @param secretKey    secret key, used in signing all outgoing data
     * @param certProvider provider for the certificates used in verification
     */
    public BulletinBoardDKGBroadcaster(WebTarget target,
                                       AsymmetricKeyParameter secretKey,
                                       CertificateProvider certProvider) {
        this.target = target;
        this.secretKey = secretKey;
        this.certProvider = certProvider;
    }

    @Override
    public void commit(CommitmentDTO commitmentDTO) {
        Entity<SignedEntity<CommitmentDTO>> commitmentsEntity = Entity.entity(new SignedEntity<>(commitmentDTO, secretKey), MediaType.APPLICATION_JSON);
        Response postCommitments = target.path("commitments").request().post(commitmentsEntity);
        if (postCommitments.getStatus() != 204) {
            logger.error("Failed to post commitment! Received status: " + postCommitments.getStatus());
        }

    }

    @Override
    public List<CommitmentDTO> getCommitments() {
        String commitmentsString = target.path("commitments").request().get(String.class);
        TypeReference<List<SignedEntity<CommitmentDTO>>> commitmentListType = new TypeReference<List<SignedEntity<CommitmentDTO>>>() {
        };

        try {
            return mapper.readValue(commitmentsString, commitmentListType).stream()
                    .filter(c -> verifySignature(c, c.getEntity().getId()))
                    .map(SignedEntity::getEntity).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to read commitments from BulletinBoard!", e);
            throw new RuntimeException("Failed to get commitments from BulletinBoard", e);
        }
    }

    private boolean verifySignature(SignedEntity<?> entity, int daId) {
        try {
            String certString = certProvider.generateCertMap().get(daId);
            AsymmetricKeyParameter pk = CertificateHelper.getPublicKeyFromCertificate(certString.getBytes(StandardCharsets.UTF_8));
            return entity.verifySignature(pk);
        } catch (Exception e) {
            logger.error("IOException occurred when verifying signature", e);
            return false;
        }
    }

    @Override
    public void pedersenComplain(PedersenComplaintDTO complaint) {
        logger.info("Sending complaint:" + complaint);
        Entity<SignedEntity<PedersenComplaintDTO>> entity = Entity.entity(new SignedEntity<>(complaint, secretKey), MediaType.APPLICATION_JSON);
        final Response response = target.path("pedersenComplain").request().post(entity);
        if (response.getStatus() != 204) {
            logger.error("Failed to post complaint! Received status: " + response.getStatus());
        }
    }

    @Override
    public List<PedersenComplaintDTO> getPedersenComplaints() {
        String complaintsString = target.path("pedersenComplaints").request().get(String.class);
        try {
            return mapper.readValue(complaintsString, new TypeReference<List<SignedEntity<PedersenComplaintDTO>>>() {
            }).stream().filter(c -> verifySignature(c, c.getEntity().getSenderId()))
                    .map(SignedEntity::getEntity)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to read pedersen complaint list from Bulletin Board", e);
            throw new RuntimeException("Unable to fetch pedersen complaints from BulletinBoard", e);
        }
    }

    @Override
    public void feldmanComplain(FeldmanComplaintDTO complaint) {
        logger.info("Sending complaint:" + complaint);
        Entity<SignedEntity<FeldmanComplaintDTO>> entity = Entity.entity(new SignedEntity<>(complaint, secretKey), MediaType.APPLICATION_JSON);
        final Response response = target.path("feldmanComplain").request().post(entity);
        if (response.getStatus() != 204) {
            logger.error("Failed to post complaint! Received status: " + response.getStatus());
        }
    }

    @Override
    public List<FeldmanComplaintDTO> getFeldmanComplaints() {
        String complaintsString = target.path("feldmanComplaints").request().get(String.class);
        try {
            return mapper.readValue(complaintsString, new TypeReference<List<SignedEntity<FeldmanComplaintDTO>>>() {
            }).stream().filter(c -> verifySignature(c, c.getEntity().getSenderId()))
                    .map(SignedEntity::getEntity)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to read feldman complaint list from Bulletin Board", e);
            throw new RuntimeException("Unable to fetch feldman complaints from BulletinBoard", e);
        }
    }

    @Override
    public void resolveComplaint(ComplaintResolveDTO complaintResolveDTO) {
        final Response response = target.path("resolveComplaint").request()
                .post(Entity.entity(new SignedEntity<>(complaintResolveDTO, secretKey), MediaType.APPLICATION_JSON));
        if (response.getStatus() != 204) {
            logger.error("Failed to post complaint resolve! Received status: " + response.getStatus());
        }
    }

    @Override
    public List<ComplaintResolveDTO> getResolves() {
        String complaintResolvesString = target.path("complaintResolves").request().get(String.class);
        try {
            return mapper.readValue(complaintResolvesString, new TypeReference<List<SignedEntity<ComplaintResolveDTO>>>() {
            }).stream().filter(c -> verifySignature(c, c.getEntity().getComplaintResolverId()))
                    .map(SignedEntity::getEntity)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to read resolved complaints from BulletinBoard. Failing", e);
            throw new RuntimeException("Failed to get complaint resolves from BulletinBoard", e);
        }
    }
}
