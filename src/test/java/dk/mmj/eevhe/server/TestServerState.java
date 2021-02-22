package dk.mmj.eevhe.server;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertSame;

public class TestServerState {
    private ServerState state;

    @Before
    public void setUp() throws Exception {
        state = ServerState.getInstance();
        state.reset();
    }

    @Test
    public void saveAndReturn() {
        String val = "VAL";
        state.put("key", val);

        assertSame("Fetched value did not match original one", val, state.get("key", String.class));


    }

    @Test
    public void noExceptionOnNoEntry() {
        try {
            state.get("keyWithNoEntry", String.class);
        } catch (Throwable t) {
            fail(t.toString());
        }
    }

    @Test
    public void noExceptionOnWrongClass() {
        state.put("keyWithIntEntry", 1);
        try {
            state.get("keyWithIntEntry", String.class);
        } catch (Throwable t) {
            fail(t.toString());
        }
    }
}
