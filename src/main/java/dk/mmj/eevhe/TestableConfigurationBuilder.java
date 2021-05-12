package dk.mmj.eevhe;

import java.util.List;

/**
 * Interface for making a configBuilder more easily testable
 */
public interface TestableConfigurationBuilder {

    /**
     * For testability
     *
     * @return list of parameters
     */
    List<String> getParameters();
}
