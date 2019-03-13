package dk.mmj.evhe.crypto;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public class TestElGamal {
    private KeyPair generateKeysFromP11G2() {
        PrimePair primes = new PrimePair(new BigInteger("11"), new BigInteger("5"));
        BigInteger g = new BigInteger("2");
        return ElGamal.generateKeys(new SimpleKeyGenParams(g, primes));
    }

    @Test
    public void shouldBeAbleToEncryptAndDecrypt0() {
        KeyPair keyPair = generateKeysFromP11G2();
        CipherText cipherText = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), new BigInteger("0"));
        int b = ElGamal.homomorphicDecryption(keyPair, cipherText);

        Assert.assertEquals(0, b);
    }

    @Test
    public void shouldBeAbleToEncryptAndDecrypt1() {
        KeyPair keyPair = generateKeysFromP11G2();
        CipherText cipherText = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ONE);
        int b = ElGamal.homomorphicDecryption(keyPair, cipherText);

        Assert.assertEquals(1, b);
    }

    @Test
    public void testHomomorphicAdditionWhenPlaintextsBothAre1() {
        KeyPair keyPair = generateKeysFromP11G2();
        CipherText cipherText1 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ONE);
        CipherText cipherText2 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ONE);
        CipherText cipherTextProduct = ElGamal.homomorphicAddition(cipherText1, cipherText2);

        int b = ElGamal.homomorphicDecryption(keyPair, cipherTextProduct);

        Assert.assertEquals(2, b);
    }

    @Test
    public void testHomomorphicAdditionWhenPlaintextsBothAre0() {
        KeyPair keyPair = generateKeysFromP11G2();
        CipherText cipherText1 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ZERO);
        CipherText cipherText2 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ZERO);
        CipherText cipherTextProduct = ElGamal.homomorphicAddition(cipherText1, cipherText2);

        int b = ElGamal.homomorphicDecryption(keyPair, cipherTextProduct);

        Assert.assertEquals(0, b);
    }

    @Test
    public void testHomomorphicAdditionWhenPlaintextsAre0And1() {
        KeyPair keyPair = generateKeysFromP11G2();
        CipherText cipherText1 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ZERO);
        CipherText cipherText2 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ONE);
        CipherText cipherTextProduct = ElGamal.homomorphicAddition(cipherText1, cipherText2);

        int b = ElGamal.homomorphicDecryption(keyPair, cipherTextProduct);

        Assert.assertEquals(1, b);
    }

    @Test
    public void testHomomorphicAdditionWhenPlaintextsAre1And0() {
        KeyPair keyPair = generateKeysFromP11G2();
        CipherText cipherText1 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ONE);
        CipherText cipherText2 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ZERO);
        CipherText cipherTextProduct = ElGamal.homomorphicAddition(cipherText1, cipherText2);

        int b = ElGamal.homomorphicDecryption(keyPair, cipherTextProduct);

        Assert.assertEquals(1, b);
    }

    @Test
    public void testHomomorphicAdditionWith2048bitP() {
        String pString = "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1 29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245 E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F 83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D 670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9 DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510 15728E5A 8AACAA68 FFFFFFFF FFFFFFFF";
        KeyPair keyPair = ElGamal.generateKeys(new PersistedKeyParameters(pString, "2"));
        CipherText cipherText1 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ONE);
        CipherText cipherText2 = ElGamal.homomorphicEncryption(keyPair.getPublicKey(), BigInteger.ZERO);
        CipherText cipherTextProduct = ElGamal.homomorphicAddition(cipherText1, cipherText2);

        int b = ElGamal.homomorphicDecryption(keyPair, cipherTextProduct);

        Assert.assertEquals(1, b);
    }

    private static class SimpleKeyGenParams implements KeyGenerationParameters {
        private BigInteger g;
        private PrimePair pair;

        private SimpleKeyGenParams(BigInteger g, PrimePair pair) {
            this.g = g;
            this.pair = pair;
        }

        @Override
        public PrimePair getPrimePair() {
            return pair;
        }

        @Override
        public BigInteger getGenerator() {
            return g;
        }
    }
}
