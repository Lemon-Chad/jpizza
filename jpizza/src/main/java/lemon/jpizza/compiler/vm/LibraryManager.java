package lemon.jpizza.compiler.vm;

import lemon.jpizza.Pair;
import lemon.jpizza.Shell;
import lemon.jpizza.compiler.libraries.*;
import lemon.jpizza.compiler.libraries.awt.AbstractWindowToolkit;
import lemon.jpizza.compiler.libraries.pretzel.Pretzel;
import lemon.jpizza.compiler.libraries.puddle.PDL;
import lemon.jpizza.compiler.types.GenericType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.objects.FuncType;
import lemon.jpizza.compiler.types.objects.TupleType;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.errors.Error;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static lemon.jpizza.Constants.readString;

public class LibraryManager {
    VM vm;

    static final HashMap<String, String> SHIFT = new HashMap<String, String>(){{
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
    static final HashMap<String, String> UNSHIFT = new HashMap<String, String>(){{
        for (String k : SHIFT.keySet())
            put(SHIFT.get(k), k);
    }};
    
    private LibraryManager(VM vm) {
        this.vm = vm;
    }

    private void define(String name, JNative.Method method, Type returnType, int argc) {
        if (vm == null) {
            Type[] types = new Type[argc];
            for (int i = 0; i < argc; i++)
                types[i] = Types.ANY;
            Shell.globals.put(name, new FuncType(returnType, types, new GenericType[0], false));
        } else
            vm.defineNative(name, method, argc);
    }

    private void define(String name, JNative.Method method, Type returnType, Type... types) {
        if (vm == null)
            Shell.globals.put(name, new FuncType(returnType, types, new GenericType[0], false));
        else
            vm.defineNative(name, method, types);
    }

    // Esto se usa para funciones irregulares
    // tuple(1, 2, 3) -> (int, int, int), tuple(1, 2, "3") -> (int, int, String)
    private void define(String name, JNative.Method method, FuncType type) {
        if (vm == null)
            Shell.globals.put(name, type);
        else
            vm.defineNative(name, method, type.varargs ? -1 : type.parameterTypes.length);
    }

    public static void Setup(VM vm) {
        new LibraryManager(vm).setup();
    }

    private void setup() {
        builtin();
        time();
        gens();
        io();
        sys();
        awt();
        json();
        httpx();
        puddle();
        guis();
        pretzel();
        collections();

        new JPrinter(vm).Start();
    }

    private void pretzel() {
        new Pretzel(vm).Start();
    }

    private void puddle() {
        new PDL(vm).Start();
    }

    private void httpx() {
        new HTTPx(vm).Start();
    }

    private void awt() {
        new AbstractWindowToolkit(vm).Start();
    }

    private void json() {
        new JPSon(vm).Start();
    }

    private void sys() {
        new JSystem(vm).Start();
    }

    private void guis() {
        new GUIs(vm).Start();
    }

    private void io() {
        new IOFile(vm).Start();
    }

    private void gens() {
        new Generators(vm).Start();
    }

    private void collections() {
        new JPCollections(vm).Start();
    }

    private void builtin() {
        // IO Function
        define("print", (args) -> {
            Shell.logger.out(args[0]);
            return NativeResult.Ok();
        }, Types.VOID, 1);
        define("println", (args) -> {
            Shell.logger.outln(args[0]);
            return NativeResult.Ok();
        }, Types.VOID, 1);
        define("printback", (args) -> {
            Shell.logger.out(args[0]);
            return NativeResult.Ok(args[0]);
        }, Types.ANY, 1);

        define("field", (args) -> {
            Shell.logger.out(args[0].asString());
            return NativeResult.Ok(new Value(Shell.logger.in()));
        }, Types.STRING, 1);
        define("nfield", (args) -> {
            Pattern p = Pattern.compile("-?\\d+(\\.\\d+)?");
            Shell.logger.out(args[0].asString());
            String text;
            do {
                text = Shell.logger.in();
            } while (!p.matcher(text).matches());
            return NativeResult.Ok(new Value(Double.parseDouble(text)));
        }, Types.FLOAT, 1);

        define("sim", (args) -> {
            Pair<JFunc, Error> pair = Shell.compile("<sim>", args[0].asString());
            if (pair.b != null) {
                return NativeResult.Err(pair.b.error_name, pair.b.details);
            }
            Shell.runCompiled("<sim>", pair.a, new String[0]);
            return NativeResult.Ok();
        }, Types.VOID, Types.STRING);
        define("run", (args) -> {
            String path = args[0].asString();
            if (path.endsWith(".devp")) {
                String text;
                try {
                    text = readString(Paths.get(path));
                } catch (IOException e) {
                    return NativeResult.Err("Internal", e.toString());
                }

                Pair<JFunc, Error> pair = Shell.compile(path, text);
                if (pair.b != null) {
                    return NativeResult.Err(pair.b.error_name, pair.b.details);
                }
                Shell.runCompiled(path, pair.a, new String[0]);
            }
            else if (path.endsWith(".jbox")) {
                Shell.runCompiled(path, path, new String[0]);
            }
            else {
                return NativeResult.Err("File Extension", "Invalid file extension");
            }
            return NativeResult.Ok();
        }, Types.VOID, Types.STRING);

        define("clear", (args) -> {
            try {
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                }
                else {
                    Runtime.getRuntime().exec("clear");
                }
            } catch (IOException | InterruptedException ignored) {}
            return NativeResult.Ok();
        }, Types.VOID, 0);

        // Number Functions
        define("round", (args) -> NativeResult.Ok(new Value(Math.round(args[0].asNumber()))), Types.INT, Types.FLOAT);
        define("floor", (args) -> NativeResult.Ok(new Value(Math.floor(args[0].asNumber()))), Types.INT, Types.FLOAT);
        define("ceil", (args) -> NativeResult.Ok(new Value(Math.ceil(args[0].asNumber()))), Types.INT, Types.FLOAT);
        define("abs", (args) -> NativeResult.Ok(new Value(Math.abs(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
        define("arctan2",
                (args) -> NativeResult.Ok(new Value(Math.atan2(args[0].asNumber(), args[1].asNumber()))),
                Types.FLOAT, Types.FLOAT, Types.FLOAT);
        define("sin", (args) -> NativeResult.Ok(new Value(Math.sin(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
        define("cos", (args) -> NativeResult.Ok(new Value(Math.cos(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
        define("tan", (args) -> NativeResult.Ok(new Value(Math.tan(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
        define("arcsin", (args) -> NativeResult.Ok(new Value(Math.asin(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
        define("arccos", (args) -> NativeResult.Ok(new Value(Math.acos(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
        define("arctan", (args) -> NativeResult.Ok(new Value(Math.atan(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
        define("min",
                (args) -> NativeResult.Ok(new Value(Math.min(args[0].asNumber(), args[1].asNumber()))),
                Types.FLOAT, Types.FLOAT, Types.FLOAT);
        define("max",
                (args) -> NativeResult.Ok(new Value(Math.max(args[0].asNumber(), args[1].asNumber()))),
                Types.FLOAT, Types.FLOAT, Types.FLOAT);
        define("log",
                (args) -> NativeResult.Ok(new Value(Math.log(args[0].asNumber()) / Math.log(args[1].asNumber()))),
                Types.FLOAT, Types.FLOAT, Types.FLOAT);
        define("doubleStr",
                (args) -> NativeResult.Ok(new Value(String.format("%." + args[1].asNumber().intValue(), args[0].asNumber()))),
                Types.FLOAT, Types.FLOAT, Types.FLOAT);
        define("parseNum",
                (args) -> {
                    try {
                        return NativeResult.Ok(new Value(Double.parseDouble(args[0].asString())));
                    } catch (NumberFormatException e) {
                        return NativeResult.Err("Number Format", "Could not parse number");
                    }
                },
                Types.FLOAT, Types.STRING);

        // Random Functions
        define("random", (args) -> NativeResult.Ok(new Value(Math.random())), Types.FLOAT, 0);
        define("randint", (args) -> {
            double min = args[0].asNumber();
            double max = args[1].asNumber();
            return NativeResult.Ok(new Value(min + Math.round(Math.random() * (max - min + 1))));
        }, Types.FLOAT, Types.FLOAT, Types.FLOAT);
        define("choose", args -> {
            List<Value> list = args[0].asList();
            int max = list.size() - 1;
            int index = (int) (Math.random() * max);
            return NativeResult.Ok(list.get(index));
        }, Types.ANY, 1);

        // Type Functions
        define("type", (args) -> NativeResult.Ok(new Value(args[0].type())), Types.STRING, 1);

        define("isList", (args) -> NativeResult.Ok(new Value(args[0].isList)), Types.BOOL, 1);
        define("isFunction", (args) -> NativeResult.Ok(new Value(args[0].isClosure)), Types.BOOL, 1);
        define("isBoolean", (args) -> NativeResult.Ok(new Value(args[0].isBool)), Types.BOOL, 1);
        define("isDict", (args) -> NativeResult.Ok(new Value(args[0].isMap)), Types.BOOL, 1);
        define("isNumber", (args) -> NativeResult.Ok(new Value(args[0].isNumber)), Types.BOOL, 1);
        define("isString", (args) -> NativeResult.Ok(new Value(args[0].isString)), Types.BOOL, 1);

        define("str", (args) -> NativeResult.Ok(new Value(args[0].asString())), Types.STRING, 1);
        define("list", (args) -> NativeResult.Ok(new Value(args[0].asList())), Types.LIST, 1);
        define("bool", (args) -> NativeResult.Ok(new Value(args[0].asBool())), Types.BOOL, 1);
        define("num", (args) -> NativeResult.Ok(new Value(args[0].asNumber())), Types.FLOAT, 1);
        define("dict", (args) -> NativeResult.Ok(new Value(args[0].asMap())), Types.DICT, 1);
        define("chr", (args) -> NativeResult.Ok(new Value(new String(
                new byte[] { args[0].asNumber().byteValue() }
        ))), Types.STRING, Types.INT);
        define("chrs", (args) -> NativeResult.Ok(new Value(new String(
                args[0].asBytes()
        ))), Types.STRING, Types.BYTES);
        define("tuple", (args) -> NativeResult.Ok(new Value(args)),
                new FuncType(Types.ANY, new Type[0], new GenericType[0], true) {
                    @Override
                    public Type call(Type[] arguments, Type[] generics) {
                        if (generics.length > 0) {
                            return null;
                        }
                        return new TupleType(arguments);
                    }
                }
        );

        // Convert number list to byte[]
        define("byter", (args) -> {
            List<Value> list = args[0].asList();
            byte[] bytes = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Value v = list.get(i);
                if (!v.isNumber || v.asNumber().byteValue() != v.asNumber())
                    return NativeResult.Err("Type", "List must contain only bytes");
                bytes[i] = v.asNumber().byteValue();
            }
            return NativeResult.Ok(new Value(bytes));
        }, Types.LIST, Types.LIST);

        define("floating",
                (args) -> NativeResult.Ok(new Value(Math.round(args[0].asNumber()) != args[0].asNumber())),
                Types.BOOL, Types.FLOAT);

        // Dictionary Functions
        define("set", (args) -> {
            args[0].asMap().put(args[1], args[2]);
            return NativeResult.Ok();
        }, Types.VOID, Types.DICT, Types.ANY, Types.ANY);
        define("overset", (args) -> {
            args[0].asMap().replace(args[1], args[2]);
            return NativeResult.Ok();
        }, Types.VOID, Types.DICT, Types.ANY, Types.ANY);
        define("get", (args) -> {
            if (args[0].asMap().containsKey(args[1]))
                return NativeResult.Ok(args[0].get(args[1]));
            return NativeResult.Err("Key", "Key not found");
        }, Types.ANY, Types.DICT, Types.ANY);
        define("delete", (args) -> {
            args[0].delete(args[1]);
            return NativeResult.Ok();
        }, Types.VOID, Types.DICT, Types.ANY);

        // String Functions
        define("split", (args) -> {
            String str = args[0].asString();
            String delim = args[1].asString();
            String[] result = str.split(delim);
            List<Value> list = new ArrayList<>();
            for (String s : result) {
                list.add(new Value(s));
            }
            return NativeResult.Ok(new Value(list));
        }, Types.LIST, Types.STRING, Types.STRING);
        define("substr", (args) -> {
            String str = args[0].asString();
            int start = args[1].asNumber().intValue();
            int end = args[2].asNumber().intValue();

            while (start < 0) start = str.length() + start;
            while (end < 0) end = str.length() + end;

            if (start > str.length()) start = str.length();
            if (end > str.length()) end = str.length();

            return NativeResult.Ok(new Value(str.substring(start, end)));
        }, Types.STRING, Types.STRING, Types.INT, Types.INT);
        define("join", (args) -> {
            Value str = args[0];
            Value list = args[1];

            List<String> strings = new ArrayList<>();
            for (Value val : list.asList())
                strings.add(val.asString());

            return NativeResult.Ok(new Value(String.join(str.asString(), strings)));
        }, Types.STRING, Types.STRING, Types.LIST);
        define("replace", (args) -> {
            String str = args[0].asString();
            String old = args[1].asString();
            String newStr = args[2].asString();
            return NativeResult.Ok(new Value(str.replace(old, newStr)));
        }, Types.STRING, Types.STRING, Types.STRING, Types.STRING);
        define("escape",
                (args) -> NativeResult.Ok(new Value(StringEscapeUtils.unescapeJava(args[0].asString()))),
                Types.STRING, Types.STRING);
        define("unescape",
                (args) -> NativeResult.Ok(new Value(StringEscapeUtils.escapeJava(args[0].asString()))),
                Types.STRING, Types.STRING);
        define("strUpper",
                (args) -> NativeResult.Ok(new Value(args[0].asString().toUpperCase())),
                Types.STRING, Types.STRING);
        define("strLower",
                (args) -> NativeResult.Ok(new Value(args[0].asString().toLowerCase())),
                Types.STRING, Types.STRING);
        define("strShift", (args) -> {
            String str = args[0].asString();
            StringBuilder sb = new StringBuilder();
            for (char c : str.toCharArray()) {
                String s = Character.toString(c);
                sb.append(SHIFT.getOrDefault(s, s));
            }
            return NativeResult.Ok(new Value(sb.toString()));
        }, Types.STRING, Types.STRING);
        define("strUnshift", (args) -> {
            String str = args[0].asString();
            StringBuilder sb = new StringBuilder();
            for (char c : str.toCharArray()) {
                String s = Character.toString(c);
                sb.append(UNSHIFT.getOrDefault(s, s));
            }
            return NativeResult.Ok(new Value(sb.toString()));
        }, Types.STRING, Types.STRING);

        // List Functions
        define("append", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.append(value);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.ANY);
        define("remove", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.remove(value);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.ANY);
        define("pop", (args) -> {
            Value list = args[0];
            Value index = args[1];

            if (index.asNumber() < 0 || index.asNumber() >= list.asList().size()) {
                return NativeResult.Err("Index", "Index out of bounds");
            }

            return NativeResult.Ok(list.pop(index.asNumber()));
        }, Types.ANY, Types.LIST, Types.INT);
        define("extend", (args) -> {
            Value list = args[0];
            Value other = args[1];

            list.add(other);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.LIST);
        define("insert", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            if (list.asList().size() < index.asNumber() || index.asNumber() < 0) {
                return NativeResult.Err("Scope", "Index out of bounds");
            }

            list.insert(index.asNumber(), value);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.ANY, Types.INT);
        define("setIndex", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            if (index.asNumber() >= list.asList().size()) {
                return NativeResult.Err("Index", "Index out of bounds");
            }

            list.set(index.asNumber(), value);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.ANY, Types.INT);
        define("sublist", (args) -> {
            Value list = args[0];
            Value start = args[1];
            Value end = args[2];

            if (list.asList().size() < end.asNumber() || start.asNumber() < 0 || end.asNumber() < start.asNumber()) {
                return NativeResult.Err("Scope", "Index out of bounds");
            }

            return NativeResult.Ok(new Value(list.asList().subList(start.asNumber().intValue(),
                    end.asNumber().intValue())));
        }, Types.LIST, Types.LIST, Types.INT, Types.INT);

        // Collection Functions
        define("size", args -> {
            Value list = args[0];
            return NativeResult.Ok(new Value(list.asList().size()));
        }, Types.INT, 1);
        define("contains", args -> {
            Value list = args[0];
            Value val = args[1];
            return NativeResult.Ok(new Value(list.asList().contains(val)));
        }, Types.BOOL, 2);
        define("indexOf", args -> {
            Value list = args[0];
            Value val = args[1];
            return NativeResult.Ok(new Value(list.asList().indexOf(val)));
        }, Types.INT, 2);

        // Results
        define("ok", args -> NativeResult.Ok(new Value(args[0].asBool())), Types.BOOL, Types.RESULT);
        define("resolve", args -> {
            if (!args[0].asBool())
                return NativeResult.Err("Unresolved", "Unresolved error in catcher");
            return NativeResult.Ok(args[0].asRes().getValue());
        }, Types.ANY, Types.RESULT);
        define("catch", args -> {
            if (!args[0].asBool())
                return NativeResult.Ok(new Value(args[0].asList()));
            return NativeResult.Ok();
        }, Types.ANY, Types.RESULT);
        define("fail", args -> {
            if (args[0].asBool())
                return NativeResult.Ok();
            return NativeResult.Err("Released", args[0].toString());
        }, Types.VOID, Types.RESULT);
    }

    private void time() {
        new Time(vm).Start();
    }
}
