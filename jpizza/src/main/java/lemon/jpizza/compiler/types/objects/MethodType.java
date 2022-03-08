package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;

import java.util.Map;

public class MethodType extends Type {
    public final FuncType funcType;
    public final Map<Type, Type> generics;

    public MethodType(FuncType inner, Map<Type, Type> generics) {
        super("method");
        this.funcType = inner;
        this.generics = generics;
    }

    @Override
    public boolean callable() {
        return true;
    }

    @Override
    public Type applyGenerics(Map<Type, Type> generics) {
        return new MethodType((FuncType) funcType.applyGenerics(generics), generics);
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
        return funcType.call(arguments, generics, this.generics);
    }

    @Override
    public Type access(String name) {
        return null;
    }

    @Override
    public Type accessInternal(String name) {
        return null;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return funcType.equals(o);
    }

    @Override
    public int[] dump() {
        return funcType.dump();
    }
}
