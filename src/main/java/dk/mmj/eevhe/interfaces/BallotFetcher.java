package dk.mmj.eevhe.interfaces;

import dk.mmj.eevhe.entities.PersistedBallot;

import java.util.List;

/**
 * Abstraction to enable injection of multiple ways to fetch ballots - or simulate doing so
 */
public interface BallotFetcher {

    /**
     * @return list of cast ballots in the election
     */
    List<PersistedBallot> getBallots();
}
