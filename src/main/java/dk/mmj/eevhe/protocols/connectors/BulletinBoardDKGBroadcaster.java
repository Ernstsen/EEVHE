package dk.mmj.eevhe.protocols.connectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.client.FetchingUtilities;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.entities.wrappers.CommitmentWrapper;
import dk.mmj.eevhe.entities.wrappers.ComplaintResolveWrapper;
import dk.mmj.eevhe.entities.wrappers.FeldmanComplaintWrapper;
import dk.mmj.eevhe.entities.wrappers.PedersenComplaintWrapper;
import dk.mmj.eevhe.interfaces.CertificateProvider;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGBroadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.glassfish.jersey.internal.util.Producer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BulletinBoardDKGBroadcaster implements DKGBroadcaster {
    private final Logger logger = LogManager.getLogger(BulletinBoardDKGBroadcaster.class);

    private final WebTarget target;
    private final AsymmetricKeyParameter secretKey;
    private final CertificateProvider certProvider;
    private final Producer<Collection<X509CertificateHolder>> bbCertificateProducer;

    /**
     * @param target                bulletinBoard to post data to
     * @param secretKey             secret key, used in signing all outgoing data
     * @param certProvider          provider for the certificates used in verification
     * @param bbCertificateProducer function producing collection of valid certificates for BB-peers
     */
    public BulletinBoardDKGBroadcaster(WebTarget target,
                                       AsymmetricKeyParameter secretKey,
                                       CertificateProvider certProvider,
                                       Producer<Collection<X509CertificateHolder>> bbCertificateProducer) {
        this.target = target;
        this.secretKey = secretKey;
        this.certProvider = certProvider;
        this.bbCertificateProducer = bbCertificateProducer;
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
        CommitmentWrapper wrappedCommitments = FetchingUtilities.fetch(
                target.path("commitments"),
                new TypeReference<CommitmentWrapper>() {
                },
                bbCertificateProducer.call(),
                logger
        );
        return wrappedCommitments.getContent().stream()
                .filter(c -> verifySignature(c, c.getEntity().getId()))
                .map(SignedEntity::getEntity).collect(Collectors.toList());
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
        PedersenComplaintWrapper wrappedComplaints = FetchingUtilities.fetch(
                target.path("pedersenComplaints"),
                new TypeReference<PedersenComplaintWrapper>() {
                },
                bbCertificateProducer.call(),
                logger
        );

        return wrappedComplaints.getContent().stream()
                .filter(c -> verifySignature(c, c.getEntity().getSenderId()))
                .map(SignedEntity::getEntity)
                .collect(Collectors.toList());
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
        FeldmanComplaintWrapper wrappedComplaints = FetchingUtilities.fetch(
                target.path("feldmanComplaints"),
                new TypeReference<FeldmanComplaintWrapper>() {
                },
                bbCertificateProducer.call(),
                logger
        );

        return wrappedComplaints.getContent().stream().filter(c -> verifySignature(c, c.getEntity().getSenderId()))
                .map(SignedEntity::getEntity)
                .collect(Collectors.toList());
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
        ComplaintResolveWrapper wrappedResolves = FetchingUtilities.fetch(
                target.path("complaintResolves"),
                new TypeReference<ComplaintResolveWrapper>() {
                },
                bbCertificateProducer.call(),
                logger
        );

        return wrappedResolves.getContent().stream().filter(c -> verifySignature(c, c.getEntity().getComplaintResolverId()))
                .map(SignedEntity::getEntity)
                .collect(Collectors.toList());
    }
}
