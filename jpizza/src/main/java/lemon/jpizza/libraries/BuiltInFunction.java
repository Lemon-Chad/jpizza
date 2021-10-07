package lemon.jpizza.libraries;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.Pair;
import lemon.jpizza.errors.Error;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.executables.ClassInstance;
import lemon.jpizza.objects.executables.Function;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Shell;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class BuiltInFunction extends Library {
    private final static Scanner scanner = new Scanner(System.in);
    private final Random random = new Random();
    private final Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public static void initialize() {
        BuiltInFunction.initialize("compiled", BuiltInFunction.class, new HashMap<>(){{
            put("setIndex", Arrays.asList("list", "item", "index"));
            put("insert", Arrays.asList("list", "item", "index"));
            put("substr", Arrays.asList("str", "start", "end"));
            put("sublist", Arrays.asList("list", "start", "end"));
            put("set", Arrays.asList("dict", "key", "value"));
            put("preprocess", Arrays.asList("processed", "preprocessor"));
            put("postprocess", Arrays.asList("processed", "postprocessor"));
            put("arctan2", Arrays.asList("a", "b"));
            put("join", Arrays.asList("string", "list"));
            put("getattr", Arrays.asList("instance", "value"));
            put("hasattr", Arrays.asList("instance", "value"));
            put("get", Arrays.asList("dict", "value"));
            put("delete", Arrays.asList("dict", "value"));
            put("foreach", Arrays.asList("list", "func"));
            put("append", Arrays.asList("list", "value"));
            put("remove", Arrays.asList("list", "value"));
            put("pop", Arrays.asList("list", "value"));
            put("extend", Arrays.asList("listA", "listB"));
            put("contains", Arrays.asList("list", "value"));
            put("randint", Arrays.asList("min", "max"));
            put("min", Arrays.asList("a", "b"));
            put("max", Arrays.asList("a", "b"));
            put("split", Arrays.asList("value", "splitter"));
            put("enumProps", Arrays.asList("prop", "enumChild"));
            put("log", Arrays.asList("value", "base"));
            put("println", Collections.singletonList("value"));
            put("print", Collections.singletonList("value"));
            put("printback", Collections.singletonList("value"));
            put("type", Collections.singletonList("value"));
            put("value", Collections.singletonList("value"));
            put("sim", Collections.singletonList("value"));
            put("escape", Collections.singletonList("value"));
            put("unescape", Collections.singletonList("value"));
            put("round", Collections.singletonList("value"));
            put("floor", Collections.singletonList("value"));
            put("ceil", Collections.singletonList("value"));
            put("abs", Collections.singletonList("value"));
            put("run", Collections.singletonList("fn"));
            put("size", Collections.singletonList("value"));
            put("str", Collections.singletonList("value"));
            put("list", Collections.singletonList("value"));
            put("ok", Collections.singletonList("res"));
            put("fail", Collections.singletonList("res"));
            put("catch", Collections.singletonList("res"));
            put("resolve", Collections.singletonList("res"));
            put("bool", Collections.singletonList("value"));
            put("num", Collections.singletonList("value"));
            put("dict", Collections.singletonList("value"));
            put("func", Collections.singletonList("value"));
            put("isList", Collections.singletonList("value"));
            put("isFunction", Collections.singletonList("value"));
            put("isBoolean", Collections.singletonList("value"));
            put("isDict", Collections.singletonList("value"));
            put("isNull", Collections.singletonList("value"));
            put("isNumber", Collections.singletonList("value"));
            put("isString", Collections.singletonList("value"));
            put("field", Collections.singletonList("value"));
            put("nfield", Collections.singletonList("value"));
            put("choose", Collections.singletonList("value"));
            put("byter", Collections.singletonList("value"));
            put("floating", Collections.singletonList("value"));
            put("strUpper", Collections.singletonList("value"));
            put("strLower", Collections.singletonList("value"));
            put("strShift", Collections.singletonList("value"));
            put("strUnshift", Collections.singletonList("value"));
            put("sin", Collections.singletonList("a"));
            put("cos", Collections.singletonList("a"));
            put("tan", Collections.singletonList("a"));
            put("arcsin", Collections.singletonList("a"));
            put("arccos", Collections.singletonList("a"));
            put("arctan", Collections.singletonList("a"));
            put("random", new ArrayList<>());
            put("clear", new ArrayList<>());
            put("createDennis", new ArrayList<>());
            put("pi", new ArrayList<>());
            put("euler", new ArrayList<>());

        }},
                Shell.globalSymbolTable);
    }

    static final HashMap<String, String> upper = new HashMap<>(){{
        put("1", "!");
        put("2", "@");
        put("3", "#");
        put("4", "$");
        put("5", "%");
        put("6", "^");
        put("7", "&");
        put("8", "*");
        put("9", "(");
        put("0", ")");

        put("`", "~");

        put("'", "\"");
        put(";", ":");

        put("/", "?");
        put(".", ">");
        put(",", "<");

        put("[", "{");
        put("]", "}");
        put("\\", "|");

        put("-", "_");
        put("=", "+");
    }};
    static final HashMap<String, String> lower = new HashMap<>(){{
        for (String k : upper.keySet())
            put(upper.get(k), k);
    }};

    public BuiltInFunction(String name) { super(name, "compiled"); }

    public RTResult execute_escape(Context execCtx) {
        return new RTResult().success(
                new Str(StringEscapeUtils.unescapeJava(execCtx.symbolTable.get("value").toString()))
        );
    }

    public RTResult execute_unescape(Context execCtx) {
        return new RTResult().success(
                new Str(StringEscapeUtils.escapeJava(execCtx.symbolTable.get("value").toString()))
        );
    }

    public RTResult execute_preprocess(Context execCtx) {
        RTResult res = new RTResult();
        Obj par = res.register(checkType(execCtx.symbolTable.get("processed"),
                "function", Constants.JPType.Function));
        Obj proc = res.register(checkType(execCtx.symbolTable.get("preprocessor"),
                "function", Constants.JPType.Function));
        if (res.error != null) return res;
        return res.success(((Function) par).addPreProcessor((Function) proc));
    }

    public RTResult execute_postprocess(Context execCtx) {
        RTResult res = new RTResult();
        Obj par = res.register(checkType(execCtx.symbolTable.get("processed"),
                "function", Constants.JPType.Function));
        Obj proc = res.register(checkType(execCtx.symbolTable.get("postprocessor"),
                "function", Constants.JPType.Function));
        if (res.error != null) return res;
        return res.success(((Function) par).addPostProcessor((Function) proc));
    }

    public RTResult execute_fail(Context execCtx) {
        Obj r = (Obj) execCtx.symbolTable.get("res");
        if (r.jptype != Constants.JPType.Res) return new RTResult().success(new Null());
        Result res = (Result) r;
        if (res.ok())
            return new RTResult().success(new Null());
        return new RTResult().failure(RTError.Released(
                r.get_start(), r.get_end(),
                res.fail().toString(),
                execCtx
        ));
    }

    public RTResult execute__version_(Context execCtx) {
        return new RTResult().success(new Str("v1.1.1"));
    }

    public RTResult execute_pi(Context execCtx) {
        return new RTResult().success(new Num(Math.PI));
    }

    public RTResult execute_euler(Context execCtx) {
        return new RTResult().success(new Num(Math.E));
    }

    public RTResult execute_log(Context execCtx) {
        RTResult res = new RTResult();
        Obj v = res.register(checkType(execCtx.symbolTable.get("value"), "number", Constants.JPType.Number));
        Obj b = res.register(checkType(execCtx.symbolTable.get("base"), "number", Constants.JPType.Number));
        if (res.error != null) return res;

        double value = v.number;
        double base = b.number;

        return res.success(new Num(Math.log(value) / Math.log(base)));
    }

    public RTResult execute_catch(Context execCtx) {
        Obj r = (Obj) execCtx.symbolTable.get("res");
        if (r.jptype != Constants.JPType.Res)
            return new RTResult().failure(RTError.Type(
                    r.get_start(), r.get_end(),
                    "Expected catcher type",
                    execCtx
            ));
        Result res = (Result) r;
        return new RTResult().success(res.ok() ? new Null() : res.fail());
    }

    public RTResult execute_ok(Context execCtx) {
        Obj r = (Obj) execCtx.symbolTable.get("res");
        if (r.jptype != Constants.JPType.Res)
            return new RTResult().failure(RTError.Type(
                    r.get_start(), r.get_end(),
                    "Expected catcher type",
                    execCtx
            ));
        Result res = (Result) r;
        return new RTResult().success(new Bool(res.ok()));
    }

    public RTResult execute_resolve(Context execCtx) {
        Obj r = (Obj) execCtx.symbolTable.get("res");
        if (r.jptype != Constants.JPType.Res)
            return new RTResult().failure(RTError.Type(
                    r.get_start(), r.get_end(),
                    "Expected catcher type",
                    execCtx
            ));
        Result res = (Result) r;
        if (!res.ok())
            return new RTResult().failure(RTError.Unresolved(
                    r.get_start(), r.get_end(),
                    "Unresolved error in catcher",
                    execCtx
            ));
        return new RTResult().success(res.resolve());
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_getattr(Context execCtx) {
        Obj o = ((Obj) execCtx.symbolTable.get("instance"));
        if (o.jptype != Constants.JPType.ClassInstance) return new RTResult().failure(RTError.Type(
                o.pos_start, o.pos_end,
                "Expected class instance",
                context
        ));
        Obj acc = ((Obj) execCtx.symbolTable.get("value"));
        if (acc.jptype != Constants.JPType.String) return new RTResult().failure(RTError.Type(
                acc.pos_start, acc.pos_end,
                "Expected String",
                context
        ));
        var val = ((ClassInstance) o)._access(acc);
        if (val instanceof String)
            return Interpreter.getThis(val, execCtx, o.pos_start, acc.pos_end);
        else if (val instanceof RTError) return new RTResult().failure((RTError) val);
        return new RTResult().success(((Obj)val).set_context(((ClassInstance)o).value));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_hasattr(Context execCtx) {
        Obj o = ((Obj) execCtx.symbolTable.get("instance"));
        if (o.jptype != Constants.JPType.ClassInstance) return new RTResult().failure(RTError.Type(
                o.pos_start, o.pos_end,
                "Expected class instance",
                context
        ));
        Obj acc = ((Obj) execCtx.symbolTable.get("value"));
        if (acc.jptype != Constants.JPType.String) return new RTResult().failure(RTError.Type(
                acc.pos_start, acc.pos_end,
                "Expected String",
                context
        ));
        var val = ((ClassInstance) o)._access(acc);
        return new RTResult().success(new Bool(!(val instanceof RTError)));
    }

    public RTResult execute_byter(Context execCtx) {
        Obj bytelist = ((Obj) execCtx.symbolTable.get("value")).alist();
        if (bytelist.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                bytelist.get_start(), bytelist.get_end(),
                "Expected list",
                execCtx
        ));
        List<Obj> prelst = bytelist.list;
        byte[] bytes = new byte[prelst.size()];
        for (int i = 0; i < prelst.size(); i++) {
            Obj n = prelst.get(i).number();
            if (n.jptype != Constants.JPType.Number) return new RTResult().failure(RTError.Type(
                    bytelist.get_start(), bytelist.get_end(),
                    "Expected byte",
                    execCtx
            ));
            double val = n.number;
            if ((byte) val != val) return new RTResult().failure(RTError.Type(
                    bytelist.get_start(), bytelist.get_end(),
                    "Byte out of range",
                    execCtx
            ));
            bytes[i] = (byte) val;
        }
        return new RTResult().success(new Bytes(bytes));
    }

    public RTResult execute_println(Context execCtx) {
        Shell.logger.outln(execCtx.symbolTable.get("value"));
        return new RTResult().success(new Null());
    }

    public RTResult execute_createDennis(Context execCtx) {
        return new RTResult().success(new EnumJ("dennis", new HashMap<>(){{
            String[] members = new String[]{"Ocean", "Frozen", "Icey", "Yeetus", "Triangle", "Clown", "Lake"};
            for (int i = 0; i < members.length; i++)
                put(members[i], new EnumJChild(i, new ArrayList<>(), new ArrayList<>()));
        }}));
    }

    public RTResult execute_replace(Context execCtx) {
        RTResult res = new RTResult();
        Obj s = res.register(checkType(execCtx.symbolTable.get("str"), "String", Constants.JPType.String));
        Obj o = res.register(checkType(execCtx.symbolTable.get("old"), "String", Constants.JPType.String));
        Obj n = res.register(checkType(execCtx.symbolTable.get("new"), "String", Constants.JPType.String));
        if (res.error != null) return res;
        return res.success(new Str(s.toString().replace(o.toString(), n.toString())));
    }

    public RTResult execute_enumProps(Context execCtx) {
        Obj p = (Obj) execCtx.symbolTable.get("prop");
        Obj ec = (Obj) execCtx.symbolTable.get("enumChild");
        if (p.jptype != Constants.JPType.ClassInstance) return new RTResult().failure(RTError.Type(
                p.get_start(), p.get_end(),
                "Expected prop",
                execCtx
        ));
        if (ec.jptype != Constants.JPType.EnumChild) return new RTResult().failure(RTError.Type(
                p.get_start(), p.get_end(),
                "Expected enum child",
                execCtx
        ));
        EnumJChild enumChild = (EnumJChild) ec;
        ClassInstance prop = (ClassInstance) p;
        return new RTResult().success(new Bool(
            enumChild.parent.name.equals(prop.access(new Str("$parent"))) &&
                    enumChild.val == (int) prop.access(new Str("$child"))
        ));
    }

    public RTResult execute_print(Context execCtx) {
        Shell.logger.out(execCtx.symbolTable.get("value"));
        return new RTResult().success(new Null());
    }

    public RTResult execute_type(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).type());
    }

    public RTResult execute_floating(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (num.jptype != Constants.JPType.Number) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Bool(num.floating));
    }

    public RTResult execute_random(Context _execCtx) {
        return new RTResult().success(new Num(Math.random()));
    }

    public RTResult execute_clear(Context _execCtx) {
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c",
                        "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException ignored) {}
        return new RTResult().success(new Null());
    }

    public RTResult execute_round(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (num.jptype != Constants.JPType.Number) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Num(Math.round(
                num.number
        )));
    }

    public RTResult execute_foreach(Context execCtx) {
        RTResult res = new RTResult();
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj func = ((Obj) execCtx.symbolTable.get("func")).function();
        List<Obj> newList = new ArrayList<>();
        List<Obj> preList = (List<Obj>) list.value;
        int size = preList.size();
        for (int i = 0; i < size; i++) {
            Obj after = res.register(func.execute(Collections.singletonList(preList.get(i)), new ArrayList<>(), new HashMap<>(),
                    new Interpreter()));
            if (res.error != null) return res;
            newList.add(after);
        }
        return res.success(new PList(newList));
    }

    public RTResult execute_floor(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (num.jptype != Constants.JPType.Number) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Num(Math.floor(
                num.number
        )));
    }

    public RTResult execute_ceil(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (num.jptype != Constants.JPType.Number) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Num(Math.ceil(
                num.number
        )));
    }

    public RTResult execute_abs(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (num.jptype != Constants.JPType.Number) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Num(Math.abs(
                num.number
        )));
    }

    public RTResult execute_run(Context execCtx) {
        RTResult res = new RTResult();
        Obj fln = (Obj) execCtx.symbolTable.get("fn");
        if (fln.jptype != Constants.JPType.String) return res.failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a string",
                execCtx
        ));

        String fn = fln.string;
        Path path = Path.of(fn);
        File s = new File(String.valueOf(path));
        if (!s.exists()) return res.failure(RTError.FileNotFound(
                pos_start, pos_end,
                "File does not exist in " + Paths.get("").toAbsolutePath(),
                execCtx
        )); String script;
        try {
            script = Files.readString(path);
        } catch (IOException e) {
            return res.failure(RTError.Internal(
                    pos_start, pos_end,
                    "IOException reading file",
                    execCtx
            ));
        }
        Pair<Obj, Error> runtime = Shell.run(fn, script, false);
        if (runtime.b != null) return res.failure(new RTError(
                runtime.b.error_name,
                pos_start, pos_end,
                String.format("Failed to finish executing script \"%s\"%n%s", fn, runtime.b.asString()),
                execCtx
        ));
        Shell.logger.outln(runtime.a);
        return res.success(runtime.a != null ? runtime.a : new Null());
    }

    public RTResult execute_size(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("value")).alist();
        if (list.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a list",
                execCtx
        ));
        return new RTResult().success(new Num(list.list.size()));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_insert(Context execCtx) {
        Obj index = ((Obj) execCtx.symbolTable.get("index")).number();
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj item = ((Obj) execCtx.symbolTable.get("item"));
        RTResult e = isInt(index, execCtx);
        if (e.error != null) return e;
        if (list.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a list",
                execCtx
        ));
        if (index.number > list.list.size() || index.number < 0) return new RTResult().failure(RTError.OutOfBounds(
                pos_start, pos_end,
                "Index is out of bounds",
                execCtx
        ));
        list.list.add(Math.toIntExact(Math.round(index.number)), item);
        return new RTResult().success(list);
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_setIndex(Context execCtx) {
        Obj index = ((Obj) execCtx.symbolTable.get("index")).number();
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj item = ((Obj) execCtx.symbolTable.get("item"));
        RTResult e = isInt(index, execCtx);
        if (e.error != null) return e;
        if (list.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a list",
                execCtx
        ));
        if (index.number >= list.list.size() || index.number < 0) return new RTResult().failure(RTError.OutOfBounds(
                pos_start, pos_end,
                "Index is out of bounds",
                execCtx
        ));
        list.list.set(index.number.intValue(), item);
        return new RTResult().success(list);
    }

    public RTResult execute_substr(Context execCtx) {
        RTResult res = new RTResult();
        String val = execCtx.symbolTable.get("str").toString();

        Object _start = execCtx.symbolTable.get("start");
        Object _end = execCtx.symbolTable.get("end");

        Obj start = res.register(checkPosInt(_start));
        Obj end = res.register(checkPosInt(_end));

        if (res.error != null) return res;

        int strt = start.number.intValue();
        int nd = end.number.intValue();

        if (strt > val.length() || strt < 0) return new RTResult().failure(RTError.OutOfBounds(
                pos_start, pos_end,
                "Start is out of bounds",
                execCtx
        ));
        if (nd > val.length() || nd < 0) return new RTResult().failure(RTError.OutOfBounds(
                pos_start, pos_end,
                "End is out of bounds",
                execCtx
        ));

        return res.success(new Str(val.substring(strt, nd)));
    }
    public RTResult execute_sublist(Context execCtx) {
        RTResult res = new RTResult();

        Object _start = execCtx.symbolTable.get("start");
        Object _end = execCtx.symbolTable.get("end");
        Object _val = execCtx.symbolTable.get("list");

        Obj start = res.register(checkPosInt(_start));
        Obj end = res.register(checkPosInt(_end));
        Obj val = res.register(checkType(_val, "list", Constants.JPType.List));

        if (res.error != null) return res;

        int strt = start.number.intValue();
        int nd = end.number.intValue();
        List<Obj> lst = val.list;

        if (strt > lst.size() || strt < 0) return new RTResult().failure(RTError.OutOfBounds(
                pos_start, pos_end,
                "Start is out of bounds",
                execCtx
        ));
        if (nd > lst.size() || nd < 0) return new RTResult().failure(RTError.OutOfBounds(
                pos_start, pos_end,
                "End is out of bounds",
                execCtx
        ));

        return res.success(new PList(new ArrayList<>(lst.subList(strt, nd))));
    }

    public RTResult execute_split(Context execCtx) {
        Obj string = ((Obj) execCtx.symbolTable.get("value")).astring();
        Obj splitter = ((Obj) execCtx.symbolTable.get("splitter")).astring();
        if (string.jptype != Constants.JPType.String || splitter.jptype != Constants.JPType.String) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a string",
                execCtx
        ));
        String str = string.string;
        String split = splitter.string;
        String[] pieces = str.split(split);
        List<Obj> fragments = new ArrayList<>();
        int length = pieces.length;
        for (int i = 0; i < length; i++) {
            fragments.add(new Str(pieces[i]));
        } return new RTResult().success(new PList(fragments));
    }
    public RTResult execute_strUpper(Context execCtx) {
        String string = execCtx.symbolTable.get("value").toString();
        return new RTResult().success(new Str(string.toUpperCase()));
    }
    public RTResult execute_strLower(Context execCtx) {
        String string = execCtx.symbolTable.get("value").toString();
        return new RTResult().success(new Str(string.toLowerCase()));
    }
    public RTResult execute_strShift(Context execCtx) {
        String string = execCtx.symbolTable.get("value").toString();
        StringBuilder sb = new StringBuilder();
        for (char c : string.toCharArray()) {
            String s = String.valueOf(c);
            if (upper.containsKey(s))
                sb.append(upper.get(s));
            else
                sb.append(s.toUpperCase());
        }
        return new RTResult().success(new Str(sb.toString()));
    }
    public RTResult execute_strUnshift(Context execCtx) {
        String string = execCtx.symbolTable.get("value").toString();
        StringBuilder sb = new StringBuilder();
        for (char c : string.toCharArray()) {
            String s = String.valueOf(c);
            if (lower.containsKey(s))
                sb.append(lower.get(s));
            else
                sb.append(s.toLowerCase());
        }
        return new RTResult().success(new Str(sb.toString()));
    }
    public RTResult execute_join(Context execCtx) {
        RTResult res = new RTResult();

        StringBuilder sb = new StringBuilder();
        String string = execCtx.symbolTable.get("string").toString();

        Obj val = res.register(checkType(execCtx.symbolTable.get("list"), "list", Constants.JPType.List));
        if (res.error != null) return res;
        List<Obj> it = val.list;

        if (it.size() > 0) {
            sb.append(it.get(0));
            for (int i = 1; i < it.size(); i++)
                sb.append(string).append(it.get(i));
        }

        return new RTResult().success(new Str(sb.toString()));
    }

    public RTResult execute_str(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).astring());
    }
    public RTResult execute_list(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).alist());
    }
    public RTResult execute_bool(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).bool());
    }
    public RTResult execute_num(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).number());
    }
    public RTResult execute_dict(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).dictionary());
    }
    public RTResult execute_func(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).function());
    }

    public RTResult execute_isNumber(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Num));
    }
    public RTResult execute_isList(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof PList));
    }
    public RTResult execute_isDict(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Dict));
    }
    public RTResult execute_isString(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Str));
    }
    public RTResult execute_isBoolean(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Bool));
    }
    public RTResult execute_isFunction(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Function));
    }
    public RTResult execute_isNull(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Null));
    }

    public RTResult execute_printback(Context execCtx) {
        Obj obj = ((Obj) execCtx.symbolTable.get("value")).astring();
        Shell.logger.outln(obj);
        return new RTResult().success(new Str(obj.toString()));
    }

    public RTResult execute_field(Context execCtx) {
        Shell.logger.out(execCtx.symbolTable.get("value"));
        String text = scanner.nextLine();
        return new RTResult().success(new Str(text));
    }

    public RTResult execute_sim(Context execCtx) {
        Obj in = ((Obj) execCtx.symbolTable.get("value")).astring();
        if (in.jptype != Constants.JPType.String) return new RTResult().failure(RTError.Type(
                in.pos_start, in.pos_end,
                "Expected string input",
                execCtx
        ));
        var out = Shell.run("<sim>", in.string, false);
        if (out.b != null) return new RTResult().failure(out.b);
        return new RTResult().success(out.a);
    }

    public RTResult execute_nfield(Context execCtx) {
        Shell.logger.out(execCtx.symbolTable.get("value"));
        String text;
        do {
            text = scanner.nextLine();
        } while (!pattern.matcher(text).matches());
        return new RTResult().success(new Num(Double.parseDouble(text)));
    }

    public RTResult execute_choose(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("value")).alist();
        if (list.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a list",
                context
        ));
        List<Obj> value = list.list;
        if (value.size() == 0) return new RTResult().success(new Null());
        return new RTResult().success(value.get(random.nextInt(value.size())));
    }

    private RTResult isInt(Obj num, Context execCtx) {
        if (num.jptype != Constants.JPType.Number) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        if (num.floating) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a long",
                execCtx
        ));
        return new RTResult().success(new Null());
    }

    public RTResult execute_randint(Context execCtx) {
        RTResult res = new RTResult();
        Obj min = ((Obj) execCtx.symbolTable.get("min")).number();
        Obj max = ((Obj) execCtx.symbolTable.get("max")).number();
        res.register(isInt(min, execCtx));
        if (res.error != null) return res;
        res.register(isInt(max, execCtx));
        if (res.error != null) return res;
        int iMin = Math.toIntExact(Math.round(min.number));
        int iMax = Math.toIntExact(Math.round(max.number));
        double num = random.nextInt(iMax - iMin + 1) + iMin;
        return new RTResult().success(new Num(num));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_min(Context execCtx) {
        RTResult res = new RTResult();

        Obj a = ((Obj) execCtx.symbolTable.get("a")).number();
        Obj b = ((Obj) execCtx.symbolTable.get("b")).number();

        res.register(checkType(a, "number", Constants.JPType.Number));
        res.register(checkType(b, "number", Constants.JPType.Number));
        if (res.error != null) return res;

        double x = a.number;
        double y = b.number;

        double num = Math.min(x, y);
        return new RTResult().success(new Num(num));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_max(Context execCtx) {
        RTResult res = new RTResult();

        Obj a = ((Obj) execCtx.symbolTable.get("a")).number();
        Obj b = ((Obj) execCtx.symbolTable.get("b")).number();

        res.register(checkType(a, "number", Constants.JPType.Number));
        res.register(checkType(b, "number", Constants.JPType.Number));
        if (res.error != null) return res;

        double x = a.number;
        double y = b.number;

        double num = Math.max(x, y);
        return new RTResult().success(new Num(num));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_sin(Context execCtx) {
        RTResult res = new RTResult();

        Obj a = ((Obj) execCtx.symbolTable.get("a")).number();

        res.register(checkType(a, "number", Constants.JPType.Number));
        if (res.error != null) return res;

        double x = a.number;

        double num = Math.sin(x);
        return new RTResult().success(new Num(num));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_cos(Context execCtx) {
        RTResult res = new RTResult();

        Obj a = ((Obj) execCtx.symbolTable.get("a")).number();

        res.register(checkType(a, "number", Constants.JPType.Number));
        if (res.error != null) return res;

        double x = a.number;

        double num = Math.cos(x);
        return new RTResult().success(new Num(num));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_tan(Context execCtx) {
        RTResult res = new RTResult();

        Obj a = ((Obj) execCtx.symbolTable.get("a")).number();

        res.register(checkType(a, "number", Constants.JPType.Number));
        if (res.error != null) return res;

        double x = a.number;

        double num = Math.tan(x);
        return new RTResult().success(new Num(num));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_arcsin(Context execCtx) {
        RTResult res = new RTResult();

        Obj a = ((Obj) execCtx.symbolTable.get("a")).number();

        res.register(checkType(a, "number", Constants.JPType.Number));
        if (res.error != null) return res;

        double x = a.number;

        double num = Math.asin(x);
        return new RTResult().success(new Num(num));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_arccos(Context execCtx) {
        RTResult res = new RTResult();

        Obj a = ((Obj) execCtx.symbolTable.get("a")).number();

        res.register(checkType(a, "number", Constants.JPType.Number));
        if (res.error != null) return res;

        double x = a.number;

        double num = Math.acos(x);
        return new RTResult().success(new Num(num));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_arctan(Context execCtx) {
        RTResult res = new RTResult();

        Obj a = ((Obj) execCtx.symbolTable.get("a")).number();

        res.register(checkType(a, "number", Constants.JPType.Number));
        if (res.error != null) return res;

        double x = a.number;

        double num = Math.atan(x);
        return new RTResult().success(new Num(num));
    }

    public RTResult execute_arctan2(Context execCtx) {
        RTResult res = new RTResult();

        Obj a = ((Obj) execCtx.symbolTable.get("a")).number();
        Obj b = ((Obj) execCtx.symbolTable.get("b")).number();

        res.register(isInt(a, execCtx));
        res.register(isInt(b, execCtx));
        if (res.error != null) return res;

        double x = a.number;
        double y = b.number;

        double num = Math.atan2(y, x);
        return new RTResult().success(new Num(num));
    }

    public RTResult execute_append(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj value = (Obj) execCtx.symbolTable.get("value");
        if (list.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a list",
                execCtx
        ));
        Pair<Obj, RTError> result = list.append(value);
        if (result.b != null) return new RTResult().failure(result.b);
        return new RTResult().success(result.a);
    }

    public RTResult execute_remove(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj value = ((Obj) execCtx.symbolTable.get("value"));
        if (list.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a list",
                execCtx
        ));
        Pair<Obj, RTError> result = list.remove(value);
        if (result.b != null) return new RTResult().failure(result.b);
        return new RTResult().success(result.a);
    }

    public RTResult execute_contains(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj value = ((Obj) execCtx.symbolTable.get("value"));
        if (list.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a list",
                execCtx
        ));
        return new RTResult().success(new Bool(list.list.contains(value)));
    }

    public RTResult execute_pop(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj value = ((Obj) execCtx.symbolTable.get("value")).number();
        if (list.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a list",
                execCtx
        ));
        RTResult e = isInt(value, execCtx);
        if (e.error != null) return e;
        Pair<Obj, RTError> result = list.pop(value);
        if (result.b != null) return new RTResult().failure(result.b);
        return new RTResult().success(result.a);
    }

    public RTResult execute_extend(Context execCtx) {
        Obj listA = ((Obj) execCtx.symbolTable.get("listA")).alist();
        Obj listB = ((Obj) execCtx.symbolTable.get("listB")).alist();
        if (listA.jptype != Constants.JPType.List || listB.jptype != Constants.JPType.List) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a list",
                execCtx
        ));
        List<Obj> listAval = listA.list;
        listAval.addAll(listB.list);
        return new RTResult().success(new PList(listAval));
    }

    public RTResult keyInDict(Context execCtx, Obj dict) {
        Obj value = ((Obj) execCtx.symbolTable.get("value"));
        if (dict.jptype != Constants.JPType.Dict) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a dict",
                execCtx
        ));
        if (!dict.map.containsKey(value)) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Key not in dict",
                execCtx
        ));
        return new RTResult().success(value);
    }

    public RTResult execute_get(Context execCtx) {
        RTResult res = new RTResult();
        Obj dict = ((Obj) execCtx.symbolTable.get("dict")).dictionary();
        Obj value = res.register(keyInDict(execCtx, dict));
        if (res.error != null) return res;
        Obj v = dict.map.getOrDefault(value, new Null());
        return res.success(v);
    }

    public RTResult execute_delete(Context execCtx) {
        RTResult res = new RTResult();
        Obj dict = ((Obj) execCtx.symbolTable.get("dict")).dictionary();
        Obj value = res.register(keyInDict(execCtx, dict));
        return res.success(dict.map.remove(value));
    }

    public RTResult execute_set(Context execCtx) {
        Obj dict = ((Obj) execCtx.symbolTable.get("dict")).dictionary();
        Obj key = ((Obj) execCtx.symbolTable.get("key"));
        Obj value = ((Obj) execCtx.symbolTable.get("value"));
        if (dict.jptype != Constants.JPType.Dict) return new RTResult().failure(RTError.Type(
                pos_start, pos_end,
                "Argument must be a dictionary",
                execCtx
        ));
        return new RTResult().success(((Dict) dict).set(key, value));
    }

}
