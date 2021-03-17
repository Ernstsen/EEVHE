package dk.mmj.eevhe.crypto.signature;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestCertificateProviderImpl.class,
        TestCertificateSuite.class
})
public class SignatureTestSuite {
}
