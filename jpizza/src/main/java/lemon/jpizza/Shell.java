package lemon.jpizza;

import lemon.jpizza.compiler.Compiler;
import lemon.jpizza.compiler.FunctionType;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.errors.Error;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.generators.Lexer;
import lemon.jpizza.generators.Parser;
import lemon.jpizza.libraries.*;
import lemon.jpizza.libraries.httpretzel.HTTPretzel;
import lemon.jpizza.libraries.jdraw.JDraw;
import lemon.jpizza.libraries.pdl.SafeSocks;
import lemon.jpizza.libraries.socks.SockLib;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.expressions.BodyNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.executables.ClassInstance;
import lemon.jpizza.objects.primitives.PList;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.ParseResult;
import lemon.jpizza.results.RTResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Shell {

    public static final Logger logger = new Logger();
    public static final SymbolTable globalSymbolTable = new SymbolTable();
    public static String root;
    public static VM vm;
    public static final String fileEncoding = System.getProperty("file.encoding");

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
        SafeSocks.initialize();
    }

    @SuppressWarnings("DuplicatedCode")
    public static void main(String[] args) throws IOException {
        root = System.getenv("JPIZZA_DATA_DIR") == null ? System.getProperty("user.home") + "/.jpizza" : System.getenv("JPIZZA_DATA_DIR");
        Scanner in = new Scanner(System.in);
        initLibs();

        PList cmdargs = new PList(new ArrayList<>());
        for (String arg : args) {
            cmdargs.append(new Str(arg));
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
            }
            else if (args[0].endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    String scrpt = Files.readString(Path.of(args[0]));
                    String dir = Path.of(args[0]).toString();
                    String[] dsfn = getFNDirs(dir);
                    String fn = dsfn[0]; String newDir = dsfn[1];
                    System.setProperty("user.dir", newDir);
                    Pair<Obj, Error> res = run(fn, scrpt, false);
                    if (res.b != null)
                        Shell.logger.fail(res.b.asString());
                }
                else {
                    Shell.logger.outln("File does not exist.");
                }
            }
            else if (args[0].equals("docs")) {
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
                }
                else {
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
                            }
                            else {
                                logger.enableLogging();
                                logger.outln("{}");
                            }
                            logger.disableLogging();
                            return;
                        }
                        case "--compile" -> {
                            Error res = compile(fn, scrpt,
                                    newDir + "\\" + fn.substring(0, fn.length() - 5) + ".jbox");
                            if (res != null) {
                                String message = String.format("%s: %s", res.error_name, res.details);
                                Shell.logger.enableLogging();
                                Shell.logger.outln(String.format("{\"lines\": [%s, %s], \"cols\": [%s, %s], \"msg\": " +
                                                "\"%s\"}",
                                        res.pos_start.ln, res.pos_end.ln,
                                        res.pos_start.col, res.pos_end.col,
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
                }
                else {
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
                }
                else {
                    Shell.logger.outln("File does not exist.");
                }
            }
            return;
        }

        Shell.logger.outln("Exit with 'quit'");
        Shell.logger.enableTips();
        while (true) {
            Shell.logger.out("-> ");
            String input = in.nextLine() + ";";

            if (input.equals("quit;"))
                break;
           //  compile("<shell>", input, "shell.jbox");
            Pair<Obj, Error> a = run("<shell>", input, true);
            if (a.b != null) {
                Shell.logger.fail(a.b.asString());
            }
            else {
                List<Obj> results = a.a.list;
                if (results.size() > 0) {
                    StringBuilder out = new StringBuilder();
                    int size = results.size();
                    for (Obj result : results) {
                        if (result != null && result.jptype != Constants.JPType.Null)
                            out.append(Shell.logger.ots(result)).append(", ");
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
        ParseResult<Node> ast = parser.parse();
        if (ast.error != null)
            return new Pair<>(null, ast.error);
        return new Pair<>(((BodyNode)ast.node).statements, null);
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
            return new Pair<>(null, RTError.Internal(
                    null, null,
                    "Out of memory",
                    context
            ));
        }
        return new Pair<>(result.value, result.error);
    }

    public static Error compile(String fn, String text, String outpath) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return ast.b;
        List<Node> outNode = ast.a;

        Compiler compiler = new Compiler(FunctionType.Script, text);
        JFunc func = compiler.compileBlock(outNode);

        try {
            FileOutputStream fout;
            fout = new FileOutputStream(outpath);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(func);
            oos.close();
            fout.close();
        } catch (IOException e) {
            return RTError.Internal(
                    null, null,
                    "Could not write to file",
                    null
            );
        }

        return null;
    }

    public static Pair<Obj, Error> runCompiled(String fn, String inpath) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(inpath);
        } catch (FileNotFoundException e) {
            return new Pair<>(null, RTError.FileNotFound(null, null,
                    "File does not exist!\n" + inpath, null));
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object ost = ois.readObject();
            ois.close();
            fis.close();
            if (!(ost instanceof JFunc)) return new Pair<>(null, RTError.FileNotFound(null, null,
                    "File is not JPizza bytecode!", null));

            JFunc func = (JFunc) ost;
            vm = new VM(func).trace(fn);

            vm.run();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new Pair<>(null, null);
    }

    //Another public static 
    public static Pair<ClassInstance, Error> imprt(String fn, String text, Context _parent, Position _entry_pos) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Pair<>(null, ast.b);
        Context context = new Context(fn, null, null);
        context.symbolTable = new SymbolTable(globalSymbolTable);
        RTResult result = new Interpreter().interpret(ast.a, context, false);
        return new Pair<>(new ClassInstance(context, fn), result.error);
    }

}
