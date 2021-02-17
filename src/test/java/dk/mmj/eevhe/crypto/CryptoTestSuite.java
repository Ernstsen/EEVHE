package dk.mmj.eevhe.crypto;

import dk.mmj.eevhe.crypto.keygeneration.TestKeyGenerationsParametersImpl;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestKeyGenerationsParametersImpl.class,
        TestDLogProofUtils.class,
        TestElGamal.class,
        TestVoteProofUtils.class,
        TestSecurityUtils.class,
        TestSecretSharingUtils.class
})
public class CryptoTestSuite {
}
