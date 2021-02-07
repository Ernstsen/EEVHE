package dk.mmj.eevhe;

import dk.mmj.eevhe.crypto.CryptoTestSuite;
import dk.mmj.eevhe.entities.TestSerialization;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        CryptoTestSuite.class,
        TestSerialization.class
})
public class TestSuite {
}
