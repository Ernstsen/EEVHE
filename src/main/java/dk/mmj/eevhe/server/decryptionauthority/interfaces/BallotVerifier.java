package dk.mmj.eevhe.server.decryptionauthority.interfaces;

import dk.mmj.eevhe.entities.BallotDTO;

/**
 * interface for verifying cast ballots
 */
public interface BallotVerifier {

    /**
     * Determines whether a ballot is valid
     *
     * @param ballotDTO the ballot
     * @return whether the ballot is valid, and should be included
     */
    boolean verifyBallot(BallotDTO ballotDTO);
}
