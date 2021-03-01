package dk.mmj.eevhe.protocols.connectors.interfaces;

import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.ComplaintDTO;
import dk.mmj.eevhe.entities.ComplaintResolveDTO;

import java.util.List;

/**
 * Enables reading/writing to a 'broadcast' channel in a DKG protocol
 */
public interface Broadcaster {


    /**
     * Broadcasts commitments
     *
     * @param commitmentDTO the commitments to be broadcasted
     */
    void commit(CommitmentDTO commitmentDTO);

    /**
     * Delivers all broadcasted commitments
     *
     * @return list of all commitments
     */
    List<CommitmentDTO> getCommitments();

    /**
     * Broadcast a complaint
     *
     * @param complaint complaint to be broadcasted
     */
    void complain(ComplaintDTO complaint);

    /**
     * Delivers all broadcasted complaints
     *
     * @return list of all broadcasted complaints
     */
    List<ComplaintDTO> getComplaints();

    /**
     * Broadcasts a resolve to a complaint
     *
     * @param complaintResolveDTO the resolve to broadcast
     */
    void resolveComplaint(ComplaintResolveDTO complaintResolveDTO);

    /**
     * Delivers all broadcasted complaint resolves
     *
     * @return list of all broadcasted resolves
     */
    List<ComplaintResolveDTO> getResolves();
}
