package lemon.jpizza.compiler.types.primitives;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;

import java.util.Arrays;

public class IntType extends PrimitiveType {
    static final IntType INSTANCE = new IntType();

    static final TokenType[] VALID_OPS = {
            TokenType.Plus,
            TokenType.Minus,
            TokenType.Star,
            TokenType.Slash,
            TokenType.Caret,
            TokenType.Percent,
            TokenType.RightAngle,
            TokenType.LeftAngle,
            TokenType.GreaterEquals,
            TokenType.LessEquals,
            TokenType.TildeAmpersand,
            TokenType.TildePipe,
            TokenType.TildeCaret,
            TokenType.LeftTildeArrow,
            TokenType.TildeTilde,
            TokenType.RightTildeArrow,
            TokenType.PlusPlus,
            TokenType.MinusMinus,
    };

    private IntType() {
        super("int");
    }

    @Override
    public Type operation(TokenType operation, Type other) {
        if (other != Types.INT && other != Types.FLOAT) {
            return null;
        }
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return other == Types.FLOAT ? Types.FLOAT : Types.INT;
        }
        return null;
    }

    @Override
    public Type operation(TokenType operation) {
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return Types.INT;
        }
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.INT};
    }
}
