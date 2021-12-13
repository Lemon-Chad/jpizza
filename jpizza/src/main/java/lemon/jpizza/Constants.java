package lemon.jpizza;

import lemon.jpizza.contextuals.Context;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Constants {
    public static final char[] NUMBERS = "0123456789".toCharArray();
    public static final char[] NUMDOT = "0123456789._".toCharArray();
    public static final char[] LETTERS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static final char[] LETTERS_DIGITS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();
    public static final List<Tokens.TT> TYPETOKS = Arrays.asList(
            Tokens.TT.IDENTIFIER,
            Tokens.TT.KEYWORD,
            Tokens.TT.FLOAT,
            Tokens.TT.INT,
            Tokens.TT.LPAREN,
            Tokens.TT.RPAREN,
            Tokens.TT.LSQUARE,
            Tokens.TT.RSQUARE
    );
    public static final String[] KEYWORDS = {
            "free",
            "assert",
            "let",
            "throw",
            "struct",
            "do",
            "loop",
            "pass",
            "cal",
            "yourmom",
            "async",
            "import",
            "scope",
            "as",
            "extend",
            "bin",
            "function",
            "attr",
            "bake",
            "const",
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
            "mthd",
            "md",
            "ingredients",
            "recipe",
            "class",
            "obj",
            "switch",
            "case",
            "fn",
            "enum",
            "default",
            "match",
            "pub",
            "prv",
            "static",
            "stc"
    };
    @SuppressWarnings("unused") public static char BREAK = ';';
    @SuppressWarnings("unused") public static char[] IGNORE = new char[]{' ', '\n', '\t'};
    public static final Map<String, Context> LIBRARIES = new HashMap<>();
    public static final char splitter = '\n';

    public static final Map<String, String> STANDLIBS = new HashMap<>(){{
        put("std", """

enum pub Option {
    Some { val },
    None,
}

enum pub StaticOption {
    Box(T){ val: T },
    Empty(T),
}

class Iter {
    prv iterable: list;
    prv f;
    prv output;

    ingredients<iterable, f, output> {
        attr iterable => iterable;
        attr f => f;
        attr output => output;
    }

    mthd collect {
        let fin => for (x <- iterable) => f(x);
        output(..fin)
    }

    mthd stream {
        for (x <- iterable) f(x);
    }

    mthd bin type -> `Iter`;

}

class Array {
    prv internal: list;

    ingredients<..items>(T) {
        internal => [];
        if (T == "any") {
            internal => for (item <- items) => &item;
            return null;
        }

        let itype => None;
        for (item <- items) {
            itype => Some(type(item));
            if (itype::val != T)
                throw "Type", `Expected type ${T}, got ${itype::val}`;
            append(internal, &item);
        }
    }

    mthd bin string -> str(for (x <- internal) => str(x));

    mthd bin type -> `Array(${T})`;
    
    mthd bin eq<other> {
        if (type(other) != type(this)) return false;
        if (other::size() != this::size()) return false;
        
        list(this) == list(other)
    }
    
    mthd insert<item#T, index#num> = void {
        insert(internal, item, index);
    }

    mthd remove<item#T> = void {
        if (!contains(internal, item)) throw "Out of Bounds", "Item is not in Array";
        internal /= item;
    }

    mthd iter<func#function> = Iter {
        Iter(internal, func, !<..items> -> Array(..items)<any>)
    }

    mthd pop<index#num> = T {
        let item => *(internal[index]);
        internal /= item;
        return item;
    }

    mthd add<item#T> = void {
        append(internal, &item);
    }

    mthd size -> size(internal);

    mthd addAll<..items> = void {
        for (item <- items)
            add(item);
    }

    mthd bin list -> internal;

    mthd slice<min#num, max#num> {
        Array(..for (x <- sublist(internal, min, max)) => *x)<T>
    }

    mthd indexOf<item> = num {
        return indexOf(internal, &item);
    }

    mthd bin bracket<index> -> internal[index];

    mthd contains<x> = bool {
        return contains(internal, x);
    }

    mthd join<str#String> -> join(str, internal);

}

class Tuple {
    prv items: list;
    ingredients<..entry> {
        items => [];
        for (item <- entry)
            append(items, &item);
    }

    mthd size -> size(items);

    mthd bin bracket<other> -> items[other];

    mthd bin string -> `(${substr(str(items), 1, size(str(items)) - 1)})`;
    
    mthd bin eq<other> {
        if (type(other) != type(this)) return false;
        if (other::size() != this::size()) return false;
        
        list(this) == list(other)
    }
    
    mthd bin type {
        let str => '(';
        for (item <- items)
            str += type(item);
        str + ')'
    }
    
    mthd contains<x> -> contains(items, x);

    mthd bin list -> items;
}

class Map {
    prv internal: dict;

    ingredients<..pairs>(K, V) {
        internal => {};
        for (pair <- pairs) {
            if (type(pair) != "list" | size(pair) != 2)
                throw "Type", "Expected a key-value pair ([k, v])";
            elif (K != "any" & type(pair[0]) != K)
                throw "Type", `Expected key type to be ${K}, got ${type(pair[0])}`;
            elif (V != "any" & type(pair[1]) != V)
                throw "Type", `Expected key type to be ${V}, got ${type(pair[1])}`;
            
            set(internal, pair[0], &pair[1]);
        }
    }

    mthd iter<f#function> -> Iter(
        list(pairArray()),
        f,
        !<..array> {
            let new => Map()<any, any>;
            for (pair <- list(array))
                new::set(pair[0], pair[1]);
            new
        }
    );

    mthd bin string {
        let new => {};
        for (key <- list(internal))
            set(new, str(key), str(internal[key]));
        str(new)
    }

    mthd bin bracket<other#K> -> internal[other];

    mthd contains<other> -> contains(list(internal), other);

    mthd bin list -> list(internal);

    mthd keyArray -> Array(..list(internal));

    mthd get<other#K> {
        if (!this::contains(other)) throw "Out of Bounds", "Key not in map";
        internal[other]
    }

    mthd getOrDefault<other#K, def> {
        if (!this::contains(other)) return def;
        internal[other]
    }

    mthd size -> size(list(internal));
    
    mthd bin eq<other> {
        if (type(other) != type(this)) return false;
        
        dict(this) == dict(other)
    }
    
    mthd bin dictionary -> internal;

    mthd set<key#K, value#V> {
        set(internal, key, value);
    }

    mthd del<key#K> {
        if (!this::contains(key)) throw "Out of Bounds", "Key not in map";
        delete(internal, key);
    }

    mthd pairArray {
        let arr => Array()<Array(any)>;
        for (key <- list(internal)) {
            var a => Array()<any>;
            a::addAll(key, internal[key]);
            arr::add(a);
        }
        arr
    }
}

        """);
        put("socks", """
import sockets as _sockets;

class SocketConnection {
	prv id: num;
	ingredients<cID#num> {
		id => cID;
	}

	mthd send<msg> = void -> _sockets::serverSend(id, msg);
	mthd sendBytes<msg#bytearray> = void -> _sockets::serverSendBytes(id, msg);

	mthd recv = any -> _sockets::serverRecv(id);
	mthd recvBytes<length#num> = bytearray -> _sockets::serverRecvBytes(id, length);
	mthd recvAllBytes = bytearray -> _sockets::serverRecvAllBytes(id);

	mthd close = void -> _sockets::closeServerConnection(id);
}

class Socket {
	prv id: num;
	ingredients<port#num> {
		id => _sockets::newServer(port);
	}

	mthd listen = SocketConnection -> SocketConnection(_sockets::connect(id));

	mthd close = void -> _sockets::closeServer(id);
}

class SocketClient {
	prv id: num;
	ingredients<host#String, port#num> {
		id => _sockets::newClient(host, port);
	}

	mthd send<msg> = void -> _sockets::clientSend(id, msg);
	mthd sendBytes<msg#bytearray> = void -> _sockets::clientSendBytes(id, msg);

	mthd recv = any -> _sockets::clientRecv(id);
	mthd recvBytes<length#num> = bytearray -> _sockets::clientRecvBytes(id, length);
	mthd recvAllBytes = bytearray -> _sockets::clientRecvAllBytes(id);

	mthd close = void -> _sockets::clientClose(id);
}
""");
    }};

    public static int indexToLine(String code, int index) {
        System.out.println(index);
        return code.substring(0, index).split("\n").length - 1;
    }

    public static int leftPadding(String str) {
        int tabs = 0;
        while (tabs < str.length() && Character.isWhitespace(str.charAt(tabs))) {
            tabs++;
        }
        return tabs;
    }

    public static String highlightFlat(String source, int index, int len) {
        StringBuilder sb = new StringBuilder();

        String[] lines = source.split("\n");

        int line = 0;
        while (index >= lines[line].length() && line < lines.length - 1) {
            index -= lines[line].length() + 1;
            line++;
        }

        int tabs = leftPadding(lines[line]);
        index -= tabs;
        len += tabs;

        while (len > 0 && line < lines.length) {
            String lineStr = lines[line];
            int lineLen = lineStr.length();

            tabs = leftPadding(lineStr);
            len -= tabs;
            lineLen -= tabs;
            String text = lineStr.substring(tabs);

            if (lineLen == 0) {
                sb.append("\n");
                line++;
                continue;
            }

            int end = Math.min(index + len, lineLen);
            String highlight;
            if (end - index >= 2) {
                if (Shell.fileEncoding.equals("UTF-8"))
                    highlight = "╰" + "─".repeat(end - index - 2) + "╯";
                else
                    highlight = "\\" + "_".repeat(end - index - 2) + "/";
            }
            else {
                highlight = "^";
            }

            sb.append(text)
              .append("\n")
              .append(" ".repeat(index))
              .append(highlight)
              .append("\n");

            len -= lineLen - index;

            line++;
            index = 0;
        }

        return sb.toString();
    }

    public static final Map<Tokens.TT, Operations.OP> tto = new HashMap<>(){{
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

    public static final Map<String, JPType> methTypes = new HashMap<>(){{
        put("eq", JPType.Boolean);
        put("lt", JPType.Boolean);
        put("lte", JPType.Boolean);
        put("ne", JPType.Boolean);
        put("also", JPType.Boolean);
        put("including", JPType.Boolean);

        put("type", JPType.String);
    }};

    public enum JPType {
        Ref,
        Deref,
        Pattern,
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
        Extend,
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
        Res,
        Assert,
        Spread,
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

    public static String stringWithArrows(String text, @NotNull Position pos_start, @NotNull Position pos_end) {
        StringBuilder result = new StringBuilder();

        int idxStart = Math.max(0, text.lastIndexOf(splitter, pos_start.tidx));
        int idxEnd = text.indexOf(splitter, idxStart + 1);

        if (idxEnd < 0) idxEnd = text.length();

        int line_count = pos_end.ln - pos_start.ln + 1;
        int offs = 0;
        int colStart, colEnd, dist;
        for (int i = 0; i < line_count; i++) {
            String line = text.substring(idxStart, idxEnd);

            colStart = i == 0 ? pos_start.tcol : nonWhitespace(line);
            colEnd = i == line_count - 1 ? pos_end.tcol : line.length() - 1;
            dist = colEnd - colStart;

            String grouping;
            if (dist >= 2) {
                if (Shell.fileEncoding.equals("UTF-8"))
                    grouping = "╰" + "─".repeat(dist - 2) + "╯";
                else
                    grouping = "\\" + "_".repeat(dist - 2) + "/";
            }
            else {
                grouping = "^";
            }

            result.append(line).append("\n")
                    .append(" ".repeat(Math.max(0, colStart + offs))).append(grouping);

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
        return switch (obj.jptype) {
            case Dict ->{
                Map<Object, Object> objMap = new ConcurrentHashMap<>();
                ConcurrentHashMap<Obj, Obj> deMap = obj.map;

                for (Obj k : deMap.keySet())
                    objMap.put(toObject(k), toObject(deMap.get(k)));

                yield objMap;
            }

            case List ->{
                List<Object> objLst = new ArrayList<>();
                List<Obj> olst = new ArrayList<>(obj.list);

                for (int i = 0; i < olst.size(); i++)
                    objLst.add(toObject(olst.get(i)));

                yield objLst;
            }

            case Number -> obj.number;

            case String -> obj.string;

            case Boolean -> obj.boolval;

            case Bytes -> obj.arr;

            case Generic -> obj.value;
            default -> null;
        };
    }

}
