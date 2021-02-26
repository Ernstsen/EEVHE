package dk.mmj.eevhe.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestVoter.class,
        TestClientConfigBuilder.class
})
public class ClientTestSuite {
}
