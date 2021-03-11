package dk.mmj.eevhe.entities;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TestSerialization.class,
        TestSignedEntity.class
})
public class EntityTestSuite {
}
