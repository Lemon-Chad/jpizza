package lemon.jpizza.compiler.values;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ValueArray implements Serializable {
    public int length;
    public List<Value> values;

    public ValueArray() {
        this.length = 0;
        this.values = new ArrayList<>();
    }

    public void write(Value value) {
        values.add(value);
        length++;
    }

    public void free() {
        values.clear();
        length = 0;
    }
}
