package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class TestBBPackage {
    @Test
    public void shouldRepresentBallotAsString() {
        String id = "Some random ID";

        CandidateVoteDTO candidateVote1 = new CandidateVoteDTO(
                new CipherText(BigInteger.valueOf(1), BigInteger.valueOf(2)),
                id,
                new Proof(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3), BigInteger.valueOf(4)));

        CandidateVoteDTO candidateVote2 = new CandidateVoteDTO(
                new CipherText(BigInteger.valueOf(10), BigInteger.valueOf(20)),
                id,
                new Proof(BigInteger.valueOf(10), BigInteger.valueOf(20), BigInteger.valueOf(30), BigInteger.valueOf(40)));

        List<CandidateVoteDTO> candidateVotes = new ArrayList<CandidateVoteDTO>(){{
            add(candidateVote1);
            add(candidateVote2);
        }};

        Proof sumIsOneProof = new Proof(BigInteger.valueOf(0), BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3));

        BallotDTO ballot = new BallotDTO(candidateVotes, id, sumIsOneProof);

        BBPackage<BallotDTO> bbPackage = new BBPackage<>(ballot);

        String actual = "";

        try {
            actual = bbPackage.getContentAsString();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        String expected = "{\"candidateVotes\":[{\"cipherText\":{\"c\":1,\"d\":2},\"id\":\"Some random ID\",\"proof\":{\"e0\":1,\"e1\":2,\"z0\":3,\"z1\":4}},{\"cipherText\":{\"c\":10,\"d\":20},\"id\":\"Some random ID\",\"proof\":{\"e0\":10,\"e1\":20,\"z0\":30,\"z1\":40}}],\"id\":\"Some random ID\",\"sumIsOneProof\":{\"e0\":0,\"e1\":1,\"z0\":2,\"z1\":3}}";
        Assert.assertEquals("BBPackage content not be converted to string correctly", expected, actual);
    }
}
