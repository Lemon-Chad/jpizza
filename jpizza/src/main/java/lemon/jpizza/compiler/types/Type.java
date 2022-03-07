package lemon.jpizza.compiler.types;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.ChunkCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Type {
    public String name;

    public Type(final String name) {
        this.name = name;
    }

    // Binary operations
    // If it returns null, operation is incompatible
    protected abstract Type operation(final TokenType operation, final Type other);
    public Type isCompatible(final TokenType operation, final Type other) {
        if (operation == TokenType.EqualEqual || operation == TokenType.BangEqual) {
            return Types.BOOL;
        }
        else if (other instanceof AnyType) {
            return Types.ANY;
        }
        else if (operation == TokenType.Colon) {
            return this instanceof VoidType ? other : this;
        }
        return operation(operation, other);
    }

    // Unary operations
    // If it returns null, operation is incompatible
    protected abstract Type operation(TokenType operation);
    public Type isCompatible(TokenType operation) {
        return operation(operation);
    }

    public abstract Type call(final Type[] arguments, final Type[] generics);

    public abstract Type access(final String name);
    public abstract Type accessInternal(final String name);

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    public int[] compile() {
        int[] dump = dump();
        int[] result = new int[dump.length + 1];
        result[0] = ChunkCode.Type;
        System.arraycopy(dump, 0, result, 1, dump.length);
        return result;
    }

    public abstract int[] dump();

    public List<Integer> dumpList() {
        return Arrays.stream(compile()).boxed().collect(Collectors.toList());
    }
}
