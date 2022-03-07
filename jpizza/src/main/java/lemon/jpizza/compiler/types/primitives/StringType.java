package lemon.jpizza.compiler.types.primitives;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;

public class StringType extends PrimitiveType {
    static final StringType INSTANCE = new StringType();

    private StringType() {
        super("String");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.Plus) {
            return Types.STRING;
        }
        else if (operation == TokenType.Star && other == Types.INT) {
            return Types.STRING;
        }
        else if (operation == TokenType.LeftBracket && other == Types.INT) {
            return Types.STRING;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.STRING};
    }
}
