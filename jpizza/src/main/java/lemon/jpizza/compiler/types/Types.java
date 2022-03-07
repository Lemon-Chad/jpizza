package lemon.jpizza.compiler.types;

import lemon.jpizza.compiler.types.primitives.*;

public class Types {
    public static final VoidType VOID = VoidType.INSTANCE;
    public static final AnyType ANY = AnyType.INSTANCE;
    public static final IntType INT = PrimitiveTypes.INT;
    public static final FloatType FLOAT = PrimitiveTypes.FLOAT;
    public static final BooleanType BOOL = PrimitiveTypes.BOOL;
    public static final ListType LIST = PrimitiveTypes.LIST;
    public static final DictType DICT = PrimitiveTypes.DICT;
    public static final StringType STRING = PrimitiveTypes.STRING;
    public static final BytesType BYTES = PrimitiveTypes.BYTES;
    public static final ResultType RESULT = PrimitiveTypes.RESULT;
    public static final SpreadType SPREAD = SpreadType.INSTANCE;
}
