package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.ComplaintResolveDTO;
import dk.mmj.eevhe.entities.FeldmanComplaintDTO;
import dk.mmj.eevhe.entities.PedersenComplaintDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGBroadcaster;

import java.util.ArrayList;
import java.util.List;

public class TestDKGBroadcaster implements DKGBroadcaster {
    final List<CommitmentDTO> commitments = new ArrayList<>();
    final List<PedersenComplaintDTO> pedersenComplaints = new ArrayList<>();
    final List<FeldmanComplaintDTO> feldmanComplaints = new ArrayList<>();
    final List<ComplaintResolveDTO> resolves = new ArrayList<>();


    @Override
    public void commit(CommitmentDTO commitmentDTO) {
        commitments.add(commitmentDTO);
    }

    @Override
    public List<CommitmentDTO> getCommitments() {
        return commitments;
    }

    @Override
    public void pedersenComplain(PedersenComplaintDTO complaint) {
        pedersenComplaints.add(complaint);
    }

    @Override
    public void feldmanComplain(FeldmanComplaintDTO complaint) {
        feldmanComplaints.add(complaint);
    }

    @Override
    public List<PedersenComplaintDTO> getPedersenComplaints() {
        return pedersenComplaints;
    }

    @Override
    public List<FeldmanComplaintDTO> getFeldmanComplaints() {
        return feldmanComplaints;
    }

    @Override
    public void resolveComplaint(ComplaintResolveDTO complaintResolveDTO) {
        resolves.add(complaintResolveDTO);
    }

    @Override
    public List<ComplaintResolveDTO> getResolves() {
        return resolves;
    }
}
