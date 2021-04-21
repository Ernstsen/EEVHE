package dk.mmj.eevhe.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Singleton state for a server. Acts like a map
 */
public class ServerState {
    // State
    private final static ServerState instance = new ServerState();
    private final static Logger logger = LogManager.getLogger(ServerState.class);
    private final Map<String, Object> state = new ConcurrentHashMap<>();

    /**
     * Getter for singleton instance
     *
     * @return the ServerState
     */
    public static ServerState getInstance() {
        return instance;
    }

    /**
     * Puts an object into the state
     *
     * @param key    unique key used as reference for the stored object
     * @param object object to be stored
     */
    public void put(String key, Object object) {
        state.put(key, object);
    }

    public synchronized <T> void putInList(String key, T object) {
        //noinspection unchecked
        List<T> list = (List<T>) state.get(key);

        if (list == null) {
            list = new ArrayList<>();
            state.put(key, list);
        }

        list.add(object);
    }

    /**
     * Getter for a stored object.
     * <br>
     * If the the object that is retrieved is not assignable from the
     * class given as parameter, null is returned and a warning is logged.
     *
     * @param key         identifier of the object to be retrieved
     * @param objectClass object class - object is casted to this class before return
     * @param <T>         Generic type of class
     * @return the object identified by the key, cast to the class. Null if class is wrong or object not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> objectClass) {
        Object object = state.get(key);
        if (object == null) {
            logger.debug("Retrieved null from state with key=" + key);
            return null;
        }
        if (!objectClass.isAssignableFrom(object.getClass())) {
            logger.warn("Retrieved object from state where did not match expected. Class=" + object.getClass() + ", expected=" + objectClass);
            return null;
        }
        return (T) object;
    }

    /**
     * Compute if absent, similar to the one from the {@link Map} interface
     *
     * @param key      identifier of the object to be retrieved
     * @param function function to compute if key is absent
     * @param <T>      Generic type of class
     * @return The object either computed or found
     */
    public <T> T computeIfAbsent(String key, Function<String, T> function) {
        return (T) state.computeIfAbsent(key, function);
    }

    /**
     * Resets memory, for testing purposes only.
     */
    public void reset() {
        state.clear();
    }
}
