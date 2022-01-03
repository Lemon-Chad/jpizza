package lemon.jpizza;

import lemon.jpizza.compiler.Compiler;
import lemon.jpizza.compiler.FunctionType;
import lemon.jpizza.compiler.values.Var;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.compiler.vm.VMResult;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.errors.Error;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.generators.Lexer;
import lemon.jpizza.generators.Optimizer;
import lemon.jpizza.generators.Parser;
import lemon.jpizza.libraries.*;
import lemon.jpizza.libraries.httpretzel.HTTPretzel;
import lemon.jpizza.libraries.jdraw.JDraw;
import lemon.jpizza.libraries.pdl.SafeSocks;
import lemon.jpizza.libraries.socks.SockLib;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.TreePrinter;
import lemon.jpizza.nodes.expressions.BodyNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.executables.ClassInstance;
import lemon.jpizza.results.ParseResult;
import lemon.jpizza.results.RTResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Shell {

    public static final Logger logger = new Logger();
    public static final SymbolTable globalSymbolTable = new SymbolTable();
    public static String root;
    public static VM vm;
    public static final Map<String, Var> globals = new HashMap<>();
    public static final String fileEncoding = System.getProperty("file.encoding");

    static class Flags {
        public static final int COMPILE   = 0b0001;
        public static final int RUN       = 0b0010;
        public static final int REFACTOR  = 0b0100;
        public static final int SHELL     = 0b1000;
    }

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

    static boolean hasFlag(int target, int flag) {
        return (target & flag) == flag;
    }

    @SuppressWarnings("DuplicatedCode")
    public static void main(String[] args) throws IOException {
        root = System.getenv("JPIZZA_DATA_DIR") == null ? System.getProperty("user.home") + "/.jpizza" : System.getenv("JPIZZA_DATA_DIR");

        int flags = Flags.SHELL;
        String to = null;
        String target = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-c" -> flags |= Flags.COMPILE;
                case "-rf" -> flags |= Flags.REFACTOR;
                case "-r" -> flags |= Flags.RUN;
                case "-o" -> {
                    if (i + 1 < args.length) {
                        to = args[i + 1];
                        i++;
                    }
                    else {
                        Shell.logger.fail("-o requires an argument");
                    }
                }
                default -> {
                    if (args[i].startsWith("-")) {
                        Shell.logger.fail("Unknown option: " + args[i]);
                    }
                    else {
                        target = args[i];
                    }
                }
            }
            flags &= ~Flags.SHELL;
        }

        if (flags == Flags.SHELL) repl();

        if (hasFlag(flags, Flags.COMPILE) && hasFlag(flags, Flags.RUN)) {
            Shell.logger.fail("Cannot compile and run at the same time");
        }

        if (hasFlag(flags, Flags.REFACTOR)) {
            Shell.logger.enableTips();
        }

        if (Objects.equals(target, "help")) {
            Shell.logger.outln("Usage: jpizza [options] [target]");
            Shell.logger.outln("Options:");
            Shell.logger.outln("  -c\t\tCompile target");
            Shell.logger.outln("  -r\t\tRun target");
            Shell.logger.outln("  -o [target]\tOutput target to file");
            Shell.logger.outln("  -rf\t\tRefactor target");
            Shell.logger.outln("Targets:");
            Shell.logger.outln("  help\t\tPrint this help message");
            Shell.logger.outln("  <file>\tCompile and/or run file");
            Shell.logger.outln("  v\t\tPrint version");
            Shell.logger.outln("  docs\t\tPrint documentation");
        }
        else if (Objects.equals(target, "version")) {
            Shell.logger.outln("jpizza version " + VM.VERSION);
        }
        else if (Objects.equals(target, "docs")) {
            Shell.logger.outln("Documentation can be found at https://jpizza.readthedocs.io/en/latest/");
        }
        else if (target == null) {
            Shell.logger.fail("No target specified");
        }
        else {
            // .devp is the raw file format
            // It contants the source code
            if (target.endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    String scrpt = Files.readString(Path.of(args[0]));
                    String dir = Path.of(args[0]).toString();
                    String[] dsfn = getFNDirs(dir);
                    String fn = dsfn[0]; String newDir = dsfn[1];
                    System.setProperty("user.dir", newDir);
                    if (hasFlag(flags, Flags.RUN)) {
                        Pair<JFunc, Error> res = compile(fn, scrpt);
                        if (res.b != null)
                            Shell.logger.fail(res.b.asString());
                        runCompiled(fn, res.a, args);
                    }
                    else if (hasFlag(flags, Flags.COMPILE)) {
                        to = to == null ? newDir + "\\" + fn.substring(0, fn.length() - 5) + ".jbox" : to + ".jbox";
                        Error e = compile(fn, scrpt, to);
                        if (e != null)
                            Shell.logger.fail(e.asString());
                    }
                }
                else {
                    Shell.logger.fail("File does not exist.");
                }
            }
            // .jbox is the compiled file format
            // It contains the bytecode
            else if (target.endsWith(".jbox")) {
                if (hasFlag(flags, Flags.COMPILE)) {
                    Shell.logger.fail("Cannot compile a compiled file");
                }
                else if (hasFlag(flags, Flags.REFACTOR)) {
                    Shell.logger.fail("Cannot refactor a compiled file");
                }
                else if (to != null) {
                    Shell.logger.fail("Cannot output to a compiled file");
                }
                else if (hasFlag(flags, Flags.RUN)) {
                    // Run the compiled file
                    if (Files.exists(Path.of(args[0]))) {
                        String dir = Path.of(args[0]).toString();
                        String[] dsfn = getFNDirs(dir);
                        String fn = dsfn[0]; String newDir = dsfn[1];
                        System.setProperty("user.dir", newDir);
                        Error res = runCompiled(fn, args[0], args);
                        if (res != null)
                            Shell.logger.fail(res.asString());
                    }
                    else {
                        Shell.logger.fail("File does not exist.");
                    }
                }
            }
        }

    }

    public static void repl() {
        Scanner in = new Scanner(System.in);

        Shell.logger.outln("Exit with 'quit'");
        Shell.logger.enableTips();

        while (true) {
            Shell.logger.out("-> ");
            String input = in.nextLine() + ";";

            if (input.equals("quit;"))
                break;
            //  compile("<shell>", input, "shell.jbox");
            Pair<JFunc, Error> a = compile("<shell>", input);
            if (a.b != null) {
                Shell.logger.fail(a.b.asString());
            }
            else {
                runCompiled("<shell>", a.a, new String[]{"<shell>"}, globals);
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
        Shell.logger.debug(TreePrinter.print(ast.node));
        BodyNode body = (BodyNode) Optimizer.optimize(ast.node);
        return new Pair<>(body.statements, null);
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

    public static Pair<JFunc, Error> compile(String fn, String text) {
        return compile(fn, text, false);
    }

    public static Pair<JFunc, Error> compile(String fn, String text, boolean scope) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Pair<>(null, ast.b);
        List<Node> outNode = ast.a;

        Compiler compiler = new Compiler(FunctionType.Script, text);

        if (scope)
            compiler.beginScope();
        JFunc func = compiler.compileBlock(outNode);
        if (scope)
            compiler.endScope(ast.a.get(0).pos_start, ast.a.get(ast.a.size() - 1).pos_end);

        return new Pair<>(func, null);
    }

    public static Error compile(String fn, String text, String outpath) {
        Pair<JFunc, Error> res = compile(fn, text);
        if (res.b != null) return res.b;
        JFunc func = res.a;

        try {
            FileOutputStream fout;
            fout = new FileOutputStream(outpath);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(func);
            oos.close();
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
            return RTError.Internal(
                    null, null,
                    "Could not write to file",
                    null
            );
        }

        return null;
    }

    public static JFunc load(String inpath) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(inpath);
        } catch (FileNotFoundException e) {
            Shell.logger.fail(RTError.FileNotFound(null, null,
                    "File does not exist!\n" + inpath, null).asString());
            return null;
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object ost = ois.readObject();
            ois.close();
            fis.close();

            if (!(ost instanceof JFunc)) {
                Shell.logger.fail(RTError.FileNotFound(null, null,
                        "File is not JPizza bytecode!" + inpath, null).asString());
                return null;
            }

            return (JFunc) ost;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void runCompiled(String fn, JFunc func, String[] args) {
        runCompiled(fn, func, args, new HashMap<>());
    }

    public static void runCompiled(String fn, JFunc func, String[] args, Map<String, Var> globals) {
        vm = new VM(func, globals).trace(fn);
        VMResult res = vm.run();
        if (res == VMResult.ERROR) return;
        vm.finish(args);
    }

    public static Error runCompiled(String fn, String inpath, String[] args) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(inpath);
        } catch (FileNotFoundException e) {
            return RTError.FileNotFound(null, null,
                    "File does not exist!\n" + inpath, null);
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object ost = ois.readObject();
            ois.close();
            fis.close();
            if (!(ost instanceof JFunc)) return RTError.FileNotFound(null, null,
                    "File is not JPizza bytecode!", null);

            JFunc func = (JFunc) ost;
            vm = new VM(func).trace(fn);

            VMResult res = vm.run();
            if (res == VMResult.ERROR) return null;
            vm.finish(args);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Another public static 
    public static Pair<ClassInstance, Error> imprt(String fn, String text) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Pair<>(null, ast.b);
        Context context = new Context(fn, null, null);
        context.symbolTable = new SymbolTable(globalSymbolTable);
        RTResult result = new Interpreter().interpret(ast.a, context, false);
        return new Pair<>(new ClassInstance(context, fn), result.error);
    }

}
