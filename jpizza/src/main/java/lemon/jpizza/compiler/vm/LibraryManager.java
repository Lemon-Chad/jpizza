package lemon.jpizza.compiler.vm;

import lemon.jpizza.Pair;
import lemon.jpizza.Shell;
import lemon.jpizza.compiler.libraries.Generators;
import lemon.jpizza.compiler.libraries.IOFile;
import lemon.jpizza.compiler.libraries.JSystem;
import lemon.jpizza.compiler.libraries.Time;
import lemon.jpizza.compiler.libraries.awt.AbstractWindowToolkit;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.errors.Error;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class LibraryManager {
    VM vm;

    static final HashMap<String, String> SHIFT = new HashMap<>(){{
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
    static final HashMap<String, String> UNSHIFT = new HashMap<>(){{
        for (String k : SHIFT.keySet())
            put(SHIFT.get(k), k);
    }};
    
    private LibraryManager(VM vm) {
        this.vm = vm;
    }

    private void define(String name, JNative.Method method, int argc) {
        vm.defineNative(name, method, argc);
    }

    private void define(String name, JNative.Method method, List<String> types) {
        vm.defineNative(name, method, types);
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
    }

    private void awt() {
        new AbstractWindowToolkit(vm).setup();
    }

    private void sys() {
        new JSystem(vm).setup();
    }

    private void io() {
        new IOFile(vm).setup();
    }

    private void gens() {
        new Generators(vm).setup();
    }

    private void builtin() {
        // IO Functions
        define("print", (args) -> {
            Shell.logger.out(args[0]);
            return NativeResult.Ok();
        }, 1);
        define("println", (args) -> {
            Shell.logger.outln(args[0]);
            return NativeResult.Ok();
        }, 1);
        define("printback", (args) -> {
            Shell.logger.out(args[0]);
            return NativeResult.Ok(args[0]);
        }, 1);

        define("field", (args) -> {
            Shell.logger.out(args[0].asString());
            return NativeResult.Ok(new Value(Shell.logger.in()));
        }, 1);
        define("nfield", (args) -> {
            Pattern p = Pattern.compile("-?\\d+(\\.\\d+)?");
            Shell.logger.out(args[0].asString());
            String text;
            do {
                text = Shell.logger.in();
            } while (!p.matcher(text).matches());
            return NativeResult.Ok(new Value(Double.parseDouble(text)));
        }, 1);

        define("sim", (args) -> {
            Pair<JFunc, Error> pair = Shell.compile("<sim>", args[0].asString());
            if (pair.b != null) {
                return NativeResult.Err(pair.b.error_name, pair.b.details);
            }
            Shell.runCompiled("<sim>", pair.a, new String[0]);
            return NativeResult.Ok();
        }, List.of("String"));
        define("run", (args) -> {
            String path = args[0].asString();
            if (path.endsWith(".devp")) {
                String text;
                try {
                    text = Files.readString(Path.of(path));
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
        }, List.of("String"));

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
        }, 0);

        // Number Functions
        define("round", (args) -> NativeResult.Ok(new Value(Math.round(args[0].asNumber()))), List.of("num"));
        define("floor", (args) -> NativeResult.Ok(new Value(Math.floor(args[0].asNumber()))), List.of("num"));
        define("ceil", (args) -> NativeResult.Ok(new Value(Math.ceil(args[0].asNumber()))), List.of("num"));
        define("abs", (args) -> NativeResult.Ok(new Value(Math.abs(args[0].asNumber()))), List.of("num"));
        define("arctan2",
                (args) -> NativeResult.Ok(new Value(Math.atan2(args[0].asNumber(), args[1].asNumber()))),
                List.of("num", "num"));
        define("sin", (args) -> NativeResult.Ok(new Value(Math.sin(args[0].asNumber()))), List.of("num"));
        define("cos", (args) -> NativeResult.Ok(new Value(Math.cos(args[0].asNumber()))), List.of("num"));
        define("tan", (args) -> NativeResult.Ok(new Value(Math.tan(args[0].asNumber()))), List.of("num"));
        define("arcsin", (args) -> NativeResult.Ok(new Value(Math.asin(args[0].asNumber()))), List.of("num"));
        define("arccos", (args) -> NativeResult.Ok(new Value(Math.acos(args[0].asNumber()))), List.of("num"));
        define("arctan", (args) -> NativeResult.Ok(new Value(Math.atan(args[0].asNumber()))), List.of("num"));
        define("min",
                (args) -> NativeResult.Ok(new Value(Math.min(args[0].asNumber(), args[1].asNumber()))),
                List.of("num", "num"));
        define("max",
                (args) -> NativeResult.Ok(new Value(Math.max(args[0].asNumber(), args[1].asNumber()))),
                List.of("num", "num"));
        define("log",
                (args) -> NativeResult.Ok(new Value(Math.log(args[0].asNumber()) / Math.log(args[1].asNumber()))),
                List.of("num", "num"));
        define("doubleStr",
                (args) -> NativeResult.Ok(new Value(String.format("%." + args[1].asNumber().intValue(), args[0].asNumber()))),
                List.of("num", "num"));
        define("parseNum",
                (args) -> {
                    try {
                        return NativeResult.Ok(new Value(Double.parseDouble(args[0].asString())));
                    } catch (NumberFormatException e) {
                        return NativeResult.Err("Number Format", "Could not parse number");
                    }
                },
                List.of("String"));

        // Random Functions
        define("random", (args) -> NativeResult.Ok(new Value(Math.random())), 0);
        define("randint", (args) -> {
            double min = args[0].asNumber();
            double max = args[1].asNumber();
            return NativeResult.Ok(new Value(min + Math.round(Math.random() * (max - min + 1))));
        }, List.of("num", "num"));
        define("choose", args -> {
            List<Value> list = args[0].asList();
            int max = list.size() - 1;
            int index = (int) (Math.random() * max);
            return NativeResult.Ok(list.get(index));
        }, 1);

        // Type Functions
        define("type", (args) -> NativeResult.Ok(new Value(args[0].type())), 1);

        define("isList", (args) -> NativeResult.Ok(new Value(args[0].isList)), 1);
        define("isFunction", (args) -> NativeResult.Ok(new Value(args[0].isClosure)), 1);
        define("isBoolean", (args) -> NativeResult.Ok(new Value(args[0].isBool)), 1);
        define("isDict", (args) -> NativeResult.Ok(new Value(args[0].isMap)), 1);
        define("isNumber", (args) -> NativeResult.Ok(new Value(args[0].isNumber)), 1);
        define("isString", (args) -> NativeResult.Ok(new Value(args[0].isString)), 1);

        define("str", (args) -> NativeResult.Ok(new Value(args[0].asString())), 1);
        define("list", (args) -> NativeResult.Ok(new Value(args[0].asList())), 1);
        define("bool", (args) -> NativeResult.Ok(new Value(args[0].asBool())), 1);
        define("num", (args) -> NativeResult.Ok(new Value(args[0].asNumber())), 1);
        define("dict", (args) -> NativeResult.Ok(new Value(args[0].asMap())), 1);
        define("chr", (args) -> NativeResult.Ok(new Value(new String(
                new byte[] { args[0].asNumber().byteValue() }
        ))), List.of("num"));
        define("chrs", (args) -> NativeResult.Ok(new Value(new String(
                args[0].asBytes()
        ))), List.of("bytearray"));

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
        }, List.of("list"));

        define("floating",
                (args) -> NativeResult.Ok(new Value(Math.round(args[0].asNumber()) != args[0].asNumber())),
                List.of("num"));

        // Dictionary Functions
        define("set", (args) -> {
            args[0].asMap().put(args[1], args[2]);
            return NativeResult.Ok();
        }, List.of("dict", "any", "any"));
        define("overset", (args) -> {
            args[0].asMap().replace(args[1], args[2]);
            return NativeResult.Ok();
        }, List.of("dict", "any", "any"));
        define("get", (args) -> {
            if (args[0].asMap().containsKey(args[1]))
                return NativeResult.Ok(args[0].asMap().getOrDefault(args[1], new Value()));
            return NativeResult.Err("Key", "Key not found");
        }, List.of("dict", "any"));
        define("delete", (args) -> {
            args[0].asMap().remove(args[1]);
            return NativeResult.Ok();
        }, List.of("dict", "any"));

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
        }, List.of("String", "String"));
        define("substr", (args) -> {
            String str = args[0].asString();
            int start = args[1].asNumber().intValue();
            int end = args[2].asNumber().intValue();

            while (start < 0) start = str.length() + start;
            while (end < 0) end = str.length() + end;

            if (start > str.length()) start = str.length();
            if (end > str.length()) end = str.length();

            return NativeResult.Ok(new Value(str.substring(start, end)));
        }, List.of("String", "num", "num"));
        define("join", (args) -> {
            Value str = args[0];
            Value list = args[1];

            List<String> strings = new ArrayList<>();
            for (Value val : list.asList())
                strings.add(val.asString());

            return NativeResult.Ok(new Value(String.join(str.asString(), strings)));
        }, List.of("String", "list"));
        define("replace", (args) -> {
            String str = args[0].asString();
            String old = args[1].asString();
            String newStr = args[2].asString();
            return NativeResult.Ok(new Value(str.replace(old, newStr)));
        }, List.of("String", "String", "String"));
        define("escape",
                (args) -> NativeResult.Ok(new Value(StringEscapeUtils.unescapeJava(args[0].asString()))),
                List.of("String"));
        define("unescape",
                (args) -> NativeResult.Ok(new Value(StringEscapeUtils.escapeJava(args[0].asString())))
                , List.of("String"));
        define("strUpper",
                (args) -> NativeResult.Ok(new Value(args[0].asString().toUpperCase())),
                List.of("String"));
        define("strLower",
                (args) -> NativeResult.Ok(new Value(args[0].asString().toLowerCase()))
                , List.of("String"));
        define("strShift", (args) -> {
            String str = args[0].asString();
            StringBuilder sb = new StringBuilder();
            for (char c : str.toCharArray()) {
                String s = Character.toString(c);
                sb.append(SHIFT.getOrDefault(s, s));
            }
            return NativeResult.Ok(new Value(sb.toString()));
        }, 1);
        define("strUnshift", (args) -> {
            String str = args[0].asString();
            StringBuilder sb = new StringBuilder();
            for (char c : str.toCharArray()) {
                String s = Character.toString(c);
                sb.append(UNSHIFT.getOrDefault(s, s));
            }
            return NativeResult.Ok(new Value(sb.toString()));
        }, 1);

        // Instance Functions
        define("getattr", (args) -> {
            if (!args[0].isInstance) {
                return NativeResult.Err("Type", "Not an instance");
            }
            Value val = args[0].asInstance().getField(args[1].asString(), false);
            if (val == null) {
                return NativeResult.Err("Scope", "No such field");
            }
            return NativeResult.Ok(val);
        }, 2);
        define("hasattr", (args) -> {
            if (!args[0].isInstance) {
                return NativeResult.Err("Type", "Not an instance");
            }
            return NativeResult.Ok(new Value(args[0].asInstance().hasField(args[1].asString())));
        }, 2);

        // List Functions
        define("append", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.append(value);
            return NativeResult.Ok();
        }, List.of("list", "any"));
        define("remove", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.remove(value);
            return NativeResult.Ok();
        }, List.of("list", "any"));
        define("pop", (args) -> {
            Value list = args[0];
            Value index = args[1];

            if (index.asNumber() < 0 || index.asNumber() >= list.asList().size()) {
                return NativeResult.Err("Index", "Index out of bounds");
            }

            return NativeResult.Ok(list.pop(index.asNumber()));
        }, List.of("list", "num"));
        define("extend", (args) -> {
            Value list = args[0];
            Value other = args[1];

            list.add(other);
            return NativeResult.Ok();
        }, List.of("list", "list"));
        define("insert", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            if (list.asList().size() < index.asNumber() || index.asNumber() < 0) {
                return NativeResult.Err("Scope", "Index out of bounds");
            }

            list.insert(index.asNumber(), value);
            return NativeResult.Ok();
        }, List.of("list", "any", "num"));
        define("setIndex", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            if (index.asNumber() >= list.asList().size()) {
                return NativeResult.Err("Index", "Index out of bounds");
            }

            list.set(index.asNumber(), value);
            return NativeResult.Ok();
        }, List.of("list", "any", "num"));
        define("sublist", (args) -> {
            Value list = args[0];
            Value start = args[1];
            Value end = args[2];

            if (list.asList().size() < end.asNumber() || start.asNumber() < 0 || end.asNumber() < start.asNumber()) {
                return NativeResult.Err("Scope", "Index out of bounds");
            }

            return NativeResult.Ok(new Value(list.asList().subList(start.asNumber().intValue(),
                    end.asNumber().intValue())));
        }, List.of("list", "num", "num"));

        // Collection Functions
        define("size", args -> {
            Value list = args[0];
            return NativeResult.Ok(new Value(list.asList().size()));
        }, 1);
        define("contains", args -> {
            Value list = args[0];
            Value val = args[1];
            return NativeResult.Ok(new Value(list.asList().contains(val)));
        }, 2);
        define("indexOf", args -> {
            Value list = args[0];
            Value val = args[1];
            return NativeResult.Ok(new Value(list.asList().indexOf(val)));
        }, 2);

        // Results
        define("ok", args -> NativeResult.Ok(new Value(args[0].asBool())), List.of("catcher"));
        define("resolve", args -> {
            if (!args[0].asBool())
                return NativeResult.Err("Unresolved", "Unresolved error in catcher");
            return NativeResult.Ok(args[0].asRes().getValue());
        }, List.of("catcher"));
        define("catch", args -> {
            if (!args[0].asBool())
                return NativeResult.Ok(new Value(args[0].asList()));
            return NativeResult.Ok();
        }, List.of("catcher"));
        define("fail", args -> {
            if (args[0].asBool())
                return NativeResult.Ok();
            return NativeResult.Err("Released", args[0].toString());
        }, List.of("catcher"));
    }

    private void time() {
        new Time(vm).setup();
    }
}
