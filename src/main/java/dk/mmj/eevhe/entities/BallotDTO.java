package dk.mmj.eevhe.entities;

import java.util.List;
import java.util.Objects;

public class BallotDTO {
    private List<CandidateVoteDTO> candidateVotes;
    private String id;
    private Proof sumIsOneProof;

    public BallotDTO() {
    }

    public BallotDTO(List<CandidateVoteDTO> candidateVotes, String id, Proof sumIsOneProof) {
        this.candidateVotes = candidateVotes;
        this.id = id;
        this.sumIsOneProof = sumIsOneProof;
    }

    public List<CandidateVoteDTO> getCandidateVotes() {
        return candidateVotes;
    }

    public BallotDTO setCandidateVotes(List<CandidateVoteDTO> candidateVotes) {
        this.candidateVotes = candidateVotes;
        return this;
    }

    public String getId() {
        return id;
    }

    public BallotDTO setId(String id) {
        this.id = id;
        return this;
    }

    public Proof getSumIsOneProof() {
        return sumIsOneProof;
    }

    public BallotDTO setSumIsOneProof(Proof sumIsOneProof) {
        this.sumIsOneProof = sumIsOneProof;
        return this;
    }

    @Override
    public String toString() {
        return "BallotDTO{" +
                "candidateVotes=" + candidateVotes +
                ", id='" + id + '\'' +
                ", sumIsOneProof=" + sumIsOneProof +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BallotDTO ballotDTO = (BallotDTO) o;
        return Objects.equals(candidateVotes, ballotDTO.candidateVotes) &&
                Objects.equals(id, ballotDTO.id) &&
                Objects.equals(sumIsOneProof, ballotDTO.sumIsOneProof);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidateVotes, id, sumIsOneProof);
    }
}
