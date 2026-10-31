package itu.webdynamique.framework.session;

import java.util.HashMap;
import java.util.Map;

/**
 * Objet session utilisateur personnalise.
 * Encapsule une Map<String, Object> stockee dans le ServletContext
 * et indexee par l'identifiant de la session HTTP.
 */
public class Session {

    private final Map<String, Object> attributes = new HashMap<>();

    public Object get(String key) {
        return attributes.get(key);
    }

    public void set(String key, Object value) {
        attributes.put(key, value);
    }

    public void remove(String key) {
        attributes.remove(key);
    }

    public void clear() {
        attributes.clear();
    }

    public Map<String, Object> getAll() {
        return new HashMap<>(attributes);
    }

    public boolean contains(String key) {
        return attributes.containsKey(key);
    }

    @Override
    public String toString() {
        return "Session{attributes=" + attributes + "}";
    }
}