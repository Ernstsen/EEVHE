package dk.mmj.eevhe.server.bulletinboard;

import dk.mmj.eevhe.entities.*;

import java.util.ArrayList;
import java.util.List;

public class BulletinBoardState {
    private final static BulletinBoardState instance = new BulletinBoardState();

    private final List<PersistedBallot> ballots = new ArrayList<>();
    private final List<SignedEntity<PartialResultList>> results = new ArrayList<>();
    private final List<SignedEntity<PartialPublicInfo>> signedPartialPublicInfos = new ArrayList<>();
    private final List<SignedEntity<CommitmentDTO>> signedCommitments = new ArrayList<>();
    private final List<SignedEntity<PedersenComplaintDTO>> signedPedersenComplaints = new ArrayList<>();
    private final List<SignedEntity<FeldmanComplaintDTO>> signedFeldmanComplaints = new ArrayList<>();
    private final List<SignedEntity<ComplaintResolveDTO>> signedComplaintResolves = new ArrayList<>();
    private final List<SignedEntity<CertificateDTO>> signedCertificates = new ArrayList<>();
    private final List<String> hasVoted = new ArrayList<>();

    /**
     * Getter for singleton instance
     *
     * @return the BBState
     */
    @Deprecated//TODO! REMOVE
    public static BulletinBoardState getInstance() {
        return instance;
    }

    public List<PersistedBallot> getBallots() {
        return ballots;
    }

    public void addBallot(PersistedBallot ballot) {
        ballots.add(ballot);
        hasVoted.add(ballot.getId());
    }

    public boolean hasVoted(PersistedBallot ballot) {
        return hasVoted.contains(ballot.getId());
    }

    public List<SignedEntity<PartialResultList>> getResults() {
        return results;
    }

    public void addResult(SignedEntity<PartialResultList> result) {
        results.add(result);
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

    /**
     * For tests. Clears state
     */
    void clear() {
        ballots.clear();
        results.clear();
        signedPartialPublicInfos.clear();
        signedCommitments.clear();
        signedPedersenComplaints.clear();
        signedFeldmanComplaints.clear();
        signedComplaintResolves.clear();
        signedCertificates.clear();
        hasVoted.clear();
    }
}
