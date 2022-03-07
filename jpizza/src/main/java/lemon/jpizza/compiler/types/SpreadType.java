package lemon.jpizza.compiler.types;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.primitives.PrimitiveType;

public class SpreadType extends PrimitiveType {
    static final SpreadType INSTANCE = new SpreadType();

    private SpreadType() {
        super("spread");
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
