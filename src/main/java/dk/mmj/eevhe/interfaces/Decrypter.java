package dk.mmj.eevhe.interfaces;

import dk.mmj.eevhe.entities.PartialKeyPair;
import dk.mmj.eevhe.entities.PartialResultList;

/**
 * Interface containing the responsibility for
 */
public interface Decrypter {

    /**
     * Does partial decryption, for a DA instance
     *
     * @param endTime endTime for the election
     * @param keyPair keys used - output from DKG protocol
     * @return partially decrypted result for this instance
     */
    PartialResultList generatePartialResult(long endTime, PartialKeyPair keyPair);


}
