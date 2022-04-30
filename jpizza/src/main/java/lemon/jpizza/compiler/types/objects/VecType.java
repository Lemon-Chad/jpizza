package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.primitives.PrimitiveType;

public class VecType extends PrimitiveType {
    private final Type itemType;

    public VecType(Type itemType) {
        super("vec<" + itemType.toString() + ">");
        this.itemType = itemType;
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.Dot || operation == TokenType.LeftBracket) {
            return Types.INT.equals(other) ? itemType : null;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        int[] subType = itemType.dump();
        int[] result = new int[subType.length + 1];
        result[0] = TypeCodes.VEC;
        System.arraycopy(subType, 0, result, 1, subType.length);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VecType && itemType.equals(((VecType) other).itemType);
    }

    public Type getItemType() {
        return itemType;
    }
}
