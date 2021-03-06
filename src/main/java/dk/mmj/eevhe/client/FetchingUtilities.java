package dk.mmj.eevhe.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.entities.BallotList;
import dk.mmj.eevhe.entities.PartialPublicInfo;
import dk.mmj.eevhe.entities.PersistedBallot;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility class for fetching information from the <i>BulletinBoard</i>
 */
public class FetchingUtilities {

    /**
     * Retrieves cast ballots from BulletinBoard
     *
     * @param logger logger to be used in giving feedback
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
     * Fetches a list of  {@link PartialPublicInfo}s from the BulletinBoard, supplied as a {@link WebTarget}.
     *
     * @param logger        logger for reporting errors
     * @param publicKeyName name of the file in the <i>RSA</i> folder, containing the public-key
     * @param target        webTarget pointing at the <i>BulletinBoard</i>
     * @return a public information signed by the Trusted Dealer, if any is found. Null otherwise
     */
    public static PartialPublicInfo fetchPublicInfo(Logger logger, String publicKeyName, WebTarget target) {
        List<PartialPublicInfo> publicInfoList = getPublicInfos(logger, target);
        if (publicInfoList == null) return null;//Never happens

        Optional<PartialPublicInfo> any = publicInfoList.stream()
                .filter(getVerifier(logger, publicKeyName))
                .findAny();

        if (!any.isPresent()) {
            logger.error("No public information retrieved from the server was signed by the trusted dealer. Terminating");
            System.exit(-1);
            return null;//Never happens
        }
        return any.get();
    }

    public static List<PartialPublicInfo> getPublicInfos(Logger logger, WebTarget target) {
        Response response = target.path("publicInfo").request().buildGet().invoke();
        String responseString = response.readEntity(String.class);

        List<PartialPublicInfo> publicInfoList;
        try {
            publicInfoList = new ObjectMapper().readValue(responseString, new TypeReference<List<PartialPublicInfo>>() {
            });
        } catch (IOException e) {
            logger.error("FetchingUtilities: Failed to deserialize public informations list retrieved from bulletin board", e);
            throw new RuntimeException("Failed to fet public infos list");
        }
        return publicInfoList;
    }

    /**
     * Creates a predicate, returning true if the public keys loads without error,
     * and a given {@link PartialPublicInfo} is signed with the corresponding secret-key
     *
     * @param logger        logger for reporting errors
     * @param publicKeyName name of the file in the <i>RSA</i> folder, containing the public-key
     * @return Predicate returning true if a publicInformationEntity is verified, false otherwise
     */
    private static Predicate<PartialPublicInfo> getVerifier(Logger logger, String publicKeyName) {
//        AsymmetricKeyParameter pk = loadPublicKey(logger, publicKeyName);
//
//        if (pk == null) {
//            return (info) -> false;
//        }
//
//        return informationEntity -> {
//            RSADigestSigner digest = new RSADigestSigner(new SHA256Digest());
//            digest.init(false, pk);
//            informationEntity.updateSigner(digest);
//            byte[] encodedSignature = informationEntity.getSignature().getBytes();
//
//            return digest.verifySignature(Base64.decode(encodedSignature));
//        };
        return (i) -> true;
    }

}
