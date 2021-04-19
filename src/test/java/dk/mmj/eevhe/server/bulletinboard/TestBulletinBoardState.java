package dk.mmj.eevhe.server.bulletinboard;

import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.entities.*;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.math.BigInteger.valueOf;

public class TestBulletinBoardState {
    private AsymmetricKeyParameter secretKey;

    public TestBulletinBoardState() throws IOException {
        secretKey = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
    }

    @Test
    public void testBallot() {
        BulletinBoardState bulletinBoardState = BulletinBoardState.getInstance();

        CipherText c = new CipherText(valueOf(165), valueOf(684983));
        Proof p1 = new Proof(valueOf(64986), valueOf(859483), valueOf(92873452), valueOf(293885671));
        CipherText c2 = new CipherText(valueOf(1652), valueOf(68498));
        Proof p2 = new Proof(valueOf(4986), valueOf(8359483), valueOf(873452), valueOf(885671));
        List<CandidateVoteDTO> candidates = Arrays.asList(new CandidateVoteDTO(c, "id", p1), new CandidateVoteDTO(c2, "id2", p2));
        Proof p3 = new Proof(valueOf(486), valueOf(359483), valueOf(73452), valueOf(85671));

        BallotDTO ballotDTO = new BallotDTO(candidates, "id", p3);
        PersistedBallot persistedBallot = new PersistedBallot(ballotDTO);

        Assert.assertEquals("Ballot should not exist in BBState at this point", 0, bulletinBoardState.getBallots().getBallots().size());
        Assert.assertFalse("Ballot should not exist in BBState at this point", bulletinBoardState.hasVoted(persistedBallot));

        bulletinBoardState.addBallot(persistedBallot);

        Assert.assertTrue("Ballot should exist in BBState at this point", bulletinBoardState.hasVoted(persistedBallot));

        Assert.assertEquals("BBState should contain BallotList with exactly one ballot",
                new BallotList(new ArrayList<>(Collections.singletonList(persistedBallot))),
                bulletinBoardState.getBallots());
        Assert.assertTrue("BBState should contain this specific ballot", bulletinBoardState.hasVoted(persistedBallot));
    }

    @Test
    public void testResult() {
        BulletinBoardState bulletinBoardState = BulletinBoardState.getInstance();

        CipherText c = new CipherText(valueOf(165), valueOf(684983));
        CipherText c2 = new CipherText(valueOf(1652), valueOf(68498));

        DLogProofUtils.Proof dp1 = new DLogProofUtils.Proof(valueOf(54), valueOf(9846));
        DLogProofUtils.Proof dp2 = new DLogProofUtils.Proof(valueOf(62968), valueOf(613658874));

        SignedEntity<PartialResultList> partialResultList = new SignedEntity<>(new PartialResultList(
                Arrays.asList(
                        new PartialResult(1, valueOf(234), dp1, c),
                        new PartialResult(2, valueOf(6854), dp2, c2)),
                58,
                18
        ), secretKey);

        Assert.assertEquals("Partial Result should not exist in BBState at this point", 0, bulletinBoardState.getResults().getResults().size());

        bulletinBoardState.addResult(partialResultList);

        Assert.assertEquals("BBState should contain this specific Partial Result", partialResultList, bulletinBoardState.getResults().getResults().get(0));
    }

    @Test
    public void testPublicInfo() {
        BulletinBoardState bulletinBoardState = BulletinBoardState.getInstance();

        PublicKey publicKey = new PublicKey(BigInteger.valueOf(4), BigInteger.valueOf(2), BigInteger.valueOf(11));
        List<Candidate> candidates = new ArrayList<Candidate>() {{
            add(new Candidate(1, "John", "Vote for John"));
            add(new Candidate(2, "Dorthe", "Dorthe as queen"));
        }};

        SignedEntity<PartialPublicInfo> partialPublicInfo = new SignedEntity<>(
                new PartialPublicInfo(1, publicKey, BigInteger.valueOf(3), candidates, 123456789, "Some certificate"), secretKey);

        Assert.assertEquals("Public Info should not exist in BBState at this point", 0, bulletinBoardState.getSignedPartialPublicInfos().size());

        bulletinBoardState.addSignedPartialPublicInfo(partialPublicInfo);

        Assert.assertEquals("BBState should contain this specific Public Info", partialPublicInfo, bulletinBoardState.getSignedPartialPublicInfos().get(0));
    }

