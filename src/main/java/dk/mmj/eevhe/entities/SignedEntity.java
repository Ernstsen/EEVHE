package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.signature.SignatureHelper;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.util.Collections;
import java.util.Objects;

public class SignedEntity<T> {

    private T entity;
    private String signature;

    protected SignedEntity() {
    }

    public SignedEntity(T entity, AsymmetricKeyParameter sk) throws JsonProcessingException {
        this(entity, sk, new ObjectMapper());
    }

    public SignedEntity(T entity, AsymmetricKeyParameter sk, ObjectMapper mapper) throws JsonProcessingException {
        this.entity = entity;
        signature = SignatureHelper.sign(sk, Collections.singletonList(mapper.writeValueAsBytes(entity)));
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

    public boolean verifySignature(AsymmetricKeyParameter pk) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return SignatureHelper.verifySignature(pk, Collections.singletonList(mapper.writeValueAsBytes(entity)), signature);
    }
}
