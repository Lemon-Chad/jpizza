package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.GenericType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FuncType extends Type {
    public final Type returnType;
    public final Type[] parameterTypes;
    public final GenericType[] generics;
    public final boolean varargs;
    public final int defaultCount;

    public FuncType(Type returnType, Type[] parameterTypes, GenericType[] generics, boolean varargs) {
        this(returnType, parameterTypes, generics, varargs, 0);
    }

    public FuncType(Type returnType, Type[] parameterTypes, GenericType[] generics, boolean varargs, int defaultCount) {
        super("function");
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.generics = generics;
        this.varargs = varargs;
        this.defaultCount = defaultCount;
    }

    @Override
    public boolean callable() {
        return true;
    }

    @Override
    public Type applyGenerics(Map<Type, Type> generics) {
        Type returnType = this.returnType.applyGenerics(generics);
        Type[] parameterTypes = new Type[this.parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = this.parameterTypes[i].applyGenerics(generics);
        }
        return new FuncType(returnType, parameterTypes, this.generics, this.varargs, this.defaultCount);
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
        return call(arguments, generics, new HashMap<>());
    }

    public Type call(Type[] arguments, Type[] generics, Map<Type, Type> inherited) {
        if (generics.length != this.generics.length) {
            return null;
        }
        Map<Type, Type> map = new HashMap<>(inherited);
        for (int i = 0; i < generics.length; i++) {
            map.put(this.generics[i], generics[i]);
        }

        if (arguments.length < parameterTypes.length - defaultCount || (!varargs && arguments.length > parameterTypes.length)) {
            return null;
        }
        for (int i = 0; i < Math.min(parameterTypes.length, arguments.length); i++) {
            Type expected = parameterTypes[i].applyGenerics(map);
            if (!expected.equals(arguments[i])) {
                return null;
            }
        }

        return returnType.applyGenerics(map);
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FuncType)) {
            return false;
        }
        FuncType other = (FuncType) o;
        if (!returnType.equals(other.returnType)) {
            return false;
        }
        if (parameterTypes.length != other.parameterTypes.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!parameterTypes[i].equals(other.parameterTypes[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int[] dump() {
        List<Integer> list = new ArrayList<>();
        list.add(TypeCodes.FUNC);
        list.addAll(returnType.dumpList());
        list.add(parameterTypes.length);
        for (Type type : parameterTypes) {
            list.addAll(type.dumpList());
        }
        list.add(generics.length);
        for (GenericType generic : generics) {
            list.addAll(generic.dumpList());
        }
        list.add(varargs ? 1 : 0);
        list.add(defaultCount);
        return list.stream().mapToInt(Integer::intValue).toArray();
    }
}
