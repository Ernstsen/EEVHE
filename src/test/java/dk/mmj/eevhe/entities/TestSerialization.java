package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.*;

@SuppressWarnings("deprecation")
public class TestSerialization {
    private List<Object> serializables;

    @Before
    public void setup() {
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
        BallotDTO ballot2 = new BallotDTO(candidates, "ballotId2", proof);
        serializables.add(ballot);
        serializables.add(new BallotList(Arrays.asList(new PersistedBallot(ballot), new PersistedBallot(ballot2))));

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
        PartialResult partialResult = new PartialResult(32, new BigInteger("2342"), dlogProof, cipherText, 12);
        PartialResult partialResult2 = new PartialResult(54, new BigInteger("234221"), dlogProof, cipherText, 122);
        serializables.add(partialResult);

        PartialResultList partialResultList = new PartialResultList(Arrays.asList(partialResult, partialResult2), 5);
        PartialResultList partialResultList2 = new PartialResultList(Arrays.asList(partialResult2, partialResult), 94);
        serializables.add(partialResultList);
        serializables.add(new PartialSecretKey(new BigInteger("23422"), new BigInteger("2342124"), new BigInteger("235423523")));

        serializables.add(new PersistedBallot(ballot));
        PersistedVote pv1 = new PersistedVote(candidateVoteDTO);
        serializables.add(pv1);
        serializables.add(new PrimePair(new BigInteger("3253"), new BigInteger("3298573493")));

        serializables.add(new ResultList(Arrays.asList(partialResultList, partialResultList2)));
        DecryptionAuthorityInfo daInfo1 = new DecryptionAuthorityInfo(0, "127.0.0.1:8080");
        DecryptionAuthorityInfo daInfo2 = new DecryptionAuthorityInfo(1, "127.0.0.1:8081");
        serializables.add(daInfo1);
        serializables.add(new CommitmentDTO(new BigInteger[]{new BigInteger("5464"), new BigInteger("641349646")}, 56, "FOO"));
        serializables.add(new PedersenComplaintDTO(69849684, 12378612));
        serializables.add(new FeldmanComplaintDTO(69849684, 12378612, new BigInteger("123"), new BigInteger("1234")));
        PartialSecretMessageDTO partialSecretMessageDTO = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 1123, 12412);
        serializables.add(partialSecretMessageDTO);
        serializables.add(new ComplaintResolveDTO(5874767, 1298376192, partialSecretMessageDTO));
        serializables.add(new DecryptionAuthorityInput("wiughweiugnwe", "woegnweoginw", 54684654, Arrays.asList(daInfo1, daInfo2)));

        serializables.add(new PartialKeyPair(new PartialSecretKey(new BigInteger("123521"), new BigInteger("8734534"), new BigInteger("98273523"))
                , new BigInteger("123456789"), publicKey));
        serializables.add(new PartialPublicInfo(1, publicKey, new BigInteger("6513894"), Arrays.asList(cand1, cand2), 16318));
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
                field.setAccessible(true);
                try {
                    Object val = field.get(serializable);
                    String valueString = val.getClass().isArray() ? Arrays.toString(toObjectArray(val)) : val.toString();
                    assertTrue(serializableClass.getName() + ": toString for DTO must contain all fields", str.contains(valueString));
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }
    }

    private static Object[] toObjectArray(Object val){
        Object[] res = new Object[Array.getLength(val)];
        for (int i = 0; i < Array.getLength(val); i++) {
            res[i] = Array.get(val, i);
        }
        return res;
    }
}
