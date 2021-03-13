package dk.mmj.eevhe.crypto;

import dk.mmj.eevhe.crypto.keygeneration.TestKeyGenerationsParametersImpl;
import dk.mmj.eevhe.crypto.signature.SignatureTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestKeyGenerationsParametersImpl.class,
        SignatureTestSuite.class,
        TestDLogProofUtils.class,
        TestElGamal.class,
        TestVoteProofUtils.class,
        TestSecurityUtils.class,
        TestFeldmanVSSUtils.class
})
public class CryptoTestSuite {
}
