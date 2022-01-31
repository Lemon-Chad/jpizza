package lemon.jpizza;

import lemon.jpizza.compiler.ChunkBuilder;
import lemon.jpizza.compiler.Compiler;
import lemon.jpizza.compiler.FunctionType;
import lemon.jpizza.compiler.values.Var;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.compiler.vm.VMResult;
import lemon.jpizza.errors.Error;
import lemon.jpizza.generators.Lexer;
import lemon.jpizza.generators.Optimizer;
import lemon.jpizza.generators.Parser;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.expressions.BodyNode;
import lemon.jpizza.results.ParseResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Shell {

    public static final Logger logger = new Logger();
    public static String root;
    public static VM vm;
    public static final Map<String, Var> globals = new HashMap<>();
    public static final String fileEncoding = System.getProperty("file.encoding");

    static class Flags {
        public static final int COMPILE   = 0b00000001;
        public static final int REFACTOR  = 0b00000100;
        public static final int HELP      = 0b00001000;
        public static final int VERSION   = 0b00010000;
        public static final int DOCS      = 0b00100000;
        public static final int OUTPUT    = 0b01000000;
        public static final int RUN       = 0b10000000;
        public static final int SHELL     = 0b00000000;
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

    static boolean hasFlag(int target, int flag) {
        return (target & flag) == flag;
    }

    @SuppressWarnings("DuplicatedCode")
    public static void main(String[] args) throws IOException {
        root = System.getenv("JPIZZA_DATA_DIR") == null ? System.getProperty("user.home") + "/.jpizza" : System.getenv("JPIZZA_DATA_DIR");

        int flags = Flags.SHELL;
        String to = null;
        String target = null;
        String refactor_to = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                switch (args[i].substring(2)) {
                    case "compile" -> {
                        if (i + 1 < args.length) {
                            target = args[i + 1];
                            i++;
                        } else {
                            Shell.logger.fail("-c requires an argument");
                        }
                        flags |= Flags.COMPILE;
                    }
                    case "refactor" -> {
                        if (i + 1 < args.length) {
                            refactor_to = args[i + 1];
                            i++;
                        } else {
                            Shell.logger.fail("-R requires an argument");
                        }
                        flags |= Flags.COMPILE;
                    }
                    case "help" -> flags |= Flags.HELP;
                    case "version" -> flags |= Flags.VERSION;
                    case "docs" -> flags |= Flags.DOCS;
                    case "output" -> {
                        if (i + 1 < args.length) {
                            to = args[i + 1];
                            i++;
                        } else {
                            Shell.logger.fail("-o requires an argument");
                        }
                        flags |= Flags.OUTPUT;
                    }
                    default -> Shell.logger.fail("Unknown argument: " + args[i]);
                }
            } else if (args[i].startsWith("-")) {
                String shortArg = args[i];
                for (int c = 1; c < shortArg.length(); c++) {
                    switch (String.valueOf(shortArg.charAt(c))) {
                        case "c" -> {
                            if (i + 1 < args.length) {
                                target = args[i + 1];
                                i++;
                            } else {
                                Shell.logger.fail("-c requires an argument");
                            }
                            flags |= Flags.COMPILE;
                        }
                        case "R" -> {
                            if (i + 1 < args.length) {
                                refactor_to = args[i + 1];
                                i++;
                            } else {
                                Shell.logger.fail("-R requires an argument");
                            }
                            flags |= Flags.COMPILE;
                        }
                        case "h" -> flags |= Flags.HELP;
                        case "v" -> flags |= Flags.VERSION;
                        case "o" -> {
                            if (i + 1 < args.length) {
                                to = args[i + 1];
                                i++;
                            } else {
                                Shell.logger.fail("-o requires an argument");
                            }
                            flags |= Flags.OUTPUT;
                        }
                        default -> Shell.logger.fail("Unknown argument: " + shortArg.charAt(c));
                    }
                }
            } else {
                if (args.length > 1) Shell.logger.fail("Invalid argument: " + args[i]);
                else {
                    target = args[i];
                    flags = Flags.RUN;
                }
            }
        }

        if (flags == Flags.SHELL) repl();

        if (hasFlag(flags, Flags.REFACTOR)) {
            Shell.logger.enableTips();
        }

        if (hasFlag(flags, Flags.HELP)) {
            Shell.logger.outln("Usage: jpizza [options] [target]");
            Shell.logger.outln("Options:");
            Shell.logger.outln("  -c, --compile [target]\t\tCompile target");
            Shell.logger.outln("  -o, --output [target]\tOutput target to file");
            Shell.logger.outln("  -R, --refactor [target]\t\tRefactor target");
            Shell.logger.outln("  -h, --help\t\tPrint this help message");
            Shell.logger.outln("  -v, --version\t\tPrint version");
            Shell.logger.outln("  --docs\t\tPrint link to documentation");
            Shell.logger.outln("You can pass in a single compiled file as an argument to run it.");
        }
        if (hasFlag(flags, Flags.VERSION)) {
            Shell.logger.outln("jpizza version " + VM.VERSION);
        }
        if (hasFlag(flags, Flags.DOCS)) {
            Shell.logger.outln("Documentation can be found at https://jpizza.readthedocs.io/en/latest/");
        }
        if (hasFlag(flags, Flags.COMPILE)) {
            if (Files.exists(Path.of(target))) {
                String scrpt = Files.readString(Path.of(target));
                String dir = Path.of(target).toString();
                String[] dsfn = getFNDirs(dir);
                String fn = dsfn[0];
                String newDir = dsfn[1];
                System.setProperty("user.dir", newDir);
                to = to == null ? newDir + "\\" + fn.substring(0, fn.length() - 5) + ".jbox" : to;
                Error e = compile(fn, scrpt, to);
                if (e != null)
                    Shell.logger.fail(e.asString());
            }
        }
        if (hasFlag(flags, Flags.RUN)) {
            if (Files.exists(Path.of(args[0]))) {
                String dir = Path.of(args[0]).toString();
                String[] dsfn = getFNDirs(dir);
                String fn = dsfn[0];
                String newDir = dsfn[1];
                System.setProperty("user.dir", newDir);
                runCompiled(fn, args[0], args);
            } else {
                Shell.logger.fail("File does not exist.");
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
        System.exit(0);
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
        BodyNode body = (BodyNode) Optimizer.optimize(ast.node);
        return new Pair<>(body.statements, null);
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
            fout.write(func.dumpBytes());
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
            return new Error(
                    null, null,
                    "Internal",
                    "Could not write to file"
            );
        }

        return null;
    }

    public static JFunc load(String inpath) {
        try {
            Path path = Paths.get(inpath);
            if (!Files.exists(path)) {
                Shell.logger.fail("File does not exist!");
            }
            byte[] arr = Files.readAllBytes(path);
            return ChunkBuilder.Build(arr);
        } catch (IOException e) {
            Shell.logger.fail("File is not readable!");
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

    public static void runCompiled(String fn, String inpath, String[] args) {
        try {
            Path path = Paths.get(inpath);
            if (!Files.exists(path)) {
                Shell.logger.fail("File does not exist!");
            }
            byte[] arr = Files.readAllBytes(path);

            JFunc func = ChunkBuilder.Build(arr);
            vm = new VM(func).trace(fn);

            VMResult res = vm.run();
            if (res == VMResult.ERROR) return;
            vm.finish(args);

        } catch (IOException e) {
            Shell.logger.fail("File is not readable!");
        }
    }

}
