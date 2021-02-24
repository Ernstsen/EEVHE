package dk.mmj.eevhe;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public abstract class AbstractConfigTest {

    /**
     * @return list of params passable to config
     */
    protected abstract List<String> getParams();

    /**
     * @return help string from config
     */
    protected abstract String getHelp();

    @Test
    public void allParametersMentionedInHelp() {
        String help = getHelp();

        for (String param : getParams()) {
            String paramString = param.replaceAll("-", "");
            assertTrue("Help did not contain parameter: " + paramString, help.contains(paramString));
        }
    }

}
