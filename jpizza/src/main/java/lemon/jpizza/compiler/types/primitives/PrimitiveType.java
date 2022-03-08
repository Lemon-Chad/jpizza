package lemon.jpizza.compiler.types.primitives;

import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;

import java.util.Map;

public abstract class PrimitiveType extends Type {
    public PrimitiveType(String name) {
        super(name);
    }

    @Override
    public Type call(final Type[] args, final Type[] generics) {
        return null;
    }

    @Override
    public Type access(final String name) {
        return null;
    }

    @Override
    public Type accessInternal(final String name) {
        return null;
    }

    @Override
    public Type applyGenerics(final Map<Type, Type> generics) {
        return this;
    }

}
