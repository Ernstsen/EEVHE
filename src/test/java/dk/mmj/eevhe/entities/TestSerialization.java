package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.fail;

public class TestSerialization {
    private List<Object> serializables;

    @Before
    public void setup() {
        List<Object> serializables = this.serializables = new ArrayList<>();
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

        PartialResultList partialResultList = new PartialResultList(Arrays.asList(partialResult, partialResult2));
        PartialResultList partialResultList2 = new PartialResultList(Arrays.asList(partialResult2, partialResult));
        serializables.add(partialResultList);
        serializables.add(new PartialSecretKey(new BigInteger("23422"), new BigInteger("235423523")));

        serializables.add(new PersistedBallot(ballot));
        PersistedVote pv1 = new PersistedVote(candidateVoteDTO);
        PersistedVote pv2 = new PersistedVote(candidateVoteDTO2);
        serializables.add(pv1);
        serializables.add(new PrimePair(new BigInteger("3253"), new BigInteger("3298573493")));

        List<Integer> ints = Arrays.asList(1, 2, 3);
        PublicInformationEntity publicInfo1 = new PublicInformationEntity(
                ints, map,
                new BigInteger("3498534"), new BigInteger("35983493"), new BigInteger("344598343"),
                2378462384623L, Arrays.asList(cand1, cand2)
        );
        PublicInformationEntity publicInfo2 = new PublicInformationEntity(
                ints, map,
                new BigInteger("23498534"), new BigInteger("359283493"), new BigInteger("3445983432"),
                22378462384623L, Arrays.asList(cand2, cand1)
        );
        serializables.add(publicInfo1);
        serializables.add(new PublicInfoList(Arrays.asList(publicInfo1, publicInfo2)));

        serializables.add(new ResultList(Arrays.asList(partialResultList, partialResultList2)));
        serializables.add(new VoteList(Arrays.asList(pv1, pv2)));
    }


    @Test
    public void testSerializeThenDeserialize() {
        List<String> errors = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();

        for (Object serializable : serializables) {
            try {
                String serialized = mapper.writeValueAsString(serializable);
                Object deserialize = mapper.readValue(serialized, serializable.getClass());

                if(!deserialize.equals(serializable)){
                    errors.add("deserialized did not match original. Serialized: " + serialized + ", Original: " + serializable.toString());
                }

            } catch (JsonProcessingException e) {
                errors.add("Unable to serialize object of type: " + serializable.getClass().getName() + " with error: " + e.getMessage());
            } catch (IOException e) {
                errors.add("Unable to deserialize object of type: " + serializable.getClass().getName() + " with error: " + e.getMessage());
            }
        }

        if(!errors.isEmpty()){
            System.out.println("Failed " + errors.size() + "/" + serializables.size());
            for (String error : errors) {
                System.out.println(error);
            }
            fail();
        }

    }

}
