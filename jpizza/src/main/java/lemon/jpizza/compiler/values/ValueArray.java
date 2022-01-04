package lemon.jpizza.compiler.values;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ValueArray {
    public int length;
    public final List<Value> values;
    public Value[] valuesArray;

    public ValueArray() {
        this.length = 0;
        this.values = new ArrayList<>();
    }

    public ValueArray(Value[] constants) {
        this.length = constants.length;
        this.values = List.of(constants);
        this.valuesArray = constants;
    }

    public int write(Value value) {
        int index = values.indexOf(value);
        if (index == -1) {
            values.add(value);
            index = length++;
        }
        return index;
    }

    public void compile() {
        valuesArray = values.toArray(new Value[0]);
    }

    public ValueArray copy() {
        ValueArray copy = new ValueArray();
        copy.length = length;
        for (Value value : values) {
            copy.values.add(value.copy());
        }
        copy.compile();
        return copy;
    }

    public int[] dump() {
        List<Integer> list = new ArrayList<>();
        list.add(length);
        for (Value value : values) {
            for (int i : value.dump()) {
                list.add(i);
            }
        }
        return list.stream().mapToInt(i -> i).toArray();
    }
}
