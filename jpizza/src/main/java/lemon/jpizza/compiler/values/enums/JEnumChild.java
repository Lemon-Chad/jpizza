package lemon.jpizza.compiler.values.enums;

import lemon.jpizza.compiler.ChunkCode;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.classes.ClassAttr;
import lemon.jpizza.compiler.values.classes.Instance;
import lemon.jpizza.compiler.vm.VM;

import java.util.*;

public class JEnumChild {
    final int value;
    JEnum parent;

    private final Value asValue;

    // For Enum Props
    public final List<String> props;
    
    public final int arity;

    public JEnumChild(int value, List<String> props) {
        this.value = value;
        this.props = props;

        this.arity = props.size();

        this.asValue = new Value(this);
    }

    public int getValue() {
        return value;
    }

    public boolean equals(JEnumChild other) {
        return value == other.value;
    }

    public void setParent(JEnum jEnum) {
        parent = jEnum;
    }

    public String type() {
        return parent.name();
    }

    public JEnum getParent() {
        return parent;
    }

    public Value create(Value[] args, VM vm) {
        Map<String, ClassAttr> fields = new HashMap<>();
        for (int i = 0; i < props.size(); i++) {
            fields.put(
                    props.get(i),
                    new ClassAttr(args[i])
            );
        }

        fields.put("$child", new ClassAttr(new Value(value)));
        fields.put("$parent", new ClassAttr(new Value(parent)));

        // Normal: EnumChild
        // Generic: EnumChild((Type1)(Type2)(etc))
        return new Value(new Instance(parent.name(), fields, vm));
    }

    public Value asValue() {
        return asValue;
    }

    public int[] dump() {
        List<Integer> dump = new ArrayList<>(Arrays.asList(ChunkCode.EnumChild, value));
        dump.add(props.size());
        for (String prop : props) {
            Value.addAllString(dump, prop);
        }
        return dump.stream().mapToInt(Integer::intValue).toArray();
    }
}
