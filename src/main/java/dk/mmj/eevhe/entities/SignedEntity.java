package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.signature.SignatureHelper;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoardState;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.util.Collections;
import java.util.Objects;

@SuppressWarnings("unused")
public class SignedEntity<T> implements BulletinBoardUpdatable {

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "entityClass")
    private T entity;
    private String signature;

    protected SignedEntity() {
    }

    public SignedEntity(T entity, AsymmetricKeyParameter sk) {
        this(entity, sk, new ObjectMapper());
    }

    public SignedEntity(T entity, AsymmetricKeyParameter sk, ObjectMapper mapper) {
        try {
            this.entity = entity;
            signature = SignatureHelper.sign(sk, Collections.singletonList(mapper.writeValueAsBytes(entity)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize", e);
        }
    }

    public T getEntity() {
        return entity;
    }

    protected void setEntity(T entity) {
        this.entity = entity;
    }

    public String getSignature() {
        return signature;
    }

    protected void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignedEntity<?> that = (SignedEntity<?>) o;
        return Objects.equals(entity, that.entity) && Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, signature);
    }

    @Override
    public String toString() {
        return "SignedEntity{" +
                "entity=" + entity +
                ", signature='" + signature + '\'' +
                '}';
    }

    /**
     * Verifies signature with given pk
     *
     * @param pk corresponding public key
     * @return whether signature is valid
     * @throws JsonProcessingException if serialization fails
     */
    public boolean verifySignature(AsymmetricKeyParameter pk) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return SignatureHelper.verifySignature(pk, Collections.singletonList(mapper.writeValueAsBytes(entity)), signature);
    }

    @Override
    public void update(BulletinBoardState bb) {//TODO: This can be done in a cleaner fashion!
        if (entity instanceof PartialResultList) {
            bb.addResult((SignedEntity<PartialResultList>) this);
        } else if (entity instanceof PartialPublicInfo) {
            bb.addSignedPartialPublicInfo((SignedEntity<PartialPublicInfo>) this);
        } else if (entity instanceof CommitmentDTO) {
            bb.addSignedCommitment((SignedEntity<CommitmentDTO>) this);
        } else if (entity instanceof PedersenComplaintDTO) {
            bb.addSignedPedersenComplaint((SignedEntity<PedersenComplaintDTO>) this);
        } else if (entity instanceof FeldmanComplaintDTO) {
            bb.addSignedFeldmanComplaint((SignedEntity<FeldmanComplaintDTO>) this);
        } else if (entity instanceof ComplaintResolveDTO) {
            bb.addSignedComplaintResolve((SignedEntity<ComplaintResolveDTO>) this);
        } else if (entity instanceof CertificateDTO) {
            bb.addSignedCertificate((SignedEntity<CertificateDTO>) this);
        }
    }
}
