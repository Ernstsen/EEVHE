package dk.mmj.eevhe.client;

import dk.mmj.eevhe.client.results.TestResultCombinerImpl;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestResultCombinerImpl.class,
        TestClientConfigBuilder.class,
        TestFetchingUtilities.class,
        TestSSLHelper.class,
        TestVoter.class,
})
public class ClientTestSuite {
}

