package dk.mmj.eevhe.crypto.keygeneration;

import dk.mmj.eevhe.entities.PrimePair;

import java.math.BigInteger;

/**
 * Parameters used in El-Gamal key generation
 */
public interface KeyGenerationParameters {

    /**
     * Getter for the pair q,p used in El-Gamal key-generation
     *
     * @return a {@link PrimePair} with (p,q)
     */
    PrimePair getPrimePair();

    /**
     * returns generator <code>g</code> for the group
     *
     * @return {@link BigInteger} representing g
     */
    BigInteger getGenerator();


}
