package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.values.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamespaceType extends Type {
    public final Map<String, Type> attributes;

    public NamespaceType(Map<String, Type> attributes) {
        super("namespace");
        this.attributes = attributes;
    }

    @Override
    public Type applyGenerics(Map<Type, Type> generics) {
        Map<String, Type> newAttributes = new HashMap<>();
        for (Map.Entry<String, Type> entry : attributes.entrySet()) {
            newAttributes.put(entry.getKey(), entry.getValue().applyGenerics(generics));
        }
        return new NamespaceType(newAttributes);
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public Type call(Type[] arguments, Type[] generics) {
        return null;
    }

    @Override
    public Type access(String name) {
        return attributes.get(name);
    }

    @Override
    public List<String> accessors() {
        return new ArrayList<>(attributes.keySet());
    }

    @Override
    public Type accessInternal(String name) {
        return null;
    }

    @Override
    public int[] dump() {
        List<Integer> list = new ArrayList<>();
        list.add(TypeCodes.NAMESPACE);
        compileAttributes(list, attributes);
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    static void compileAttributes(List<Integer> list, Map<String, Type> attributes) {
        list.add(attributes.size());
        for (Map.Entry<String, Type> entry : attributes.entrySet()) {
            Value.addAllString(list, entry.getKey());
            list.addAll(entry.getValue().dumpList());
        }
    }
}
