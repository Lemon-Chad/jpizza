package lemon.jpizza;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public class Constants {
    public static char[] NUMBERS = "0123456789".toCharArray();
    public static char[] NUMDOT = "0123456789.".toCharArray();
    public static char[] LETTERS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static char[] LETTERS_DIGITS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();
    public static String hexDigits = "0123456789abcdef";
    public static String[] KEYWORDS = {
            "free",
            "throw",
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
    
    public static Map<Tokens.TT, Operations.OP> tto = new HashMap<>(){{
        put(Tokens.TT.PLUS, Operations.OP.ADD);
        put(Tokens.TT.MINUS, Operations.OP.SUB);
        put(Tokens.TT.MUL, Operations.OP.MUL);
        put(Tokens.TT.DIV, Operations.OP.DIV);
        put(Tokens.TT.POWER, Operations.OP.FASTPOW);
        put(Tokens.TT.EE, Operations.OP.EQ);
        put(Tokens.TT.NE, Operations.OP.NE);
        put(Tokens.TT.LT, Operations.OP.LT);
        put(Tokens.TT.LTE, Operations.OP.LTE);
        put(Tokens.TT.AND, Operations.OP.INCLUDING);
        put(Tokens.TT.OR, Operations.OP.ALSO);
        put(Tokens.TT.MOD, Operations.OP.MOD);
        put(Tokens.TT.DOT, Operations.OP.GET);
        put(Tokens.TT.LSQUARE, Operations.OP.BRACKET);
    }};

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
        EnumChild, Res,
    }

    public static int nonWhitespace(String string){
        char[] characters = string.toCharArray();
        for(int i = 0; i < string.length(); i++){
            if(!Character.isWhitespace(characters[i])){
                return i;
            }
        }
        return 0;
    }

    public static String stringWithArrows(String text, Position pos_start, Position pos_end) {
        StringBuilder result = new StringBuilder();

        int idxStart = Math.max(0, text.lastIndexOf(splitter, pos_start.tidx));
        int idxEnd = text.indexOf(splitter, idxStart + 1);

        if (idxEnd < 0) idxEnd = text.length();

        int line_count = pos_end.ln - pos_start.ln + 1;
        int offs = line_count > 1 ? -1 : 0;
        for (int i = 0; i < line_count; i++) {
            String line = text.substring(idxStart, idxEnd);

            int colStart = i == 0 ? pos_start.tcol : nonWhitespace(line);
            int colEnd = i == line_count - 1 ? pos_end.tcol : line.length() - 1;

            String grouping = "";
            if (colEnd - colStart == 1) {
                grouping = "^";
            } else if (colEnd - colStart >= 2) {
                grouping = "╰" + "─".repeat(colEnd - colStart - 2) + "╯";
            }

            result.append(line).append("\n")
                    .append(" ".repeat(colStart + offs)).append(grouping);

            idxStart = idxEnd;
            idxEnd = text.indexOf(splitter, idxStart + 1);

            if (idxEnd < 0) idxEnd = text.length();
        }

        return result.toString().replace("\t", "");
    }

    public static Obj getFromValue(Object val) {
        if (val instanceof String)
            return new Str((String) val);
        else if (val instanceof Double)
            return new Num((double) val, false);
        else if (val instanceof List) {
            List<Obj> lst = new ArrayList<>();
            List<Object> list = (List<Object>) val;

            for (Object item : list)
                lst.add(getFromValue(item));

            return new PList(lst);
        }
        else if (val instanceof Map) {
            Map<Obj, Obj> mp = new HashMap<>();
            Map<Object, Object> map = (Map<Object, Object>) val;

            for (Object key : map.keySet())
                mp.put(getFromValue(key), getFromValue(map.get(key)));

            return new Dict(mp);
        }
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