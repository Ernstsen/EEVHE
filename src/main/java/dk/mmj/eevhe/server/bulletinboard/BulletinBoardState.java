package dk.mmj.eevhe.server.bulletinboard;

import dk.mmj.eevhe.entities.*;

import java.util.*;

public class BulletinBoardState {
    private final Set<PersistedBallot> ballots = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<SignedEntity<PartialResultList>> results = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<SignedEntity<PartialPublicInfo>> signedPartialPublicInfos = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<SignedEntity<CommitmentDTO>> signedCommitments = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<SignedEntity<PedersenComplaintDTO>> signedPedersenComplaints = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<SignedEntity<FeldmanComplaintDTO>> signedFeldmanComplaints = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<SignedEntity<ComplaintResolveDTO>> signedComplaintResolves = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<SignedEntity<CertificateDTO>> signedCertificates = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<String> hasVoted = Collections.synchronizedSet(new LinkedHashSet<>());

    public List<PersistedBallot> getBallots() {
        return new ArrayList<>(ballots);
    }

    public void addBallot(PersistedBallot ballot) {
        ballots.add(ballot);
        hasVoted.add(ballot.getId());
    }

    public boolean hasVoted(PersistedBallot ballot) {
        return hasVoted.contains(ballot.getId());
    }

    public List<SignedEntity<PartialResultList>> getResults() {
        return new ArrayList<>(results);
    }

    public void addResult(SignedEntity<PartialResultList> result) {
        results.add(result);
    }

    public List<SignedEntity<PartialPublicInfo>> getSignedPartialPublicInfos() {
        return new ArrayList<>(signedPartialPublicInfos);
    }

    public void addSignedPartialPublicInfo(SignedEntity<PartialPublicInfo> signedPartialPublicInfo) {
        signedPartialPublicInfos.add(signedPartialPublicInfo);
    }

    public List<SignedEntity<CommitmentDTO>> getSignedCommitments() {
        return new ArrayList<>(signedCommitments);
    }

    public void addSignedCommitment(SignedEntity<CommitmentDTO> signedCommitment) {
        signedCommitments.add(signedCommitment);
    }

    public List<SignedEntity<PedersenComplaintDTO>> getSignedPedersenComplaints() {
        return new ArrayList<>(signedPedersenComplaints);
    }

    public void addSignedPedersenComplaint(SignedEntity<PedersenComplaintDTO> signedPedersenComplaint) {
        signedPedersenComplaints.add(signedPedersenComplaint);
    }

    public List<SignedEntity<FeldmanComplaintDTO>> getSignedFeldmanComplaints() {
        return new ArrayList<>(signedFeldmanComplaints);
    }

    public void addSignedFeldmanComplaint(SignedEntity<FeldmanComplaintDTO> signedFeldmanComplaint) {
        signedFeldmanComplaints.add(signedFeldmanComplaint);
    }

    public List<SignedEntity<ComplaintResolveDTO>> getSignedComplaintResolves() {
        return new ArrayList<>(signedComplaintResolves);
    }

    public void addSignedComplaintResolve(SignedEntity<ComplaintResolveDTO> signedComplaintResolve) {
        signedComplaintResolves.add(signedComplaintResolve);
    }

    public List<SignedEntity<CertificateDTO>> getSignedCertificates() {
        return new ArrayList<>(signedCertificates);
    }

    public void addSignedCertificate(SignedEntity<CertificateDTO> signedCertificate) {
        signedCertificates.add(signedCertificate);
    }
}
