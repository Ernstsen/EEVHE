package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSignedEntity {

    @Test
    public void constructSerializeDeserialize() throws IOException {

        AsymmetricKeyParameter pk = CertificateHelper.getPublicKeyFromCertificate(Paths.get("certs/test_glob.pem"));

        byte[] bytes = Files.readAllBytes(Paths.get("certs/test_glob.pem"));
        AsymmetricKeyParameter sk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));


        DecryptionAuthorityInfo daInfo1 = new DecryptionAuthorityInfo(0, "127.0.0.1:8080");
        DecryptionAuthorityInfo daInfo2 = new DecryptionAuthorityInfo(1, "127.0.0.1:8081");
        DecryptionAuthorityInput input = new DecryptionAuthorityInput("wiughweiugnwe", "woe" +
                "gnweoginw", "woegnweoginwqwf", 54684654, Arrays.asList(daInfo1, daInfo2),
                new String(bytes)
        );

        ObjectMapper mapper = new ObjectMapper();

        SignedEntity<DecryptionAuthorityInput> entity = new SignedEntity<>(input, sk);

        String serialized = mapper.writeValueAsString(entity);

        SignedEntity<DecryptionAuthorityInput> deserialized = mapper.readValue(serialized, new TypeReference<SignedEntity<DecryptionAuthorityInput>>() {
        });

        assertEquals("Serialization/deserialization should not introduce changes", entity, deserialized);

        assertTrue("Signature should be valid", entity.verifySignature(pk));
        assertEquals("Mismatch in hashes", entity.hashCode(), deserialized.hashCode());
        assertEquals("Mismatch in toString", entity.toString(), deserialized.toString());


    }

}
