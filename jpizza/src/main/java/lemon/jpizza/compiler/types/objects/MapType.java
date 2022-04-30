package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.primitives.PrimitiveType;

public class MapType extends PrimitiveType {
    private final Type keyType;
    private final Type valueType;

    public MapType(Type keyType, Type valueType) {
        super("map<" + keyType.toString() + "," + valueType.toString() + ">");
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.LeftBracket || operation == TokenType.Dot) {
            if (!other.equals(keyType))
                return null;
            return valueType;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        int[] key = keyType.dump();
        int[] value = valueType.dump();
        int[] result = new int[key.length + value.length + 1];
        result[0] = TypeCodes.MAP;
        System.arraycopy(key, 0, result, 1, key.length);
        System.arraycopy(value, 0, result, key.length + 1, value.length);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MapType) {
            MapType other = (MapType) obj;
            return keyType.equals(other.keyType) && valueType.equals(other.valueType);
        }
        return false;
    }

    public Type getKeyType() {
        return keyType;
    }

    public Type getValueType() {
        return valueType;
    }
}
