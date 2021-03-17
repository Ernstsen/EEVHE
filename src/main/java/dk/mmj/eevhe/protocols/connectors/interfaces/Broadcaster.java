package dk.mmj.eevhe.protocols.connectors.interfaces;

import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.ComplaintResolveDTO;
import dk.mmj.eevhe.entities.FeldmanComplaintDTO;
import dk.mmj.eevhe.entities.PedersenComplaintDTO;

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
     * Broadcast a complaint in the Pedersen protocol
     *
     * @param complaint complaint to be broadcasted
     */
    void pedersenComplain(PedersenComplaintDTO complaint);

    /**
     * Delivers all broadcasted pedersen complaints
     *
     * @return list of all broadcasted complaints
     */
    List<PedersenComplaintDTO> getPedersenComplaints();

    /**
     * Broadcast a complaint in the Feldman protocol
     *
     * @param complaint complaint to be broadcasted
     */
    void feldmanComplain(FeldmanComplaintDTO complaint);

    /**
     * Delivers all broadcasted feldman complaints
     *
     * @return list of all broadcasted complaints
     */
    List<FeldmanComplaintDTO> getFeldmanComplaints();

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
