package lemon.jpizza.compiler.values;

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
        return values.get(name).val;
    }

    public void addField(String name, Value val) {
        values.put(name, new Var("any", val, false));
    }
}
