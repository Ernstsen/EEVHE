package dk.mmj.eevhe;

import dk.mmj.eevhe.crypto.CryptoTestSuite;
import dk.mmj.eevhe.entities.TestSerialization;
import dk.mmj.eevhe.protocols.TestPedersenDKG;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        CryptoTestSuite.class,
        TestSerialization.class,
        TestPedersenDKG.class
})
public class TestSuite {
}
