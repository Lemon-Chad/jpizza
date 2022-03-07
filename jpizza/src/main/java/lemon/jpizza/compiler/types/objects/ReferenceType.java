package lemon.jpizza.compiler.types.objects;

import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;

import java.util.ArrayList;
import java.util.List;

public class ReferenceType extends Type {
    public Type ref;

    public ReferenceType(Type ref) {
        super(null);
        updateRef(ref);
    }

    private void updateRef(Type ref) {
        this.name = "[" + ref.name + "]";
        this.ref = ref;
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.FatArrow) {
            updateRef(other);
            return Types.VOID;
        }
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
        List<Integer> list = new ArrayList<>();
        list.add(TypeCodes.REFERENCE);
        list.addAll(ref.dumpList());
        return list.stream().mapToInt(Integer::intValue).toArray();
    }
}
