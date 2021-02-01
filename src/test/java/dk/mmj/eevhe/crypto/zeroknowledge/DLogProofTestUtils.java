package dk.mmj.eevhe.crypto.zeroknowledge;

import dk.mmj.eevhe.entities.CipherText;
import dk.mmj.eevhe.entities.PublicKey;

import java.math.BigInteger;

public class DLogProofTestUtils {
    public static DLogProofUtils.Proof generateFixedProof(CipherText cipherText, BigInteger secretValue, PublicKey publicKey, BigInteger y, int id) {
        return DLogProofUtils.generateProof(cipherText, secretValue, publicKey, y, id);
    }
}
