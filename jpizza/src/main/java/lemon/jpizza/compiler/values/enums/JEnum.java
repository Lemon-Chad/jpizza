package lemon.jpizza.compiler.values.enums;

import lemon.jpizza.compiler.ChunkCode;
import lemon.jpizza.compiler.values.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record JEnum(String name,
                    Map<String, JEnumChild> children) {
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

    public int[] dump() {
        List<Integer> list = new ArrayList<>(List.of(ChunkCode.Enum));
        for (int i : Value.dumpString(name)) {
            list.add(i);
        }
        list.add(children.size());
        for (Map.Entry<String, JEnumChild> entry : children.entrySet()) {
            for (int i : Value.dumpString(entry.getKey())) {
                list.add(i);
            }
            for (int i : entry.getValue().dump()) {
                list.add(i);
            }
        }
        return list.stream().mapToInt(i -> i).toArray();
    }
}
