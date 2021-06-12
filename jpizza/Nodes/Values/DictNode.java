package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Position;

import java.util.Map;

public class DictNode extends Node {
    public Map<Object, Object> dict;

    public DictNode(Map<Object, Object> dict, Position pos_start, Position pos_end) {
        this.dict = dict;
        this.pos_start = pos_start;
        this.pos_end = pos_end;
    }

    public Object get(Object key) {
        return dict.get(key);
    }

    public DictNode delete(Object key) {
        dict.remove(key);
        return this;
    }

    public DictNode set(Object key, Object value) {
        dict.put(key, value);
        return this;
    }



}
