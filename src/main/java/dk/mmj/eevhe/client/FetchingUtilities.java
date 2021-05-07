package dk.mmj.eevhe.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
        } catch (Exception e) {
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
            return mapper.readValue(getVotes, new TypeReference<List<PersistedBallot>>() {
            });
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
     * @param logger     logger used in error reporting
     * @param target     the webTarget to be used in fetching the information entities
     * @param electionPk the parent certificate for the election
     * @return list of PartialPublicInformation entities, which was properly signed by their senders
     */
    public static List<PartialPublicInfo> getPublicInfos(Logger logger, WebTarget target, AsymmetricKeyParameter electionPk) {
        String responseString = target.path("publicInfo").request().get(String.class);

        List<SignedEntity<PartialPublicInfo>> publicInfoList;
        try {
            publicInfoList = new ObjectMapper().readValue(responseString, new TypeReference<List<SignedEntity<PartialPublicInfo>>>() {
            });
        } catch (IOException e) {
            logger.error("FetchingUtilities: Failed to deserialize public informations list retrieved from bulletin board", e);
            return null;
        }

        return publicInfoList.stream().filter(
                i -> {
                    try {
                        return i.verifySignature(getSignaturePublicKey(i.getEntity(), electionPk, logger));
                    } catch (Exception e) {
                        logger.error("Failed to verify signature, due to JSON processing", e);
                        return false;
                    }
                }
        ).map(SignedEntity::getEntity).collect(Collectors.toList());
    }

    /**
     * Helper method for fetching all bb-peer certificates
     *
     * @param logger     logger for reporting errors
     * @param edgeTarget webTarget pointing at an edge node
     * @param electionPk the parent certificate for the election
     * @return list of certificates for bb-peers on .pem format
     */
    static List<String> getBBPeerCertificates(Logger logger, WebTarget edgeTarget, AsymmetricKeyParameter electionPk) {
        String responseString = edgeTarget.path("certificates").request().get(String.class);

        ContentVerifierProvider verifier;
        try {
            verifier = new BcRSAContentVerifierProviderBuilder(new DefaultDigestAlgorithmIdentifierFinder())
                    .build(electionPk);
        } catch (OperatorCreationException e) {
            logger.error("Failed to create verifier for election pk");
            return null;
        }

        List<SignedEntity<List<String>>> certificates;
        try {
            certificates = new ObjectMapper().readValue(responseString, new TypeReference<List<SignedEntity<List<String>>>>() {
            });
        } catch (IOException e) {
            logger.error("FetchingUtilities: Failed to deserialize certificate list retrieved from bulletin board", e);
            return null;
        }

        //Build map of all valid certificates:
        Map<String, X509CertificateHolder> validCertificates = new HashMap<>();
        for (SignedEntity<List<String>> se : certificates) {
            for (String certString : se.getEntity()) {
                try {
                    X509CertificateHolder certHolder = CertificateHelper.readCertificate(certString.getBytes(StandardCharsets.UTF_8));
                    if (certHolder.isSignatureValid(verifier) && certHolder.getSubject().toString().contains("BB_PEER")) {
                        validCertificates.put(certHolder.getSubject().toString(), certHolder);
                    }
                } catch (IOException | CertException ignored) {
                }
            }
        }

        return verifyAndDetermineCommon(certificates, validCertificates.values(), logger);
    }

    /**
     * Iterates through all passed items, and verifies their signatures.
     * <br>
     * Ensures that each entity has posted at most one of the entities
     * <br>
     * Executes simple 'vote' to determine valid entity - most votes win
     *
     * @param entityList        list of entities to be verified, and have commonly posted one found
     * @param validCertificates collection of the valid certificates
     * @param <T>               entity type
     * @return the entity list with the most unique, valid, signatures
     */
    static <T> T verifyAndDetermineCommon(List<SignedEntity<T>> entityList, Collection<X509CertificateHolder> validCertificates, Logger logger) {
        //We ensure that each sender has only posted ONE message - if more than one has been posted, first is used
        List<SignedEntity<T>> uniqueEntities = verifySignedAndValidInner(entityList, validCertificates, logger);


        //We count how many times each item was posted
        HashMap<T, Integer> countMap = new HashMap<>();
        uniqueEntities.stream().map(SignedEntity::getEntity)
                .forEach(c -> countMap.compute(c, (k, cnt) -> cnt != null ? cnt += 1 : 1));

        //Return most commonly posted
        return countMap.entrySet()
                .stream()
                .reduce((curr, element) -> curr.getValue() > element.getValue() ? curr : element)
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Iterates through both signed entities and certificates and returns a list of signed entities that is validly signed
     * <br>
     * If one certificate is used in signing multiple entities, only the first entity encountered is included in the result
     *
     * @param list         list of signed entities
     * @param certificates list of all valid certificates on .pem format
     * @param logger       logger to be used if an error is encountered
     * @param <T>          type parameter for the entity
     * @return list of signed entities, each signed by different entities, all with valid signatures
     */
    static <T> List<SignedEntity<T>> verifySignedAndValid(List<SignedEntity<T>> list, List<String> certificates, Logger logger) {
        return verifySignedAndValidInner(list, certificates.stream().map(FetchingUtilities::toCertHolder).collect(Collectors.toList()), logger);
    }

    private static X509CertificateHolder toCertHolder(String cert) {
        try {
            return CertificateHelper.readCertificate(cert.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Iterates through both signed entities and certificates and returns a list of signed entities that is validly signed
     * <br>
     * If one certificate is used in signing multiple entities, only the first entity encountered is included in the result
     *
     * @param list         list of signed entities
     * @param certificates list of all valid certificates
     * @param logger       logger to be used if an error is encountered
     * @param <T>          type parameter for the entity
     * @return list of signed entities, each signed by different entities, all with valid signatures
     */
    static <T> List<SignedEntity<T>> verifySignedAndValidInner(List<SignedEntity<T>> list, Collection<X509CertificateHolder> certificates, Logger logger) {
        List<SignedEntity<T>> result = new ArrayList<>();

        HashSet<X509CertificateHolder> unusedCerts = new HashSet<>(certificates);

        for (SignedEntity<T> entity : list) {

            Optional<X509CertificateHolder> any = unusedCerts.stream()
                    .filter(c -> verifySignedEntity(entity, c, logger))
                    .findAny();
            if (any.isPresent()) {
                result.add(entity);
                unusedCerts.remove(any.get());
            }
        }

        return result;
    }

    private static boolean verifySignedEntity(SignedEntity<?> entity, X509CertificateHolder cert, Logger logger) {
        try {
            return entity.verifySignature(CertificateHelper.getPublicKeyFromCertificate(cert));
        } catch (IOException exception) {
            logger.error("Failed to get PK from certificate string", exception);
        }
        return false;
    }
}
