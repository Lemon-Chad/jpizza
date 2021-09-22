package lemon.jpizza;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Errors.Error;
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
import lemon.jpizza.Results.RTResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Shell {

    public static Logger logger = new Logger();
    public static SymbolTable globalSymbolTable = new SymbolTable();
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
        BuiltInFunction.initialize();
        SysLib.initialize();
        JGens.initialize();
        GUIs.initialize();
        FileLib.initialize();
        SockLib.initialize();
        HTTPLIB.initialize();
        JasonLib.initialize();
        JDraw.initialize();
        HTTPretzel.initialize();
        Time.initialize();
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
                        jpizza        ->   Open venv
                        jpizza help   ->   List commands
                        jpizza docs   ->   Link to documentation
                        
                        jpizza <file> ->            Run file
                        jpizza <file> --compile ->  Compile file
                        jpizza <file> --refactor -> Run file with refactoring tips
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
        in.close();
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
                    "Out of memory",
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
