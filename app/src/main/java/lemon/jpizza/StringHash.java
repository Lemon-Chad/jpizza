package lemon.jpizza;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StringHash implements Serializable {
    HashMap<String, Object> set = new HashMap<>();
    public StringHash() {}
    public StringHash(StringHash parent) {
        addAll(parent);
    }

    public void add(String key) {
        set.put(key, "EMPTY");
    }

    public void remove(String key) {
        set.remove(key);
    }

    public void addAll(StringHash other) {
        set.putAll((Map<? extends String, ?>) other.set.clone());
    }

    public boolean contains(String key) {
        return set.get(key) != null;
    }

}
