package dk.mmj.eevhe.integrationTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        IntegrationUnitTest.class,
        TestIntegrationTestConfigBuilder.class
})
public class IntegrationTestSuite {
}
