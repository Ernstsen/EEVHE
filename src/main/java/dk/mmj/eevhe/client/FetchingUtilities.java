package dk.mmj.eevhe.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.entities.BallotList;
import dk.mmj.eevhe.entities.PartialPublicInfo;
import dk.mmj.eevhe.entities.PersistedBallot;
import dk.mmj.eevhe.entities.SignedEntity;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.CertException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentVerifierProviderBuilder;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for fetching information from the <i>BulletinBoard</i>
 */
public class FetchingUtilities {

    /**
     * Fetches and verifies certificate for da with given id, and extracts it's publicKey
     *
     * @param daInfo            PartialPublicInfo for the DA
     * @param electionPublicKey certificate for the election
     * @param logger            logger to be used in error reporting
     * @return the public key, from the verified certificate
     */
    public static AsymmetricKeyParameter getSignaturePublicKey(
            PartialPublicInfo daInfo,
            AsymmetricKeyParameter electionPublicKey,
            Logger logger) {

        String certificateString = daInfo.getCertificate();
        try {
            X509CertificateHolder certificate = CertificateHelper.readCertificate(certificateString.getBytes(StandardCharsets.UTF_8));

            ContentVerifierProvider verifier = new BcRSAContentVerifierProviderBuilder(new DefaultDigestAlgorithmIdentifierFinder())
                    .build(electionPublicKey);

            boolean ownCertificate = certificate.getSubject().equals(new X500Name("CN=DA" + daInfo.getSenderId()));
            if (ownCertificate && certificate.isSignatureValid(verifier)) {
                return CertificateHelper.getPublicKeyFromCertificate(certificate);
            } else {
                logger.error("Invalid certificate for da with id=" + daInfo.getSenderId());
            }
        } catch (IOException | OperatorCreationException | CertException e) {
            logger.error("Failed to verify signature for certificate for da=" + daInfo.getSenderId(), e);
        }
        return null;
    }

    /**
     * Retrieves cast ballots from BulletinBoard
     *
     * @param logger        logger to be used in giving feedback
     * @param bulletinBoard WebTarget pointing at bulletinBoard
     * @return list of ballots from BulletinBoard
     */
    public static List<PersistedBallot> getBallots(Logger logger, WebTarget bulletinBoard) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String getVotes = bulletinBoard.path("getBallots").request().get(String.class);
            BallotList voteObjects = mapper.readValue(getVotes, BallotList.class);
            ArrayList<PersistedBallot> ballots = new ArrayList<>();

            for (Object ballot : voteObjects.getBallots()) {
                if (ballot instanceof PersistedBallot) {
                    ballots.add((PersistedBallot) ballot);
                } else {
                    logger.error("Found ballot that was not correct class. Was " + ballot.getClass() + ". Terminating server");
                    return null;
                }
            }
            return ballots;
        } catch (IOException e) {
            logger.error("Failed to read BallotList from JSON string", e);
            return null;
        }
    }

    /**
     * Retrieves a list of partialPublicInfos from the bulletinBoard
     * <br>
     * Only those with a valid signature by the sender is included in the list
     *
     * @param logger       logger used in error reporting
     * @param target       the webTarget to be used in fetching the information entities
     * @param electionPk the parent certificate for the election
     * @return list of PartialPublicInformation entities, which was properly signed by their senders
     */
    public static List<PartialPublicInfo> getPublicInfos(Logger logger, WebTarget target, AsymmetricKeyParameter electionPk) {
        Response response = target.path("publicInfo").request().get();
        String responseString = response.readEntity(String.class);

        List<SignedEntity<PartialPublicInfo>> publicInfoList;
        try {
            publicInfoList = new ObjectMapper().readValue(responseString, new TypeReference<List<SignedEntity<PartialPublicInfo>>>() {
            });
        } catch (IOException e) {
            logger.error("FetchingUtilities: Failed to deserialize public informations list retrieved from bulletin board", e);
            throw new RuntimeException("Failed to fet public infos list");
        }

        return publicInfoList.stream().filter(
                i -> {
                    try {
                        return i.verifySignature(getSignaturePublicKey(i.getEntity(), electionPk, logger));
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to verify signature, due to JSON processing", e);
                        return false;
                    }
                }
        ).map(SignedEntity::getEntity).collect(Collectors.toList());
    }
}
