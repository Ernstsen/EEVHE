package dk.mmj.eevhe.crypto.keygeneration;

import dk.mmj.eevhe.crypto.PedersenVSSUtils;

import java.math.BigInteger;

public class ExtendedPersistedKeyParameters extends PersistedKeyParameters implements ExtendedKeyGenerationParameters{
    private final BigInteger e;

    /**
     * Generates persisted key parameters from a prime p and a generator g
     *
     * @param pString hexadecimal encoding of prime p
     * @param gString generator g as string
     */
    public ExtendedPersistedKeyParameters(String pString, String gString) {
        super(pString, gString);
        e = PedersenVSSUtils.generateElementInSubgroup(getGenerator(), getPrimePair().getP());
    }

    @Override
    public BigInteger getGroupElement() {
        return e;
    }
}
