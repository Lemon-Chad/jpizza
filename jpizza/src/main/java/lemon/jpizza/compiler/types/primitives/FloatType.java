package lemon.jpizza.compiler.types.primitives;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;

import java.util.Arrays;

import static lemon.jpizza.compiler.types.primitives.IntType.VALID_OPS;

public class FloatType extends PrimitiveType {
    static final FloatType INSTANCE = new FloatType();

    private FloatType() {
        super("float");
    }

    @Override
    public Type operation(TokenType operation, Type other) {
        if (other != Types.INT && other != Types.FLOAT) {
            return null;
        }
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return Types.FLOAT;
        }
        return null;
    }

    @Override
    public Type operation(TokenType operation) {
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return this;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        // An int can be a float, but a float can't be an int.
        return o instanceof FloatType || o instanceof IntType;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.FLOAT};
    }
}
