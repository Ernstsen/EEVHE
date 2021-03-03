package dk.mmj.eevhe.protocols.connectors;

import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.ComplaintDTO;
import dk.mmj.eevhe.entities.ComplaintResolveDTO;
import dk.mmj.eevhe.protocols.connectors.interfaces.Broadcaster;

import java.util.ArrayList;
import java.util.List;

public class TestBroadcaster implements Broadcaster {
    final List<CommitmentDTO> commitments = new ArrayList<>();
    final List<ComplaintDTO> complaints = new ArrayList<>();
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
    public void complain(ComplaintDTO complaint) {
        complaints.add(complaint);
    }

    @Override
    public List<ComplaintDTO> getComplaints() {
        return complaints;
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
