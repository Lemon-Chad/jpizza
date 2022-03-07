package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.GenericType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.values.Value;

import java.util.*;

import static lemon.jpizza.compiler.types.objects.NamespaceType.compileAttributes;

public class ClassType extends Type {
    public String identifier;
    public ClassType parent;
    public FuncType constructor;
    public final Set<String> privates;
    public final Map<String, Type> fields;
    public final Map<String, Type> staticFields;
    public final Map<String, Type> operators;
    public final GenericType[] generics;

    public ClassType(String name, ClassType parent, FuncType constructor, Map<String, Type> fields, Set<String> privates, Map<String, Type> staticFields, Map<String, Type> operators, GenericType[] generics) {
        super("recipe");
        this.identifier = name;
        this.parent = parent;
        this.constructor = constructor;
        this.privates = privates;
        this.fields = fields;
        this.fields.putAll(operators);
        this.staticFields = staticFields;
        this.operators = operators;
        this.generics = generics;

        if (parent != null) {
            for (Map.Entry<String, Type> entry : parent.fields.entrySet()) {
                if (!fields.containsKey(entry.getKey()) && parent.privates.contains(entry.getKey())) {
                    privates.add(entry.getKey());
                }
                fields.putIfAbsent(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Type> entry : parent.staticFields.entrySet()) {
                staticFields.putIfAbsent(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Type> entry : parent.operators.entrySet()) {
                operators.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
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
        Type type = constructor.call(arguments, generics);
        if (type == null) return null;
        return new InstanceType(this, generics);
    }

    @Override
    public Type access(String name) {
        Type field = staticFields.get(name);
        if (field != null) return field;
        if (parent != null) return parent.access(name);
        return null;
    }

    @Override
    public Type accessInternal(String name) {
        return get(name, true);
    }

    public Type get(String name, boolean internal) {
        Type field = fields.get(name);
        /*
             A B Output
             0 0    1
             0 1    1
             1 0    0
             1 1    1
             The operation for this is: !A || B
         */
        if (field != null) return !privates.contains(name) || internal ? field : null;
        if (parent != null) return parent.get(name, internal);
        return null;
    }

    public Type getOperator(String name) {
        Type field = operators.get(name);
        if (field != null) return field;
        if (parent != null) return parent.getOperator(name);
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ClassType)) {
            return false;
        }
        ClassType otherType = (ClassType) other;
        return identifier.equals(otherType.identifier) ||
                Objects.equals(otherType, parent) ||
                Objects.equals(this, otherType.parent) ||
                (parent != null && parent.equals(otherType.parent));
    }

    @Override
    public int[] dump() {
        List<Integer> list = new ArrayList<>();
        list.add(TypeCodes.CLASS);
        Value.addAllString(list, identifier);
        list.add(parent == null ? 0: 1);
        if (parent != null) {
            list.addAll(parent.dumpList());
        }
        list.addAll(constructor.dumpList());
        list.add(fields.size());
        for (Map.Entry<String, Type> entry : fields.entrySet()) {
            Value.addAllString(list, entry.getKey());
            list.addAll(entry.getValue().dumpList());
            list.add(privates.contains(entry.getKey()) ? 1 : 0);
        }
        compileAttributes(list, staticFields);
        compileAttributes(list, operators);
        return list.stream().mapToInt(Integer::intValue).toArray();
    }
}
