package dk.mmj.eevhe;

import java.util.List;

public interface TestableConfigurationBuilder {

    /**
     * For testability
     *
     * @return list of parameters
     */
    List<String> getParameters();
}
