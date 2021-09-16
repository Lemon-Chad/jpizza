package lemon.jpizza;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Generators.Lexer;
import lemon.jpizza.Generators.Parser;
import lemon.jpizza.Libraries.*;
import lemon.jpizza.Libraries.HTTPretzel.HTTPretzel;
import lemon.jpizza.Libraries.JDraw.JDraw;
import lemon.jpizza.Libraries.Socks.SockLib;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Nodes.Values.ListNode;
import lemon.jpizza.Objects.Executables.ClassInstance;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.PList;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Results.ParseResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import lemon.jpizza.Errors.Error;
import lemon.jpizza.Results.RTResult;

public class Shell {

    public static Logger logger = new Logger();
    static SymbolTable globalSymbolTable = new SymbolTable();
    public static String root;

    public static String[] getFNDirs(String dir) {
        int ind = dir.lastIndexOf('\\');
        if (ind == -1)
            return new String[]{dir, "."};
        return new String[]{
                dir.substring(ind),
                dir.substring(0, ind)
        };
    }

    public static void initLibs() {
        // Load librarys
        SysLib.initialize("sys", SysLib.class, new HashMap<>(){{
            put("os", new ArrayList<>());
            put("disableOut", new ArrayList<>());
            put("enableOut", new ArrayList<>());
            put("jpv", new ArrayList<>()); // <-- JPizza version command moved here
            put("execute", Collections.singletonList("cmd"));
            put("getEnvVar", Collections.singletonList("variableName"));
            put("envVarExists", Collections.singletonList("variableName"));
            put("home", new ArrayList<>());
        }});

        GUIs.initialize("GUIs", GUIs.class, new HashMap<>(){{
            put("GUI", Collections.singletonList("value"));
        }});

        JDraw.initialize("awt", JDraw.class, new HashMap<>(){{
            put("drawOval", Arrays.asList("x", "y", "width", "height", "color"));
            put("drawRect", Arrays.asList("x", "y", "width", "height", "color"));
            put("drawCircle", Arrays.asList("radius", "x", "y", "color"));
            put("drawText", Arrays.asList("txt", "x", "y", "color"));
            put("drawSquare", Arrays.asList("radius", "x", "y", "color"));
            put("drawPoly", Arrays.asList("points", "color"));
            put("tracePoly", Arrays.asList("points", "color"));
            put("setPixel", Arrays.asList("x", "y", "color"));
            put("drawImage", Arrays.asList("x", "y", "filename"));
            put("setFont", Arrays.asList("fontName", "fontType", "fontSize"));
            put("setSize", Arrays.asList("width", "height"));
            put("setTitle", Collections.singletonList("value"));
            put("lockSize", Collections.singletonList("value"));
            put("gpuCompute", Collections.singletonList("value"));
            put("setIcon", Collections.singletonList("filename"));
            put("setBackgroundColor", Collections.singletonList("color"));
            put("mouseDown", Collections.singletonList("button"));
            put("keyDown", Collections.singletonList("key"));
            put("keyTyped", Collections.singletonList("key"));
            put("screenshot", Collections.singletonList("filename"));
            put("playSound", Collections.singletonList("filename"));
            put("start", new ArrayList<>());
            put("keyString", new ArrayList<>());
            put("mousePos", new ArrayList<>());
            put("mouseIn", new ArrayList<>());
            put("refresh", new ArrayList<>());
            put("toggleQRender", new ArrayList<>());
            put("qUpdate", new ArrayList<>());
            put("fps", new ArrayList<>());
            put("refreshLoop", new ArrayList<>());
            put("refreshUnloop", new ArrayList<>());
            put("init", new ArrayList<>());
            put("clear", new ArrayList<>());
        }});

        HTTPLIB.initialize("httpx", HTTPLIB.class, new HashMap<>(){{
            put("getRequest", Arrays.asList("url", "params"));
            put("deleteRequest", Arrays.asList("url", "params"));
            put("postRequest", Arrays.asList("url", "params", "body"));
            put("putRequest", Arrays.asList("url", "params", "body"));
            put("patchRequest", Arrays.asList("url", "params", "body"));
            put("optionsRequest", Arrays.asList("url", "params", "body"));
            put("connectRequest", Arrays.asList("url", "params", "body"));
            put("traceRequest", Arrays.asList("url", "params", "body"));
            put("headRequest", Arrays.asList("url", "params", "body"));
        }});

        HTTPretzel.initialize("pretzel", HTTPretzel.class, new HashMap<>(){{
            put("init", Arrays.asList("host", "addr"));
            put("route", Arrays.asList("route", "func"));
            put("start", new ArrayList<>());
        }});

        JGens.initialize("gens", JGens.class, new HashMap<>(){{
            put("range", Arrays.asList("start", "stop", "step"));
            put("linear", Arrays.asList("start", "stop", "step", "slope", "y-inter"));
            put("quadratic", Arrays.asList("start", "stop", "step", "a", "b", "c"));
        }});

        JasonLib.initialize("json", JasonLib.class, new HashMap<>(){{
            put("loads", Collections.singletonList("value"));
            put("dumps", Collections.singletonList("value"));
        }});

        Time.initialize("time", Time.class, new HashMap<>(){{
            put("halt", Collections.singletonList("ms"));
            put("stopwatch", Collections.singletonList("func"));
            put("epoch", new ArrayList<>());
        }});

        FileLib.initialize("iofile", FileLib.class, new HashMap<>(){{
            put("readFile", Collections.singletonList("dir"));
            put("readSerial", Collections.singletonList("dir"));
            put("readBytes", Collections.singletonList("dir"));
            put("writeFile", Arrays.asList("dir", "val"));
            put("writeSerial", Arrays.asList("dir", "val"));
            put("fileExists", Arrays.asList("dir", "val"));
            put("makeDirs", Collections.singletonList("dir"));
            put("setCWD", Collections.singletonList("dir"));
            put("getCWD", new ArrayList<>());
        }});

        SockLib.initialize("sockets", SockLib.class, new HashMap<>(){{
            put("newServer", Collections.singletonList("port"));
            put("newClient", Arrays.asList("host", "port"));

            put("connect", Collections.singletonList("server"));

            put("serverSend", Arrays.asList("client", "msg"));
            put("serverSendBytes", Arrays.asList("client", "msg"));
            put("serverRecv", Collections.singletonList("client"));
            put("serverRecvBytes", Arrays.asList("client", "length"));
            put("serverRecvAllBytes", Collections.singletonList("client"));

            put("closeServerConnection", Collections.singletonList("client"));
            put("closeServer", Collections.singletonList("server"));

            put("clientSend", Arrays.asList("client", "msg"));
            put("clientSendBytes", Arrays.asList("client", "msg"));
            put("clientRecv", Collections.singletonList("client"));
            put("clientRecvBytes", Arrays.asList("client", "length"));
            put("clientRecvAllBytes", Collections.singletonList("client"));

            put("clientClose", Collections.singletonList("client"));
        }});

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

        }}, globalSymbolTable);
    }

    @SuppressWarnings("DuplicatedCode")
    public static void main(String[] args) throws IOException {
        root = System.getenv("JPIZZA_DATA_DIR") == null ? System.getProperty("user.home") + "/.jpizza" : System.getenv("JPIZZA_DATA_DIR");
        Scanner in = new Scanner(System.in);
        initLibs();

        PList cmdargs = new PList(new ArrayList<>());
        for (int i = 0; i < args.length; i++) {
            cmdargs.append(new Str(args[i]));
        }
        globalSymbolTable.define("CMDARGS", cmdargs);

        if (args.length == 1) {
            if (args[0].equals("help")) {
                Shell.logger.outln("""
                        dp        ->   Open venv
                        dp help   ->   List commands
                        dp docs   ->   Link to documentation
                        
                        dp <file> ->            Run file
                        dp <file> --compile ->  Compile file
                        dp <file> --refactor -> Run file with refactoring tips
                        """);
            } else if (args[0].endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    String scrpt = Files.readString(Path.of(args[0]));
                    String dir = Path.of(args[0]).toString();
                    String[] dsfn = getFNDirs(dir);
                    String fn = dsfn[0]; String newDir = dsfn[1];
                    System.setProperty("user.dir", newDir);
                    Pair<Obj, Error> res = run(fn, scrpt, false);
                    if (res.b != null)
                        Shell.logger.fail(res.b.asString());
                } else {
                    Shell.logger.outln("File does not exist.");
                }
            } else if (args[0].equals("docs")) {
                Shell.logger.outln("Documentation: https://bit.ly/3vM8G0a");
            }
            else if (args[0].endsWith(".jbox")) {
                if (Files.exists(Path.of(args[0]))) {
                    String dir = Path.of(args[0]).toString();
                    String[] dsfn = getFNDirs(dir);
                    String fn = dsfn[0]; String newDir = dsfn[1];
                    System.setProperty("user.dir", newDir);
                    Pair<Obj, Error> res = runCompiled(fn, args[0]);
                    if (res.b != null)
                        Shell.logger.fail(res.b.asString());
                } else {
                    Shell.logger.outln("File does not exist.");
                }
            }
            return;
        }

        if (args.length == 2) {
            if (args[0].endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    String scrpt = Files.readString(Path.of(args[0]));
                    String dir = Path.of(args[0]).toString();
                    String[] dsfn = getFNDirs(dir);
                    String fn = dsfn[0]; String newDir = dsfn[1];
                    System.setProperty("user.dir", newDir);
                    switch (args[1]) {
                        case "--debug" -> {
                            Pair<List<Node>, Error> res = getAst(args[0], scrpt);
                            if (res.b != null) {
                                Error e = res.b;
                                String message = String.format("%s: %s", e.error_name, e.details);
                                logger.enableLogging();
                                logger.outln(String.format("{\"lines\": [%s, %s], \"cols\": [%s, %s], \"msg\": \"%s\"}",
                                        e.pos_start.ln, e.pos_end.ln,
                                        e.pos_start.col, e.pos_end.col,
                                        message));
                            } else {
                                logger.enableLogging();
                                logger.outln("{}");
                            }
                            logger.disableLogging();
                            return;
                        }
                        case "--compile" -> {
                            Pair<Obj, Error> res = compile(fn, scrpt,
                                    newDir + "\\" + fn.substring(0, fn.length() - 5) + ".jbox");
                            if (res.b != null) {
                                Error e = res.b;
                                String message = String.format("%s: %s", e.error_name, e.details);
                                Shell.logger.enableLogging();
                                Shell.logger.outln(String.format("{\"lines\": [%s, %s], \"cols\": [%s, %s], \"msg\": " +
                                                "\"%s\"}",
                                        e.pos_start.ln, e.pos_end.ln,
                                        e.pos_start.col, e.pos_end.col,
                                        message));
                            }
                            return;
                        }
                        case "--refactor" -> logger.enableTips();
                    }
                    Pair<Obj, Error> res = run(args[0], scrpt, false);
                    if (res.b != null) {
                        Shell.logger.fail(res.b.asString());
                    }
                } else {
                    Shell.logger.outln("File does not exist.");
                }
            }
            return;
        }

        if (args.length > 1) {
            if (args[0].endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    String scrpt = Files.readString(Path.of(args[0]));
                    Pair<Obj, Error> res = run(args[0], scrpt, false);
                    if (res.b != null)
                        Shell.logger.fail(res.b.asString());
                } else {
                    Shell.logger.outln("File does not exist.");
                }
            }
            return;
        }
        Shell.logger.outln("Exit with 'quit'");
        Shell.logger.enableTips();
        while (true) {
            Shell.logger.out("-> "); String input = in.nextLine() + ";";
            if (input.equals("quit;"))
                break;
            Pair<Obj, Error> a = run("<shell>", input, true);
            if (a.b != null) Shell.logger.fail(a.b.asString());
            else {
                List<Obj> results = ((PList) a.a).trueValue();
                if (results.size() > 0) {
                    StringBuilder out = new StringBuilder();
                    int size = results.size();
                    for (int i = 0; i < size; i++) {
                        if (results.get(i).jptype != Constants.JPType.Null)
                            out.append(Shell.logger.ots(results.get(i))).append(", ");
                    }
                    if (out.length() > 0) out.setLength(out.length() - 2);
                    Shell.logger.outln(out.toString());
                }
                a.a = null;
            }
        }
    }

    public static Pair<List<Node>, Error> getAst(String fn, String text) {
        Lexer lexer = new Lexer(fn, text);
        Pair<List<Token>, Error> x = lexer.make_tokens();
        List<Token> tokens = x.a;
        Error error = x.b;
        if (error != null)
            return new Pair<>(null, error);
        Parser parser = new Parser(tokens);
        ParseResult ast = parser.parse();
        if (ast.error != null)
            return new Pair<>(null, ast.error);
        return new Pair<>(((ListNode)ast.node).elements, null);
    }

    public static Pair<Obj, Error> run(String fn, String text, boolean log) {
        return run(fn, text, true, log);
    }
    public static Pair<Obj, Error> run(String fn, String text, boolean main, boolean log) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Pair<>(null, ast.b);
        Context context = new Context(fn, null, null);
        context.symbolTable = globalSymbolTable;
        Interpreter inter = new Interpreter();
        if (main) inter.makeMain();
        RTResult result;
        try {
            result = inter.interpret(ast.a, context, log);
            if (result.error != null) return new Pair<>(result.value, result.error);
            result.register(inter.finish(context));
        } catch (OutOfMemoryError e) {
            return new Pair<>(null, new RTError(
                    null, null,
                    "Out of memory!",
                    context
            ));
        }
        return new Pair<>(result.value, result.error);
    }

    public static Pair<Obj, Error> compile(String fn, String text, String outpath) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Pair<>(null, ast.b);
        List<Node> outNode = ast.a;
        FileOutputStream fout;
        ObjectOutputStream oos;

        File outFile = new File(outpath);
        try {
            //noinspection ResultOfMethodCallIgnored
            outFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fout = new FileOutputStream(outFile);
            oos = new ObjectOutputStream(fout);
            System.out.println("Pizza boxing...");
            oos.writeObject(new PizzaBox(outNode));
            fout.close();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Pair<>(null, null);
    }

    public static Pair<Obj, Error> runCompiled(String fn, String inpath) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(inpath);
        } catch (FileNotFoundException e) {
            return new Pair<>(null, new RTError(null, null,
                    "File does not exist!\n" + inpath, null));
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object ost = ois.readObject();
            ois.close();
            fis.close();
            if (!(ost instanceof PizzaBox)) return new Pair<>(null, new RTError(null, null,
                    "File is not a JPizza AST!", null));
            List<Node> ast = ((PizzaBox) ost).value;
            Context context = new Context(fn, null, null);
            context.symbolTable = globalSymbolTable;
            Interpreter inter = new Interpreter();
            inter.makeMain();
            RTResult result = inter.interpret(ast, context, true);
            if (result.error != null) return new Pair<>(result.value, result.error);
            result.register(inter.finish(context));
            return new Pair<>(result.value, result.error);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new Pair<>(null, null);
    }

    //Another public static 
    public static Pair<ClassInstance, Error> imprt(String fn, String text, Context parent, Position entry_pos) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Pair<>(null, ast.b);
        Context context = new Context(fn, parent, entry_pos);
        context.symbolTable = globalSymbolTable;
        RTResult result = new Interpreter().interpret(ast.a, context, false);
        return new Pair<>(new ClassInstance(context), result.error);
    }

}
