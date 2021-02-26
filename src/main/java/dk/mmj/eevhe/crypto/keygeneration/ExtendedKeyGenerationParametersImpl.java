package dk.mmj.eevhe.crypto.keygeneration;

import dk.mmj.eevhe.crypto.PedersenVSSUtils;

import java.math.BigInteger;

public class ExtendedKeyGenerationParametersImpl extends KeyGenerationParametersImpl implements ExtendedKeyGenerationParameters {
    private final BigInteger e;

    public ExtendedKeyGenerationParametersImpl(int primeBitLength, int primeCertainty) {
        super(primeBitLength, primeCertainty);
        e = PedersenVSSUtils.generateElementInSubgroup(getGenerator(), getPrimePair().getP());
    }

    @Override
    public BigInteger getGroupElement() {
        return e;
    }
}
