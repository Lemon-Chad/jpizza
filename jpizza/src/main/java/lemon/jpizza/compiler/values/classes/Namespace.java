package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Namespace {
    String name;
    Map<String, Var> values;
    List<String> publics;

    public Namespace(String name, Map<String, Var> values, List<String> publics) {
        this.name = name;
        this.values = values;
        this.publics = publics;
    }

    public Namespace(String name, Map<String, Var> values) {
        this(name, values, new ArrayList<>(values.keySet()));
    }

    public String getName() {
        return name;
    }

    public Map<String, Var> getValues() {
        return values;
    }

    public Var getValue(String name, boolean internal) {
        return publics.contains(name) || internal ? values.get(name) : null;
    }

    public Value getField(String name, boolean internal) {
        Var var = getValue(name, internal);
        return var != null ? var.val : null;
    }

    public void addField(String name, Value val) {
        values.put(name, new Var(val, false));
        publics.add(name);
    }

    public String name() {
        return name;
    }

    public Map<String, Var> values() {
        return values;
    }
}
