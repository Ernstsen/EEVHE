package dk.mmj.eevhe.initialization;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestSystemConfigurer.class,
        TestSystemConfigurerConfigBuilder.class
})
public class InitializationTestSuite {
}
