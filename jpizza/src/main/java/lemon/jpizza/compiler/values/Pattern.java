package lemon.jpizza.compiler.values;

import java.util.Map;

public class Pattern {
    boolean literal;
    Value value;
    Map<String, String> matches;
    Map<String, Value> cases;

    public Pattern(Value value) {
        this.value = value;
        this.literal = true;
    }

    public Pattern(Value value, Map<String, Value> cases, Map<String, String> matches) {
        this.value = value;
        this.literal = false;
        this.cases = cases;
        this.matches = matches;
    }
}
