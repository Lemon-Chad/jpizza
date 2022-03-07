package lemon.jpizza.compiler;

import lemon.jpizza.compiler.types.GenericType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.TypeCodes;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.objects.*;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.ValueArray;
import lemon.jpizza.compiler.values.enums.JEnum;
import lemon.jpizza.compiler.values.enums.JEnumChild;
import lemon.jpizza.compiler.values.functions.JFunc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class ChunkBuilder {
    int i = 0;
    int[] code;
    final TypeReader reader;

    private ChunkBuilder(byte[] code) {
        this.code = new int[code.length / 4];
        this.reader = new TypeReader();
        for (int i = 0; i < this.code.length; i++) {
            int v = 0;
            v |= (code[i * 4    ] & 0xFF) << 24;
            v |= (code[i * 4 + 1] & 0xFF) << 16;
            v |= (code[i * 4 + 2] & 0xFF) <<  8;
            v |= (code[i * 4 + 3] & 0xFF);
            this.code[i] = v;
        }
    }

    private class TypeReader {

        private Map<String, Type> readAttributes() throws IOException {
            int size = code[i++];
            Map<String, Type> attributes = new HashMap<>();
            for (int j = 0; j < size; j++) {
                String name = readString();
                Type type = readType();
                attributes.put(name, type);
            }
            return attributes;
        }

        private Type readClass() throws IOException {
            if (code[i++] != TypeCodes.CLASS)
                throw new IOException("Invalid type code");
            String name = readString();
            ClassType parent = null;
            if (code[i++] == 1) {
                parent = (ClassType) readType();
            }
            FuncType constructor = (FuncType) readType();

            Map<String, Type> fields = new HashMap<>();
            Set<String> privates = new HashSet<>();
            int size = code[i++];
            for (int j = 0; j < size; j++) {
                String fieldName = readString();
                Type type = readType();
                fields.put(fieldName, type);
                if (code[i++] == 1) {
                    privates.add(fieldName);
                }
            }
            Map<String, Type> staticFields = readAttributes();
            Map<String, Type> operators = readAttributes();
            return new ClassType(name, parent, constructor, fields, privates, staticFields, operators, constructor.generics);
        }

        public Type readEnumChild() throws IOException {
            if (code[i++] != TypeCodes.ENUMCHILD)
                throw new IOException("Invalid type code");

            String name = readString();
            int proptypeCount = code[i++];
            Type[] proptype = new Type[proptypeCount];
            GenericType[] generics = readArgs(proptypeCount, proptype);
            int propCount = code[i++];
            String[] props = new String[propCount];
            for (int j = 0; j < propCount; j++) {
                props[j] = readString();
            }
            return new EnumChildType(name, proptype, generics, props);
        }

        public Type readEnum() throws IOException {
            if (code[i++] != TypeCodes.ENUM)
                throw new IOException("Invalid type code");
            String name = readString();
            int childCount = code[i++];
            EnumChildType[] children = new EnumChildType[childCount];
            for (int j = 0; j < childCount; j++) {
                children[j] = (EnumChildType) readType();
            }
            return new EnumType(name, children);
        }

        public Type readFunc() throws IOException {
            if (code[i++] != TypeCodes.FUNC)
                throw new IOException("Invalid type code");
            Type returnType = readType();
            int argCount = code[i++];
            Type[] args = new Type[argCount];
            GenericType[] generics = readArgs(argCount, args);
            boolean isVararg = code[i++] == 1;
            int defaultCount = code[i++];
            return new FuncType(returnType, args, generics, isVararg, defaultCount);
        }

        GenericType[] readArgs(int argCount, Type[] args) throws IOException {
            for (int j = 0; j < argCount; j++) {
                args[j] = readType();
            }
            int genericCount = code[i++];
            GenericType[] generics = new GenericType[genericCount];
            for (int j = 0; j < genericCount; j++) {
                generics[j] = (GenericType) readType();
            }
            return generics;
        }

        public Type readInstance() throws IOException {
            if (code[i++] != TypeCodes.INSTANCE)
                throw new IOException("Invalid type code");
            ClassType type = (ClassType) readType();
            int genericCount = code[i++];
            Type[] generics = new Type[genericCount];
            for (int j = 0; j < genericCount; j++) {
                generics[j] = readType();
            }
            return new InstanceType(type, generics);
        }

        public Type readNamespace() throws IOException {
            if (code[i++] != TypeCodes.NAMESPACE)
                throw new IOException("Invalid type code");
            return new NamespaceType(readAttributes());
        }

        public Type readReference() throws IOException {
            if (code[i] != TypeCodes.REFERENCE)
                throw new IOException("Invalid type code");
            i++;
            return new ReferenceType(readType());
        }
    }

    private String readString() throws IOException {
        if (code[i] != ChunkCode.String)
            throw new IOException("not string");
        i++;
        int len = code[i++];
        byte[] bytes = new byte[len];
        for (int j = 0; j < len; j++) {
            bytes[j] = (byte)(code[i++] >> 2);
        }
        return new String(bytes);
    }

    private Type readType() throws IOException {
        if (code[i] != ChunkCode.Type)
            throw new IOException("not type");
        i++;
        int typeType = code[i];
        switch (typeType) {
            // Objects
            case TypeCodes.CLASS:
                return reader.readClass();
            case TypeCodes.ENUMCHILD:
                return reader.readEnumChild();
            case TypeCodes.ENUM:
                return reader.readEnum();
            case TypeCodes.FUNC:
                return reader.readFunc();
            case TypeCodes.INSTANCE:
                return reader.readInstance();
            case TypeCodes.NAMESPACE:
                return reader.readNamespace();
            case TypeCodes.REFERENCE:
                return reader.readReference();

            // Primitives
            case TypeCodes.BOOL:
                i++;
                return Types.BOOL;
            case TypeCodes.BYTES:
                i++;
                return Types.BYTES;
            case TypeCodes.DICT:
                i++;
                return Types.DICT;
            case TypeCodes.FLOAT:
                i++;
                return Types.FLOAT;
            case TypeCodes.INT:
                i++;
                return Types.INT;
            case TypeCodes.LIST:
                i++;
                return Types.LIST;
            case TypeCodes.RESULT:
                i++;
                return Types.RESULT;
            case TypeCodes.STRING:
                i++;
                return Types.STRING;
            case TypeCodes.VOID:
                i++;
                return Types.VOID;
            case TypeCodes.ANY:
                i++;
                return Types.ANY;

            // Generic
            case TypeCodes.GENERIC:
                i++;
                return new GenericType(readString());
        }
        throw new IOException("unknown type");
    }

    private boolean readBoolean() throws IOException {
        if (code[i] != ChunkCode.Boolean)
            throw new IOException("not boolean");
        i++;
        return code[i++] != 0;
    }

    private double readDouble() throws IOException {
        if (code[i] != ChunkCode.Number)
            throw new IOException("not double");
        i++;
        int a = code[i++];
        int b = code[i++];
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putInt(a);
        bb.putInt(b);
        return bb.getDouble(0);
    }

    private JEnum readEnum() throws IOException {
        if (code[i] != ChunkCode.Enum)
            throw new IOException("not enum");
        i++;
        String name = readString();
        int len = code[i++];
        Map<String, JEnumChild> map = new HashMap<>();
        for (int j = 0; j < len; j++) {
            String childName = readString();
            JEnumChild child = readEnumChild();
            map.put(childName, child);
        }
        return new JEnum(name, map);
    }

    private JEnumChild readEnumChild() throws IOException {
        if (code[i] != ChunkCode.EnumChild)
            throw new IOException("not enum child");
        i++;
        int val = code[i++];
        int propsSize = code[i++];
        List<String> props = new ArrayList<>();
        for (int j = 0; j < propsSize; j++) {
            props.add(readString());
        }
        return new JEnumChild(val, props);
    }

    private JFunc readFunc() throws IOException {
        if (code[i] != ChunkCode.Func)
            throw new IOException("not func");
        i++;
        int arity = code[i++];
        int totalArity = code[i++];

        String name = null;
        if (code[i] == 0) i++;
        else name = readString();

        int upvalueCount = code[i++];

        boolean async = code[i++] != 0;
        boolean catcher = code[i++] != 0;
        boolean varargs = code[i++] != 0;
        boolean kwargs = code[i++] != 0;

        Chunk chunk = readChunk();

        JFunc func = new JFunc(chunk.source);
        func.name = name;
        func.arity = arity;
        func.totarity = totalArity;
        func.upvalueCount = upvalueCount;
        func.async = async;
        func.catcher = catcher;
        func.varargs = varargs;
        func.kwargs = kwargs;
        func.chunk = chunk;
        return func;
    }

    private Value readValue() throws IOException {
        switch (code[i]) {
            case ChunkCode.Boolean: return new Value(readBoolean());
            case ChunkCode.Number: return new Value(readDouble());
            case ChunkCode.String: return new Value(readString());
            case ChunkCode.Enum: return new Value(readEnum());
            case ChunkCode.Func: return new Value(readFunc());
            default: return null;
        }
    }

    private Chunk readChunk() throws IOException {
        if (code[i] != ChunkCode.Chunk)
            throw new IOException("not chunk");
        i++;

        String source = readString();

        String packageName = null;
        if (code[i] == 0) {
            i++;
        }
        else {
            packageName = readString();
        }

        String target = null;
        if (code[i] == 0) {
            i++;
        }
        else {
            target = readString();
        }

        List<FlatPosition> positions = new ArrayList<>();
        int length = code[i++];
        for (int j = 0; j < length; j++) {
            int index = code[i++];
            int len = code[i++];
            int span = code[i++];
            positions.add(new FlatPosition(index, len, span));
        }

        int constantCount = code[i++];
        Value[] constants = new Value[constantCount];
        for (int j = 0; j < constantCount; j++) {
            constants[j] = readValue();
        }
        ValueArray values = new ValueArray(constants);

        Map<String, Type> globals = reader.readAttributes();

        int bytecodeCount = code[i++];
        int[] bytecodes = new int[bytecodeCount];
        for (int j = 0; j < bytecodeCount; j++) {
            bytecodes[j] = code[i++];
        }

        Chunk chunk = new Chunk(source);
        chunk.packageName = packageName;
        chunk.target = target;
        chunk.positions = positions;
        chunk.codeArray = bytecodes;
        List<Integer> code = new ArrayList<>();
        for (int j = 0; j < bytecodes.length; j++) {
            code.add(bytecodes[j]);
        }
        chunk.code = code;
        chunk.constants = values;
        chunk.globals = globals;
        return chunk;
    }

    public static JFunc Build(byte[] code) throws IOException {
        return new ChunkBuilder(code).readFunc();
    }
}
