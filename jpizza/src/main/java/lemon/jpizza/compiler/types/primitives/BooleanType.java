package lemon.jpizza.compiler.types.primitives;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;

import java.util.Arrays;

public class BooleanType extends PrimitiveType {
    static final BooleanType INSTANCE = new BooleanType();

    static final TokenType[] VALID_OPS = {
            TokenType.Ampersand,
            TokenType.Pipe,
            TokenType.Bang
    };

    private BooleanType() {
        super("bool");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (!(other instanceof BooleanType)) {
            return null;
        }
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return INSTANCE;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return INSTANCE;
        }
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.BOOL};
    }
}
