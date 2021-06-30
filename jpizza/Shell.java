package lemon.jpizza;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Generators.Lexer;
import lemon.jpizza.Generators.Parser;
import lemon.jpizza.Libraries.BuiltInFunction;
import lemon.jpizza.Libraries.GUIs;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Executables.ClassInstance;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.PList;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Results.ParseResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import lemon.jpizza.Errors.Error;
import lemon.jpizza.Results.RTResult;

public class Shell {

    public static Logger logger = new Logger();
    static SymbolTable globalSymbolTable = new SymbolTable();

    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);

        // Load librarys
        GUIs.initialize("GUIs", GUIs.class, new HashMap<>(){{
            put("GUI", Collections.singletonList("value"));
        }});

        BuiltInFunction.initialize("compiled", BuiltInFunction.class, new HashMap<>(){{
            put("insert", Arrays.asList("list", "item", "index"));
            put("set", Arrays.asList("dict", "key", "value"));
            put("get", Arrays.asList("dict", "value"));
            put("delete", Arrays.asList("dict", "value"));
            put("foreach", Arrays.asList("list", "func"));
            put("append", Arrays.asList("list", "value"));
            put("remove", Arrays.asList("list", "value"));
            put("pop", Arrays.asList("list", "value"));
            put("extend", Arrays.asList("listA", "listB"));
            put("contains", Arrays.asList("list", "value"));
            put("randint", Arrays.asList("min", "max"));
            put("split", Arrays.asList("value", "splitter"));
            put("println", Collections.singletonList("value"));
            put("print", Collections.singletonList("value"));
            put("printback", Collections.singletonList("value"));
            put("type", Collections.singletonList("value"));
            put("value", Collections.singletonList("value"));
            put("round", Collections.singletonList("value"));
            put("floor", Collections.singletonList("value"));
            put("ceil", Collections.singletonList("value"));
            put("abs", Collections.singletonList("value"));
            put("run", Collections.singletonList("fn"));
            put("size", Collections.singletonList("value"));
            put("str", Collections.singletonList("value"));
            put("list", Collections.singletonList("value"));
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
            put("floating", Collections.singletonList("value"));
            put("random", new ArrayList<>());
            put("clear", new ArrayList<>());

        }}, globalSymbolTable);

        PList cmdargs = new PList(new ArrayList<>());

        if (args.length == 1) {
            if (args[0].equals("help")) {
                Shell.logger.outln("""
                        dp -> Open venv
                        dp help -> List commands
                        dp docs -> Link to documentation
                        dp <file> -> Run file
                        """);
            } else if (args[0].endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    String scrpt = Files.readString(Path.of(args[0]));
                    String newDir = Path.of(args[0]).toString();
                    newDir = newDir.substring(0, newDir.lastIndexOf('\\'));
                    System.setProperty("user.dir", newDir);
                    globalSymbolTable.define("CMDARGS", cmdargs);
                    Double<Obj, Error> res = run(args[0], scrpt);
                    if (res.b != null)
                        Shell.logger.outln(res.b.asString());
                } else {
                    Shell.logger.outln("File does not exist.");
                }
            } else if (args[0].equals("docs")) {
                Shell.logger.outln("Documentation: https://bit.ly/3vM8G0a");
            }
            return;
        }

        if (args.length == 2) {
            if (args[0].endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    boolean debug = false;
                    if (args[1].equals("--debug")) {
                        debug = true;
                        logger.disableLogging();
                    }
                    String scrpt = Files.readString(Path.of(args[0]));
                    globalSymbolTable.define("CMDARGS", cmdargs);
                    Double<Obj, Error> res = run(args[0], scrpt);
                    if (res.b != null) {
                        if (!debug) Shell.logger.outln(res.b.asString());
                        else {
                            Error e = res.b;
                            String message = String.format("%s: %s", e.error_name, e.details);
                            Shell.logger.enableLogging();
                            Shell.logger.outln(String.format("{\"lines\": [%s, %s], \"cols\": [%s, %s], \"msg\": \"%s\"}",
                                    e.pos_start.ln, e.pos_end.ln,
                                    e.pos_start.col, e.pos_end.col,
                                    message));
                        }
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
                    Double<Obj, Error> res = run(args[0], scrpt);
                    for (int i = 1; i < args.length; i++)
                        cmdargs.add(new Str(args[i]));
                    globalSymbolTable.define("CMDARGS", cmdargs);
                    if (res.b != null)
                        Shell.logger.outln(res.b.asString());
                } else {
                    Shell.logger.outln("File does not exist.");
                }
            }
            return;
        }

        globalSymbolTable.define("CMDARGS", cmdargs);
        while (true) {
            Shell.logger.out("-> "); String input = in.nextLine();
            if (input.equals("quit"))
                break;
            Double<Obj, Error> a = run("<shell>", input);
            if (a.b != null) Shell.logger.outln(a.b.asString());
            else {
                List<Obj> results = ((PList) a.a).trueValue();
                if (results.size() > 0) {
                    StringBuilder out = new StringBuilder();
                    int size = results.size();
                    for (int i = 0; i < size; i++) {
                        if (!(results.get(i) instanceof Null)) out.append(results.get(i)).append(", ");
                    }
                    if (out.length() > 0) out.setLength(out.length() - 2);
                    Shell.logger.outln(out);
                }
            }
        }
    }

    public static Double<Node, Error> getAst(String fn, String text) {
        Lexer lexer = new Lexer(fn, text);
        Double<List<Token>, Error> x = lexer.make_tokens();
        List<Token> tokens = x.a;
        Error error = x.b;
        if (error != null)
            return new Double<>(null, error);
        Parser parser = new Parser(tokens);
        ParseResult ast = parser.parse();
        if (ast.error != null)
            return new Double<>(null, ast.error);
        return new Double<>((Node)ast.node, null);
    }

    public static Double<Obj, Error> run(String fn, String text) {
        Double<Node, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Double<>(null, ast.b);
        Context context = new Context(fn, null, null);
        context.symbolTable = globalSymbolTable;
        RTResult result = new Interpreter().visit(ast.a, context);
        return new Double<>((Obj) result.value, result.error);
    }
    //Another public static 🥱
    public static Double<ClassInstance, Error> imprt(String fn, String text, Context parent, Position entry_pos) {
        Double<Node, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Double<>(null, ast.b);
        Context context = new Context(fn, parent, entry_pos);
        context.symbolTable = globalSymbolTable;
        RTResult result = new Interpreter().visit(ast.a, context);
        return new Double<>(new ClassInstance(context), result.error);
    }

}
