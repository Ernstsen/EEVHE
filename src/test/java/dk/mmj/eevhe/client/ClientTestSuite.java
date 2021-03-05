package dk.mmj.eevhe.client;

import dk.mmj.eevhe.client.results.TestResultCombinerImpl;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestResultCombinerImpl.class,
        TestVoter.class,
        TestClientConfigBuilder.class
})
public class ClientTestSuite {
}
