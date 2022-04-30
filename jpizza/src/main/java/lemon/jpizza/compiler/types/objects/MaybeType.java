package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.primitives.PrimitiveType;

public class MaybeType extends PrimitiveType {
    private final Type optionalType;

    public MaybeType(Type optionalType) {
        super("?" + optionalType.toString());
        this.optionalType = optionalType;
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
        int[] subtype = optionalType.dump();
        int[] result = new int[subtype.length + 1];
        result[0] = TypeCodes.MAYBE;
        System.arraycopy(subtype, 0, result, 1, subtype.length);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MaybeType) {
            return optionalType.equals(((MaybeType)other).optionalType);
        }
        return false;
    }

    public Type getOptionalType() {
        return optionalType;
    }
}
