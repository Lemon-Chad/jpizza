package lemon.jpizza.compiler.types;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.values.Value;

public class GenericType extends Type {
    public GenericType(String name) {
        super(name);
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
    public Type call(Type[] arguments, Type[] generics) {
        return null;
    }

    @Override
    public Type access(String name) {
        return null;
    }

    @Override
    public Type accessInternal(String name) {
        return null;
    }

    @Override
    public int[] dump() {
        int[] name = Value.dumpString(this.name);
        int[] result = new int[name.length + 1];
        result[0] = TypeCodes.GENERIC;
        System.arraycopy(name, 0, result, 1, name.length);
        return result;
    }
}
