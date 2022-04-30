package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.primitives.IntType;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class TupleType extends Type {
    private final Type[] types;
    public TupleType(Type... types) {
        // (T1, T2, ..., Tn)
        super("(" + Arrays.stream(types).map(Type::toString).collect(Collectors.joining(", ")) + ")");
        this.types = types;
    }

    @Override
    public Type applyGenerics(final Map<Type, Type> generics) {
        Type[] newTypes = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            newTypes[i] = types[i].applyGenerics(generics);
        }
        return new TupleType(newTypes);
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.LeftBracket && other instanceof IntType) {
            return Types.ANY;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TupleType)) return false;
        TupleType tupleType = (TupleType) o;
        return Arrays.equals(types, tupleType.types);
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
        int listSize = 2;
        int[][] subTypes = new int[types.length][];
        for (int i = 0; i < types.length; i++) {
            subTypes[i] = types[i].dump();
            listSize += subTypes[i].length;
        }
        int[] result = new int[listSize];
        result[0] = TypeCodes.TUPLE;
        result[1] = types.length;
        int offset = 2;
        for (int i = 0; i < types.length; i++) {
            System.arraycopy(subTypes[i], 0, result, offset, subTypes[i].length);
            offset += subTypes[i].length;
        }
        return result;
    }

    public final Type getType(int index) {
        if (index < 0 || index >= types.length) {
            return null;
        }
        return types[index];
    }
}