    @Test
    public void testCommitment() {
        BulletinBoardState bulletinBoardState = BulletinBoardState.getInstance();

        SignedEntity<CommitmentDTO> commitment = new SignedEntity<>(
                new CommitmentDTO(new BigInteger[]{BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3)}, 1, "Some protocol"), secretKey);

        Assert.assertEquals("Commitment should not exist in BBState at this point", 0, bulletinBoardState.getSignedCommitments().size());

        bulletinBoardState.addSignedCommitment(commitment);

        Assert.assertEquals("BBState should contain this specific Commitment", commitment, bulletinBoardState.getSignedCommitments().get(0));
    }

    @Test
    public void testPedersenComplaint() {
        BulletinBoardState bulletinBoardState = BulletinBoardState.getInstance();

        SignedEntity<PedersenComplaintDTO> pedersenComplaint = new SignedEntity<>(new PedersenComplaintDTO(1,2), secretKey);

        Assert.assertEquals("Pedersen Complaint should not exist in BBState at this point", 0, bulletinBoardState.getSignedPedersenComplaints().size());

        bulletinBoardState.addSignedPedersenComplaint(pedersenComplaint);

        Assert.assertEquals("BBState should contain this specific Pedersen Complaint", pedersenComplaint, bulletinBoardState.getSignedPedersenComplaints().get(0));
    }

    @Test
    public void testFeldmanComplaint() {
        BulletinBoardState bulletinBoardState = BulletinBoardState.getInstance();

        SignedEntity<FeldmanComplaintDTO> feldmanComplaint = new SignedEntity<>(
                new FeldmanComplaintDTO(1, 2, BigInteger.valueOf(1), BigInteger.valueOf(2)), secretKey);

        Assert.assertEquals("Feldman Complaint should not exist in BBState at this point", 0, bulletinBoardState.getSignedFeldmanComplaints().size());

        bulletinBoardState.addSignedFeldmanComplaint(feldmanComplaint);

        Assert.assertEquals("BBState should contain this specific Feldman Complaint", feldmanComplaint, bulletinBoardState.getSignedFeldmanComplaints().get(0));
    }

    @Test
    public void testComplaintResolve() {
        BulletinBoardState bulletinBoardState = BulletinBoardState.getInstance();

        SignedEntity<ComplaintResolveDTO> complaintResolve = new SignedEntity<>(new ComplaintResolveDTO(1, 2,
                new PartialSecretMessageDTO(BigInteger.valueOf(1), BigInteger.valueOf(2), 1, 2)), secretKey);

        Assert.assertEquals("Complaint Resolve should not exist in BBState at this point", 0, bulletinBoardState.getSignedComplaintResolves().size());

        bulletinBoardState.addSignedComplaintResolve(complaintResolve);

        Assert.assertEquals("BBState should contain this specific Complaint Resolve", complaintResolve, bulletinBoardState.getSignedComplaintResolves().get(0));
    }

    @Test
    public void testCertificates() {
        BulletinBoardState bulletinBoardState = BulletinBoardState.getInstance();

        SignedEntity<CertificateDTO> certificate = new SignedEntity<>(new CertificateDTO("Some random certificate", 1), secretKey);

        Assert.assertEquals("Certificate should not exist in BBState at this point", 0, bulletinBoardState.getSignedCertificates().size());

        bulletinBoardState.addSignedCertificate(certificate);

        Assert.assertEquals("BBState should contain this specific Certificate", certificate, bulletinBoardState.getSignedCertificates().get(0));
    }
}