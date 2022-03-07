package lemon.jpizza.compiler.types;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.primitives.PrimitiveType;

public class VoidType extends PrimitiveType {
    static final VoidType INSTANCE = new VoidType();

    private VoidType() {
        super("void");
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
        return new int[]{TypeCodes.VOID};
    }
}
