package dk.mmj.eevhe.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.entities.PartialPublicInfo;
import dk.mmj.eevhe.entities.PersistedBallot;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.entities.wrappers.BallotWrapper;
import dk.mmj.eevhe.entities.wrappers.PublicInfoWrapper;
import dk.mmj.eevhe.entities.wrappers.StringListWrapper;
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

    private static final ObjectMapper mapper = new ObjectMapper();

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
     * @param logger            logger to be used in giving feedback
     * @param bulletinBoard     WebTarget pointing at bulletinBoard
     * @param validCertificates collection of valid BB-peer certificates
     * @return list of ballots from BulletinBoard
     */
    public static List<PersistedBallot> getBallots(Logger logger, WebTarget bulletinBoard, Collection<X509CertificateHolder> validCertificates) {
        return fetch(
                bulletinBoard.path("getBallots"),
                new TypeReference<BallotWrapper>() {
                },
                validCertificates,
                logger
        ).getContent();
    }

    /**
     * Retrieves a list of partialPublicInfos from the bulletinBoard
     * <br>
     * Only those with a valid signature by the sender is included in the list
     *
     * @param logger            logger used in error reporting
     * @param target            the webTarget to be used in fetching the information entities
     * @param electionPk        the parent certificate for the election
     * @param validCertificates collection of valid BB-peer certificates
     * @return list of PartialPublicInformation entities, which was properly signed by their senders
     */
    public static List<PartialPublicInfo> getPublicInfos(
            Logger logger,
            WebTarget target,
            AsymmetricKeyParameter electionPk,
            Collection<X509CertificateHolder> validCertificates) {
        try {
            WebTarget queryTarget = target.path("publicInfo");
            PublicInfoWrapper publicInfoList = fetch(
                    queryTarget,
                    new TypeReference<PublicInfoWrapper>() {
                    },
                    validCertificates,
                    logger);

            return publicInfoList.getContent().stream().filter(
                    i -> {
                        try {
                            return i.verifySignature(getSignaturePublicKey(i.getEntity(), electionPk, logger));
                        } catch (Exception e) {
                            logger.error("Failed to verify signature, due to JSON processing", e);
                            return false;
                        }
                    }
            ).map(SignedEntity::getEntity).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Exception occurred while fetching public infos", e);
            return null;//Avoid propagating error to rest of system
        }
    }


    /**
     * Utility method for fetching, and determining the value that the BB-peers agrees on
     *
     * @param target            webTarget to query - this is the exact endpoint to query
     * @param typeReference     typeReference to determine expected type from this method e.g. List of Ballots
     * @param validCertificates collection of valid certificates for BB-Peers
     * @param logger            logger to be used in reporting errors or warnings
     * @param <T>               return-type
     * @return the value(s) fetched and agreed upon by the BB-Peers. Null if no agreement is reached
     */
    public static <T> T fetch(
            WebTarget target,
            @SuppressWarnings("unused") TypeReference<T> typeReference,
            Collection<X509CertificateHolder> validCertificates,
            Logger logger
    ) {
        String serializedResponse = target.request().get(String.class);

        List<SignedEntity<T>> deserializedResponse;
        try {
            deserializedResponse = mapper.readValue(serializedResponse, new TypeReference<List<SignedEntity<T>>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize response", e);
        }

        return verifyAndDetermineCommon(deserializedResponse, validCertificates, logger);
    }

    /**
     * Helper method for fetching all bb-peer certificates
     *
     * @param logger     logger for reporting errors
     * @param edgeTarget webTarget pointing at an edge node
     * @param electionPk the parent certificate for the election
     * @return list of certificates for bb-peers on .pem format
     */
    public static List<X509CertificateHolder> getBBPeerCertificates(Logger logger, WebTarget edgeTarget, AsymmetricKeyParameter electionPk) {
        try {
            String responseString = edgeTarget.path("peerCertificates").request().get(String.class);

            ContentVerifierProvider verifier;
            try {
                verifier = new BcRSAContentVerifierProviderBuilder(new DefaultDigestAlgorithmIdentifierFinder())
                        .build(electionPk);
            } catch (OperatorCreationException e) {
                logger.error("Failed to create verifier for election pk");
                return null;
            }

            List<SignedEntity<StringListWrapper>> certificates;
            try {
                certificates = new ObjectMapper().readValue(responseString, new TypeReference<List<SignedEntity<StringListWrapper>>>() {
                });
            } catch (IOException e) {
                logger.error("FetchingUtilities: Failed to deserialize certificate list retrieved from bulletin board. Target: " + edgeTarget, e);
                return null;
            }

            //Build map of all valid certificates:
            Map<String, X509CertificateHolder> validCertificates = new HashMap<>();
            for (SignedEntity<StringListWrapper> se : certificates) {
                for (String certString : se.getEntity().getContent()) {
                    try {
                        X509CertificateHolder certHolder = CertificateHelper.readCertificate(certString.getBytes(StandardCharsets.UTF_8));
                        if (certHolder.isSignatureValid(verifier) && certHolder.getSubject().toString().toLowerCase().contains("bb_peer")) {
                            validCertificates.put(certHolder.getSubject().toString(), certHolder);
                        }
                    } catch (IOException | CertException ignored) {
                    }
                }
            }

            if (validCertificates.isEmpty()) {
                logger.warn("No valid BB-peer certificates found. ");
                return new ArrayList<>();
            }

            StringListWrapper certStrings = verifyAndDetermineCommon(certificates, validCertificates.values(), logger);
            return certStrings.getContent().stream().map(FetchingUtilities::toCertHolder).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to fetch BB peer certificates", e);
            return null;
        }
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
        List<SignedEntity<T>> uniqueEntities = verifySignedAndValid(entityList, validCertificates, logger);


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
    static <T> List<SignedEntity<T>> verifySignedAndValid(List<SignedEntity<T>> list, Collection<X509CertificateHolder> certificates, Logger logger) {
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
