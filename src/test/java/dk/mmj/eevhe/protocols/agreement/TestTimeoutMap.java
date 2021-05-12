package dk.mmj.eevhe.protocols.agreement;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TestTimeoutMap {

    @Test
    public void testClean() throws InterruptedException {
        String key1 = "key1";
        String val1 = "val1";
        String key2 = "key2";
        String val2 = "val2";

        HashMap<String, String> orig = new HashMap<>();
        orig.put(key1, val1);
        orig.put(key2, val2);

        TimeoutMap<String, String> timeout = new TimeoutMap<>(50, TimeUnit.MILLISECONDS);
        timeout.putAll(orig);
        Thread.sleep(25);
        timeout.get(key1);
        Thread.sleep(25);
        timeout.get(key1);
        Thread.sleep(25);
        timeout.get(key1);
        Thread.sleep(25);
        timeout.get(key1);
        Thread.sleep(40);

        String get1 = timeout.get(key1);
        String get2 = timeout.get(key2);
        assertEquals("Val 1 should still be in map", val1, get1);
        assertNull("Val 2 should have been removed", get2);
    }

    @Test
    public void shouldActLikeHashMap() {
        String key1 = "key1";
        String val1 = "val1";
        String key2 = "key2";
        String val2 = "val2";

        HashMap<String, String> orig = new HashMap<>();
        orig.put(key1, val1);
        orig.put(key2, val2);

        TimeoutMap<String, String> timeout = new TimeoutMap<>(10, TimeUnit.MINUTES);
        assertTrue("Empty-status differed from expected", timeout.isEmpty());
        timeout.putAll(orig);

        String[] keys = {key1, key2, "key3"};
        String[] values = new String[]{val1, val2, "val3"};

        assertMapEquals(keys, values, orig, timeout);

        orig.clear();
        timeout.clear();

        assertMapEquals(keys, values, orig, timeout);
    }

    private void assertMapEquals(String[] keys, String[] values, HashMap<String, String> orig, TimeoutMap<String, String> timeout) {
        assertEquals("Size differed from hashmap impl", orig.size(), timeout.size());
        assertEquals("Empty-status differed from hashmap impl", orig.isEmpty(), timeout.isEmpty());

        for (String key : keys) {
            assertEquals("ContainsKey differed from hashmap impl", orig.containsKey(key), timeout.containsKey(key));
        }

        for (String val : values) {
            assertEquals("ContainsValue differed from hashmap impl", orig.containsValue(val), timeout.containsValue(val));
        }

        assertArrayEquals("Keyset differed from hashmap impl", orig.keySet().stream().sorted().toArray(), timeout.keySet().stream().sorted().toArray());
        assertArrayEquals("ValueSet differed from hashmap impl", orig.values().stream().sorted().toArray(), timeout.values().stream().sorted().toArray());
        assertArrayEquals(
                "EntrySet differed from hashmap impl",
                orig.entrySet().stream().sorted(Map.Entry.comparingByKey()).toArray(),
                timeout.entrySet().stream().sorted(Map.Entry.comparingByKey()).toArray());
    }

}
