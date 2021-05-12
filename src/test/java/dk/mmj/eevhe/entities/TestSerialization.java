package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.client.results.ElectionResult;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.entities.wrappers.*;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.*;

@SuppressWarnings("deprecation")
public class TestSerialization {
    private List<Object> serializables;

    private static Object[] toObjectArray(Object val) {
        Object[] res = new Object[Array.getLength(val)];
        for (int i = 0; i < Array.getLength(val); i++) {
            res[i] = Array.get(val, i);
        }
        return res;
    }

    @Before
    public void setup() throws IOException {
        this.serializables = new ArrayList<>();
        CipherText cipherText = new CipherText(new BigInteger("13524651410"), new BigInteger("846021531684652196874"));
        serializables.add(cipherText);

        Proof proof = new Proof(
                new BigInteger("5610861890"), new BigInteger("156018910"),
                new BigInteger("1613486"), new BigInteger("01289444168469874"));
        serializables.add(proof);

        CandidateVoteDTO candidateVoteDTO = new CandidateVoteDTO(cipherText, "id", proof);
        CandidateVoteDTO candidateVoteDTO2 = new CandidateVoteDTO(new CipherText(BigInteger.ONE, BigInteger.TEN), "id2", proof);
        serializables.add(candidateVoteDTO);

        List<CandidateVoteDTO> candidates = Arrays.asList(candidateVoteDTO, candidateVoteDTO2);
        BallotDTO ballot = new BallotDTO(candidates, "ballotId", proof);
        BallotDTO ballot2 = new BallotDTO(candidates, "ballotIdad", proof);
        serializables.add(ballot);

        Candidate cand1 = new Candidate(0, "name", "desc");
        Candidate cand2 = new Candidate(1, "name2", "desc2");
        serializables.add(cand1);


        HashMap<Integer, BigInteger> map = new HashMap<>();
        map.put(821, BigInteger.valueOf(821));
        map.put(82112, BigInteger.valueOf(82112));
        serializables.add(new DistKeyGenResult(new BigInteger("2323"), new BigInteger("65109840"), map, map));

        PublicKey publicKey = new PublicKey(new BigInteger("34"), new BigInteger("3459"), new BigInteger("293857"));
        serializables.add(publicKey);

        serializables.add(new KeyPair(new BigInteger("785423"), publicKey));

        DLogProofUtils.Proof dlogProof = new DLogProofUtils.Proof(new BigInteger("654"), new BigInteger("69846"));
        PartialResult partialResult = new PartialResult(32, new BigInteger("2342"), dlogProof, cipherText);
        PartialResult partialResult2 = new PartialResult(54, new BigInteger("234221"), dlogProof, cipherText);
        serializables.add(partialResult);

        PartialResultList partialResultList = new PartialResultList(Arrays.asList(partialResult, partialResult2), 5, 1);
        PartialResultList partialResultList2 = new PartialResultList(Arrays.asList(partialResult2, partialResult), 5, 1);
        serializables.add(partialResultList);
        serializables.add(new PartialSecretKey(new BigInteger("23422"), new BigInteger("2342124")));

        PersistedBallot persistedBallot = new PersistedBallot(ballot);
        PersistedBallot persistedBallot2 = new PersistedBallot(ballot2);
        serializables.add(persistedBallot);
        PersistedVote pv1 = new PersistedVote(candidateVoteDTO);
        serializables.add(pv1);
        serializables.add(new PrimePair(new BigInteger("3253"), new BigInteger("3298573493")));

        String cert = new String(Files.readAllBytes(Paths.get("certs/test_glob.pem")));
        AsymmetricKeyParameter sk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));

        PeerInfo daInfo1 = new PeerInfo(0, "127.0.0.1:8080");
        PeerInfo daInfo2 = new PeerInfo(1, "127.0.0.1:8081");
        serializables.add(daInfo1);

        CommitmentDTO commitmentDTO = new CommitmentDTO(new BigInteger[]{new BigInteger("5464"), new BigInteger("641349646")}, 56, "FOO");
        CommitmentDTO commitmentDTO2 = new CommitmentDTO(new BigInteger[]{new BigInteger("54642"), new BigInteger("64149646")}, 526, "BAR");
        serializables.add(commitmentDTO);

        PedersenComplaintDTO pedersenComplaintDTO = new PedersenComplaintDTO(69849684, 12378612);
        PedersenComplaintDTO pedersenComplaintDTO2 = new PedersenComplaintDTO(9849684, 1237861);
        serializables.add(pedersenComplaintDTO);

        FeldmanComplaintDTO feldmanComplaintDTO = new FeldmanComplaintDTO(69849684, 12378612, new BigInteger("123"), new BigInteger("1234"));
        FeldmanComplaintDTO feldmanComplaintDTO2 = new FeldmanComplaintDTO(698496842, 123786132, new BigInteger("1232"), new BigInteger("123634"));
        serializables.add(feldmanComplaintDTO);

        PartialSecretMessageDTO partialSecretMessageDTO = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 1123, 12412);
        serializables.add(partialSecretMessageDTO);

        ComplaintResolveDTO complaintResolveDTO = new ComplaintResolveDTO(5874767, 1298376192, partialSecretMessageDTO);
        ComplaintResolveDTO complaintResolveDT2 = new ComplaintResolveDTO(58747657, 129837192, partialSecretMessageDTO);
        serializables.add(complaintResolveDTO);

        serializables.add(new DecryptionAuthorityInput("wiughweiugnwe", "woegnweoginw", "woegnweoginwqwf", 54684654, Arrays.asList(daInfo1, daInfo2), "Some CERT"));

        serializables.add(new PartialKeyPair(new PartialSecretKey(new BigInteger("123521"), new BigInteger("98273523"))
                , new BigInteger("123456789"), publicKey));

        PartialPublicInfo partialPublicInfo = new PartialPublicInfo(1, publicKey, new BigInteger("6513894"), Arrays.asList(cand1, cand2), 16318, cert);
        PartialPublicInfo partialPublicInfo2 = new PartialPublicInfo(2, publicKey, new BigInteger("13894"), Arrays.asList(cand2, cand1), 16311328, cert);
        serializables.add(partialPublicInfo);

        serializables.add(new ElectionResult(Arrays.asList(1, 2, 41, 12, 12541), 12412134));

        CertificateDTO certDTO = new CertificateDTO("this is very much a certificate ", 23);
        CertificateDTO certDTO2 = new CertificateDTO("this is very also much a certificate ", 232);
        serializables.add(certDTO);

        List<BBPeerInfo> peers = Arrays.asList(
                new BBPeerInfo(1, "123.123.123", "certificate stringy"),
                new BBPeerInfo(2, "321.321.321", cert)
        );

        List<PeerInfo> edges = Arrays.asList(
                new PeerInfo(1, "123.123.123"),
                new PeerInfo(2, "321.321.321")
        );

        serializables.add(new BBInput(peers, edges));

        serializables.add(new BAMessage("12", "", null, " a very likely sender"));
        serializables.add(new BAMessage("456", "MsgString...", null, " a very likely sender"));
        serializables.add(new BAMessage("7", null, true, " a very likely sender"));

        serializables.add(new BBPeerInfo(1, "127.0.0.1:8081", "asdasdasd"));

        serializables.add(new BallotWrapper(Arrays.asList(persistedBallot, persistedBallot2)));
        serializables.add(new CertificatesWrapper(Arrays.asList(new SignedEntity<>(certDTO, sk), new SignedEntity<>(certDTO2, sk))));
        serializables.add(new CommitmentWrapper(Arrays.asList(new SignedEntity<>(commitmentDTO, sk), new SignedEntity<>(commitmentDTO2, sk))));
        serializables.add(new ComplaintResolveWrapper(Arrays.asList(new SignedEntity<>(complaintResolveDTO, sk), new SignedEntity<>(complaintResolveDT2, sk))));
        serializables.add(new FeldmanComplaintWrapper(Arrays.asList(new SignedEntity<>(feldmanComplaintDTO, sk), new SignedEntity<>(feldmanComplaintDTO2, sk))));
        serializables.add(new PartialResultWrapper(Arrays.asList(new SignedEntity<>(partialResultList, sk), new SignedEntity<>(partialResultList2, sk))));
        serializables.add(new PedersenComplaintWrapper(Arrays.asList(new SignedEntity<>(pedersenComplaintDTO, sk), new SignedEntity<>(pedersenComplaintDTO2, sk))));
        serializables.add(new PublicInfoWrapper(Arrays.asList(new SignedEntity<>(partialPublicInfo, sk), new SignedEntity<>(partialPublicInfo2, sk))));
        serializables.add(new StringListWrapper(Arrays.asList("This", "is", "a", "list", "of", "strings")));

    }

    @Test
    public void testSerializeThenDeserialize() {
        List<String> errors = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();

        for (Object serializable : serializables) {
            try {
                String serialized = mapper.writeValueAsString(serializable);
                Object deserialize = mapper.readValue(serialized, serializable.getClass());

                if (!deserialize.equals(serializable)) {
                    errors.add("deserialized did not match original. Serialized: " + deserialize.toString() + ", Original: " + serializable.toString());
                } else { //Do more checks, which only makes sense if the the objects are equal
                    assertEquals("toString must be the same", serializable.toString(), deserialize.toString());
                    assertEquals("hashCode must be the same", serializable.hashCode(), deserialize.hashCode());
                }

            } catch (JsonProcessingException e) {
                errors.add("Unable to serialize object of type: " + serializable.getClass().getName() + " with error: " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            System.out.println("Failed " + errors.size() + "/" + serializables.size());
            for (String error : errors) {
                System.out.println(error);
            }
            fail();
        }

    }

    @Test
    public void testToString() {
        for (Object serializable : serializables) {
            Class<?> serializableClass = serializable.getClass();
            String str = serializable.toString();
            for (Field field : serializableClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;//static fields should not be in toString method
                }
                field.setAccessible(true);
                try {
                    Object val = field.get(serializable);
                    if (val != null) {
                        String valueString = val.getClass().isArray() ? Arrays.toString(toObjectArray(val)) : val.toString();
                        assertTrue(serializableClass.getName() + ": toString for DTO must contain all fields, did not contain: " + valueString, str.contains(valueString));
                    }
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }
    }
}
