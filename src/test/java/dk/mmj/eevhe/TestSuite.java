package dk.mmj.eevhe;

import dk.mmj.eevhe.crypto.CryptoTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        CryptoTestSuite.class
})
public class TestSuite {
}
