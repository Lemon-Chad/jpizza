package lemon.jpizza.compiler.values.enums;

import lemon.jpizza.compiler.values.Value;

import java.io.Serializable;
import java.util.Map;

public record JEnum(String name,
                    Map<String, JEnumChild> children) implements Serializable {
    public JEnum(String name, Map<String, JEnumChild> children) {
        this.name = name;
        this.children = children;

        for (JEnumChild child : children.values()) {
            child.setParent(this);
        }
    }

    public boolean has(String name) {
        return children.containsKey(name);
    }

    public Value get(String name) {
        return children.get(name).asValue();
    }
}
