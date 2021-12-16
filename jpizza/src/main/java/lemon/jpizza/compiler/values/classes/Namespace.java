package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.Var;

import java.util.Map;

public record Namespace(String name,
                        Map<String, Var> values) {

    public String getName() {
        return name;
    }

    public Map<String, Var> getValues() {
        return values;
    }

    public Var getValue(String name) {
        return values.get(name);
    }

    public Value getField(String name) {
        Var x = values.get(name);
        return x != null ? x.val : null;
    }

    public void addField(String name, Value val) {
        values.put(name, new Var("any", val, false));
    }
}
