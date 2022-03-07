package lemon.jpizza.compiler.types.primitives;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;

public class DictType extends PrimitiveType {
    static final DictType INSTANCE = new DictType();

    private DictType() {
        super("dict");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.LeftBracket || operation == TokenType.Dot) {
            return Types.ANY;
        }
        else if (operation == TokenType.Plus) {
            return other == Types.DICT ? Types.DICT : null;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.DICT};
    }
}
