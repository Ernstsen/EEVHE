package dk.mmj.eevhe;

import dk.mmj.eevhe.client.TestClientConfigBuilder;
import dk.mmj.eevhe.crypto.CryptoTestSuite;
import dk.mmj.eevhe.entities.TestSerialization;
import dk.mmj.eevhe.protocols.ProtocolTestSuite;
import dk.mmj.eevhe.server.ServerTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestClientConfigBuilder.class,
        CryptoTestSuite.class,
        TestSerialization.class,
        ProtocolTestSuite.class,
        ServerTestSuite.class,
})
public class TestSuite {
}
