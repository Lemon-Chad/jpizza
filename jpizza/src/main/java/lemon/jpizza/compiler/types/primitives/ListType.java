package lemon.jpizza.compiler.types.primitives;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;

import java.util.Arrays;

public class ListType extends PrimitiveType {
    static final ListType INSTANCE = new ListType();

    private ListType() {
        super("list");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.Dot || operation == TokenType.LeftBracket) {
            return Types.INT.equals(other) ? Types.ANY : null;
        }
        else if (operation == TokenType.Plus) {
            return other == Types.LIST ? Types.LIST : null;
        }
        else if (operation == TokenType.Slash || operation == TokenType.Percent) {
            return Types.LIST;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.LIST};
    }
}
