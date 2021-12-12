package lemon.jpizza.compiler.values.functions;

import lemon.jpizza.compiler.values.Value;

import java.util.List;

public class Spread {
    public final List<Value> values;
    public Spread(List<Value> values) {
        this.values = values;
    }
}
