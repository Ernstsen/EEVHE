package dk.mmj.eevhe.entities;

import java.util.List;

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
}
