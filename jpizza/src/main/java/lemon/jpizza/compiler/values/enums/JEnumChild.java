package lemon.jpizza.compiler.values.enums;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.classes.ClassAttr;
import lemon.jpizza.compiler.values.classes.Instance;
import lemon.jpizza.compiler.vm.VM;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JEnumChild implements Serializable {
    final int value;
    JEnum parent;

    private final Value asValue;

    // For Enum Props
    public final List<String> props;
    public final List<List<String>> propTypes;
    public final List<String> generics;

    public JEnumChild(int value, List<String> props, List<List<String>> propTypes, List<String> generics) {
        this.value = value;
        this.props = props;
        this.propTypes = propTypes;
        this.generics = generics;

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

    public Value create(Value[] args, String[] types, String[] resolvedGenerics, VM vm) {
        Map<String, ClassAttr> fields = new HashMap<>();
        for (int i = 0; i < props.size(); i++) {
            fields.put(
                    props.get(i),
                    new ClassAttr(args[i], types[i])
            );
        }

        fields.put("$child", new ClassAttr(new Value(value)));
        fields.put("$parent", new ClassAttr(new Value(parent)));

        StringBuilder type = new StringBuilder(parent.name());
        if (generics.size() > 0) {
            type.append("(");
            for (int i = 0; i < generics.size(); i++) {
                type.append("(").append(resolvedGenerics[i]).append(")");
            }
            type.append(")");
        }
        // Normal: EnumChild
        // Generic: EnumChild((Type1)(Type2)(etc))
        return new Value(new Instance(type.toString(), fields, vm));
    }

    public Value asValue() {
        return asValue;
    }
}
