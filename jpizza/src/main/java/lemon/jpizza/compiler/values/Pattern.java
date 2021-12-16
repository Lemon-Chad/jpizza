package lemon.jpizza.compiler.values;

import java.util.Map;

public class Pattern {
    public Value value;
    public Map<String, String> matches;
    public String[] keys;
    public Map<String, Value> cases;

    public Pattern(Value value, Map<String, Value> cases, String[] keys, Map<String, String> matches) {
        this.value = value;
        this.cases = cases;
        this.matches = matches;
        this.keys = keys;
    }
}
