package lemon.jpizza.compiler;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.ValueArray;
import lemon.jpizza.compiler.values.enums.JEnum;
import lemon.jpizza.compiler.values.enums.JEnumChild;
import lemon.jpizza.compiler.values.functions.JFunc;

import java.io.IOException;
import java.util.*;

public class ChunkBuilder {
    int i = 0;
    int[] code;
    private ChunkBuilder(byte[] code) {
        this.code = new int[code.length / 4];
        for (int i = 0; i < this.code.length; i++) {
            int v = 0;
            v |= (code[i * 4    ] & 0xFF) << 24;
            v |= (code[i * 4 + 1] & 0xFF) << 16;
            v |= (code[i * 4 + 2] & 0xFF) <<  8;
            v |= (code[i * 4 + 3] & 0xFF);
            this.code[i] = v;
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

    private List<String> readType() throws IOException {
        if (code[i] != ChunkCode.Type)
            throw new IOException("not type");
        i++;
        int len = code[i++];
        List<String> type = new ArrayList<>();
        for (int j = 0; j < len; j++) {
            type.add(readString());
        }
        return type;
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
        long v = (long)a << 32 | b;
        return Double.longBitsToDouble(v);
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
        int genericSlotsLen = code[i++];
        List<Integer> genericSlots = new ArrayList<>();
        for (int j = 0; j < genericSlotsLen; j++) {
            genericSlots.add(code[i++]);
        }
        int propsSize = code[i++];
        List<String> props = new ArrayList<>();
        for (int j = 0; j < propsSize; j++) {
            props.add(readString());
        }
        int propTypesSize = code[i++];
        List<List<String>> propTypes = new ArrayList<>();
        for (int j = 0; j < propTypesSize; j++) {
            propTypes.add(readType());
        }
        int genericsSize = code[i++];
        List<String> generics = new ArrayList<>();
        for (int j = 0; j < genericsSize; j++) {
            generics.add(readString());
        }
        return new JEnumChild(val, props, propTypes, generics, genericSlots);
    }

    private JFunc readFunc() throws IOException {
        if (code[i] != ChunkCode.Func)
            throw new IOException("not func");
        i++;
        int arity = code[i++];
        int genericArity = code[i++];
        int totalArity = code[i++];

        String name = null;
        if (code[i] == 0) i++;
        else name = readString();

        List<String> returnType = null;
        if (code[i] == 0) i++;
        else returnType = readType();

        int genericSlotsLen = code[i++];
        List<Integer> genericSlots = new ArrayList<>();
        for (int j = 0; j < genericSlotsLen; j++) {
            genericSlots.add(code[i++]);
        }

        int upvalueCount = code[i++];

        boolean async = code[i++] != 0;
        boolean catcher = code[i++] != 0;

        String args;
        if (code[i] == 0) {
            i++;
            args = null;
        }
        else {
            args = readString();
        }

        String kwargs;
        if (code[i] == 0) {
            i++;
            kwargs = null;
        }
        else {
            kwargs = readString();
        }

        Chunk chunk = readChunk();

        JFunc func = new JFunc(chunk.source);
        func.name = name;
        func.arity = arity;
        func.genericArity = genericArity;
        func.totarity = totalArity;
        func.returnType = returnType;
        func.genericSlots = genericSlots;
        func.upvalueCount = upvalueCount;
        func.async = async;
        func.catcher = catcher;
        func.args = args;
        func.kwargs = kwargs;
        func.chunk = chunk;
        return func;
    }

    private Value readValue() throws IOException {
        return switch (code[i]) {
            case ChunkCode.Boolean -> new Value(readBoolean());
            case ChunkCode.Number -> new Value(readDouble());
            case ChunkCode.String -> new Value(readString());
            case ChunkCode.Type -> Value.fromType(readType());
            case ChunkCode.Enum -> new Value(readEnum());
            case ChunkCode.Func -> new Value(readFunc());
            default -> null;
        };
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
        return chunk;
    }

    public static JFunc Build(byte[] code) throws IOException {
        return new ChunkBuilder(code).readFunc();
    }
}
