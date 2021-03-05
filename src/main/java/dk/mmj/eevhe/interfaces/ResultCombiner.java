package dk.mmj.eevhe.interfaces;

import dk.mmj.eevhe.client.results.ElectionResult;
import dk.mmj.eevhe.entities.PartialResultList;

import java.util.List;

/**
 * Interface representing the responsibility of combining a number of results,
 * namely {@link dk.mmj.eevhe.entities.PartialResult} into an election result
 */
public interface ResultCombiner {

    /**
     * Combines the partial results into an election result
     *
     * @param partialResultLists the results to be combined into an election result
     * @return list where an idx contains the number of votes for the candidate with that same idx
     */
    ElectionResult computeResult(List<PartialResultList> partialResultLists);

}
