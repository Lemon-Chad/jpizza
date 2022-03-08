package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.GenericType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.values.Value;

import java.util.*;

public class EnumChildType extends Type {
    public final Type[] propertyArguments;
    public final GenericType[] propertyGenerics;
    public final Map<String, GenericType> propertyGenericMap;
    public final String[] properties;
    public final ClassType asClass;

    public EnumChildType(String name, Type[] propertyArguments, GenericType[] propertyGenerics, String[] properties) {
        super(name);
        this.propertyArguments = propertyArguments;
        this.propertyGenerics = propertyGenerics;
        this.propertyGenericMap = new HashMap<>();
        this.properties = properties;

        Map<String, Type> map = new HashMap<>();
        for (int i = 0; i < properties.length; i++) {
            map.put(properties[i], propertyArguments[i]);
        }
        for (int i = 0; i < propertyGenerics.length; i++) {
            map.put(propertyGenerics[i].name, propertyGenerics[i]);
        }
        this.asClass = new ClassType(name, null, null, map, new HashSet<>(), new HashMap<>(), new HashMap<>(), propertyGenerics);
    }

    @Override
    public boolean callable() {
        return true;
    }

    @Override
    public Type applyGenerics(Map<Type, Type> generics) {
        Type[] arguments = new Type[propertyArguments.length];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = propertyArguments[i].applyGenerics(generics);
        }
        return new EnumChildType(name, arguments, propertyGenerics, properties);
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
        Map<Type, Type> map = new HashMap<>();
        for (int i = 0; i < generics.length; i++) {
            map.put(propertyGenerics[i], generics[i]);
        }

        if (arguments.length != propertyArguments.length) {
            return null;
        }
        for (int i = 0; i < arguments.length; i++) {
            if (!propertyArguments[i].applyGenerics(map).equals(arguments[i])) {
                return null;
            }
        }
        if (generics.length != propertyGenerics.length) {
            return null;
        }
        return new InstanceType(asClass, generics);
    }

    @Override
    public Type access(String name) {
        return null;
    }

    @Override
    public Type accessInternal(String name) {
        return null;
    }

    @Override
    public int[] dump() {
        List<Integer> list = new ArrayList<>();
        list.add(TypeCodes.ENUMCHILD);
        Value.addAllString(list, name);
        list.add(propertyArguments.length);
        for (Type type : propertyArguments) {
            list.addAll(type.dumpList());
        }
        list.add(propertyGenerics.length);
        for (GenericType type : propertyGenerics) {
            list.addAll(type.dumpList());
        }
        list.add(properties.length);
        for (String property : properties) {
            Value.addAllString(list, property);
        }
        return list.stream().mapToInt(Integer::intValue).toArray();
    }
}
