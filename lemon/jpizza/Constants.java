package lemon.jpizza;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {
    public static char[] NUMBERS = "0123456789".toCharArray();
    public static char[] NUMDOT = "0123456789.".toCharArray();
    public static char[] LETTERS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static char[] LETTERS_DIGITS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();
    public static String[] KEYWORDS = {
            "struct",
            "do",
            "loop",
            "pass",
            "cal",
            "yourmom",
            "async",
            "import",
            "bin",
            "function",
            "attr",
            "bake",
            "var",
            "for",
            "while",
            "null",
            "if",
            "elif",
            "else",
            "return",
            "continue",
            "break",
            "method",
            "ingredients",
            "recipe",
            "switch",
            "case",
            "fn",
            "enum",
            "default",
            "match",
    };
    @SuppressWarnings("unused") public static char BREAK = ';';
    @SuppressWarnings("unused") public static char[] IGNORE = new char[]{' ', '\n', '\t'};
    public static Map<String, Context> LIBRARIES = new HashMap<>();
    public static char splitter = '\n';

    public enum JPType {
        Bytes,
        ClassInstance,
        ClassPlate,
        CMethod,
        Function,
        Library,
        BaseFunction,
        AttrAssign,
        ClassDef,
        DynAssign,
        FuncDef,
        MethDef,
        VarAssign,
        Break,
        Call,
        Claccess,
        Continue,
        For,
        Import,
        Iter,
        Pass,
        Query,
        Return,
        Use,
        While,
        BinOp,
        UnaryOp,
        Boolean,
        Dict,
        List,
        Null,
        Number,
        String,
        Value,
        AttrAccess,
        Attr,
        VarAccess,
        Var,
        Generic,
        Switch,
        Enum,
        EnumChild,
    }

    public static String stringWithArrows(String text, Position pos_start, Position pos_end) {
        StringBuilder result = new StringBuilder();
        int idx_start = Math.max(text.lastIndexOf(splitter, pos_start.idx), 0);
        int idx_end = text.indexOf(splitter, idx_start + 1);
        int line_count = pos_end.ln - pos_start.ln + 1;
        String line; int col_start; int col_end;
        for (int i = 0; i < line_count; i++) {
            if (idx_end < 0)
                    idx_end = text.length() + idx_end + 1;
            line = text.substring(idx_start, idx_end);
            col_start = i == 0 ? pos_start.tcol : 0;
            col_end = i == line_count - 1 ? pos_end.tcol : line.length() - 1;
            result.append(line).append('\n');
            result.append(" ".repeat(col_start)).append("^".repeat(col_end - col_start));
            idx_start = idx_end;
            idx_end = text.indexOf(splitter, idx_start + 1);
            if (idx_end < 0)
                idx_end = text.length();
        }
        return result.toString().replace("\t", "");
    }

    public static Obj getFromValue(Object val) {
        if (val instanceof String)
            return new Str((String) val);
        else if (val instanceof Double)
            return new Num((double) val);
        else if (val instanceof List)
            return new PList((List<Obj>) val);
        else if (val instanceof Map)
            return new Dict((Map<Obj, Obj>) val);
        else if (val instanceof byte[])
            return new Bytes((byte[]) val);
        else if (val instanceof Boolean)
            return new Bool((boolean) val);
        else return new Null();
    }

    public static byte[] objToBytes(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public static Object toObject(Obj obj) {
        if (obj instanceof Dict) {
            Dict dct = (Dict) obj;
            Map<Object, Object> objMap = new HashMap<>();
            Map<Obj, Obj> deMap = dct.trueValue();

            for (Obj k : deMap.keySet())
                objMap.put(toObject(k), toObject(deMap.get(k)));

            return objMap;
        }
        else if (obj instanceof PList) {
            PList lst = (PList) obj;
            List<Object> objLst = new ArrayList<>();

            for (Obj o : lst.trueValue())
                objLst.add(toObject(o));

            return objLst;
        }
        else if (obj instanceof Value) {
            return ((Value) obj).value;
        }
        return null;
    }

}
