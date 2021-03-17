package dk.mmj.eevhe.crypto.keygeneration;

import dk.mmj.eevhe.entities.PrimePair;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;

import static java.math.BigInteger.ONE;

public class PersistedKeyParameters implements KeyGenerationParameters {
    private final PrimePair primePair;
    private final BigInteger g;

    /**
     * Generates persisted key parameters from a prime p and a generator g
     *
     * @param pString hexadecimal encoding of prime p
     * @param gString generator g as string
     */
    public PersistedKeyParameters(String pString, String gString) {
        this.g = new BigInteger(gString);

        BigInteger p = new BigInteger(1, Hex.decode(pString.replaceAll(" ", "")));
        BigInteger q = p.subtract(ONE).divide(BigInteger.valueOf(2));

        this.primePair = new PrimePair(p, q);
    }

    @Override
    public PrimePair getPrimePair() {
        return primePair;
    }

    @Override
    public BigInteger getGenerator() {
        return g;
    }
}
