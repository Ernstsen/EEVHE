package dk.mmj.eevhe;

import dk.mmj.eevhe.client.ClientTestSuite;
import dk.mmj.eevhe.crypto.CryptoTestSuite;
import dk.mmj.eevhe.entities.EntityTestSuite;
import dk.mmj.eevhe.initialization.InitializationTestSuite;
import dk.mmj.eevhe.integrationTest.IntegrationTestSuite;
import dk.mmj.eevhe.protocols.ProtocolTestSuite;
import dk.mmj.eevhe.server.ServerTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        ClientTestSuite.class,
        CryptoTestSuite.class,
        EntityTestSuite.class,
        InitializationTestSuite.class,
        IntegrationTestSuite.class,
        ProtocolTestSuite.class,
        ServerTestSuite.class,
        TestMain.class
})
public class TestSuite {
}
