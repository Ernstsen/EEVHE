package dk.mmj.eevhe.entities;

import dk.mmj.eevhe.server.ServerState;

import java.util.ArrayList;
import java.util.List;

public class BBState {
    private final static BBState instance = new BBState();

    private BallotList ballots = new BallotList();
    private ResultList results = new ResultList();
    private List<SignedEntity<PartialPublicInfo>> signedPartialPublicInfos = new ArrayList<>();
    private List<SignedEntity<CommitmentDTO>> signedCommitments = new ArrayList<>();
    private List<SignedEntity<PedersenComplaintDTO>> signedPedersenComplaints = new ArrayList<>();
    private List<SignedEntity<FeldmanComplaintDTO>> signedFeldmanComplaints = new ArrayList<>();
    private List<SignedEntity<ComplaintResolveDTO>> signedComplaintResolves = new ArrayList<>();
    private List<SignedEntity<CertificateDTO>> signedCertificates = new ArrayList<>();

    private List<String> hasVoted = new ArrayList<>();

    /**
     * Getter for singleton instance
     *
     * @return the BBState
     */
    public static BBState getInstance() {
        return instance;
    }

    public BallotList getBallots() {
        return ballots;
    }

    public void addBallot(PersistedBallot ballot) {
        ballots.getBallots().add(ballot);
        hasVoted.add(ballot.getId());
    }

    public boolean hasVoted(PersistedBallot ballot) {
        return hasVoted.contains(ballot.getId());
    }

    public ResultList getResults() {
        return results;
    }

    public void addResult(SignedEntity<PartialResultList> result) {
        results.getResults().add(result);
    }

    public List<SignedEntity<PartialPublicInfo>> getSignedPartialPublicInfos() {
        return signedPartialPublicInfos;
    }

    public void addSignedPartialPublicInfo(SignedEntity<PartialPublicInfo> signedPartialPublicInfo) {
        signedPartialPublicInfos.add(signedPartialPublicInfo);
    }

    public List<SignedEntity<CommitmentDTO>> getSignedCommitments() {
        return signedCommitments;
    }

    public void addSignedCommitment(SignedEntity<CommitmentDTO> signedCommitment) {
        signedCommitments.add(signedCommitment);
    }

    public List<SignedEntity<PedersenComplaintDTO>> getSignedPedersenComplaints() {
        return signedPedersenComplaints;
    }

    public void addSignedPedersenComplaint(SignedEntity<PedersenComplaintDTO> signedPedersenComplaint) {
        signedPedersenComplaints.add(signedPedersenComplaint);
    }

    public List<SignedEntity<FeldmanComplaintDTO>> getSignedFeldmanComplaints() {
        return signedFeldmanComplaints;
    }

    public void addSignedFeldmanComplaint(SignedEntity<FeldmanComplaintDTO> signedFeldmanComplaint) {
        signedFeldmanComplaints.add(signedFeldmanComplaint);
    }

    public List<SignedEntity<ComplaintResolveDTO>> getSignedComplaintResolves() {
        return signedComplaintResolves;
    }

    public void addSignedComplaintResolve(SignedEntity<ComplaintResolveDTO> signedComplaintResolve) {
        signedComplaintResolves.add(signedComplaintResolve);
    }

    public List<SignedEntity<CertificateDTO>> getSignedCertificates() {
        return signedCertificates;
    }

    public void addSignedCertificate(SignedEntity<CertificateDTO> signedCertificate) {
        signedCertificates.add(signedCertificate);
    }
}
