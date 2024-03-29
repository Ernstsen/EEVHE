package dk.mmj.eevhe.protocols.agreement;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Map implementation which removes items after them being inactive for some time.
 * <b>
 * NOTE: The following methods does NOT act as activity, thus any value access through these will still be removed
 * </b>
 * <ul>
 *     <li>keySet</li>
 *     <li>values</li>
 *     <li>entrySet</li>
 * </ul>
 *
 * @param <K> key type
 * @param <V> value type
 */
@SuppressWarnings("unchecked")
public class TimeoutMap<K, V> implements Map<K, V> {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<K, V> inner;
    private final Map<K, Long> tsMap;
    private final long cleanupMs;


    /**
     * @param timeoutTime the amount of time to retain inactive entries in map
     * @param unit        the unit of time
     */
    public TimeoutMap(int timeoutTime, TimeUnit unit) {
        inner = new HashMap<>();
        tsMap = new HashMap<>();
        cleanupMs = unit.toMillis(timeoutTime);
        scheduler.scheduleAtFixedRate(this::clean, timeoutTime, timeoutTime, unit);
    }

    /**
     * Removes all map entries older than timeout threshold.
     */
    private void clean() {
        final long threshold = System.currentTimeMillis() - cleanupMs;

        tsMap.entrySet().stream()
                .filter(e -> e.getValue() < threshold)
                .map(Entry::getKey)
                .forEach(inner::remove);
        tsMap.entrySet().removeIf(e -> e.getValue() < threshold);
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        tsMap.put((K) key, System.currentTimeMillis());
        return inner.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return inner.containsValue(value);
    }

    @Override
    public V get(Object key) {
        tsMap.put((K) key, System.currentTimeMillis());
        return inner.get(key);
    }

    @Override
    public V put(K key, V value) {
        tsMap.put(key, System.currentTimeMillis());
        return inner.put(key, value);
    }

    @Override
    public V remove(Object key) {
        tsMap.put((K) key, System.currentTimeMillis());
        return inner.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.keySet().forEach(k -> tsMap.put(k, System.currentTimeMillis()));

        inner.putAll(m);
    }

    @Override
    public void clear() {
        tsMap.clear();
        inner.clear();
    }

    @Override
    public Set<K> keySet() {
        return new HashSet<>(inner.keySet());
    }

    @Override
    public Collection<V> values() {
        return new HashSet<>(inner.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new HashSet<>(inner.entrySet());
    }
}
