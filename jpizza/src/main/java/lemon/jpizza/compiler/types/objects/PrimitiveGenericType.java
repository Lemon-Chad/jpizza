package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.primitives.PrimitiveType;

public abstract class PrimitiveGenericType extends PrimitiveType {
    public final int genericCount;

    public PrimitiveGenericType(String name, int genericCount) {
        super(name);
        this.genericCount = genericCount;
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
    public int[] dump() {
        return new int[0];
    }

    public abstract Type applyGenerics(Type... generics);
}
