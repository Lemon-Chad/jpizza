package lemon.jpizza.compiler.types;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.primitives.PrimitiveType;

public class AnyType extends PrimitiveType {
    static final AnyType INSTANCE = new AnyType();

    private AnyType() {
        super("any");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        return INSTANCE;
    }

    @Override
    protected Type operation(TokenType operation) {
        return INSTANCE;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.ANY};
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Type;
    }
}
