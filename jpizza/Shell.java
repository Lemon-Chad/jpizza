package lemon.jpizza;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Generators.Lexer;
import lemon.jpizza.Generators.Parser;
import lemon.jpizza.Libraries.BuiltInFunction;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Executables.ClassInstance;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.PList;
import lemon.jpizza.Results.ParseResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import lemon.jpizza.Errors.Error;
import lemon.jpizza.Results.RTResult;

public class Shell {

    static SymbolTable globalSymbolTable = new SymbolTable();

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);

        // Load librarys

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

        if (args.length == 1) {
            if (args[0].equals("help")) {
                System.out.println("""
                        dp -> Open venv
                        dp help -> List commands
                        dp docs -> Link to documentation
                        dp <file> -> Run file
                        """);
            } else if (args[0].endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    String scrpt = Files.readString(Path.of(args[0]));
                    Double res = run(args[0], scrpt);
                    if (res.get(1) != null)
                        System.out.println(((Error) res.get(1)).asString());
                } else {
                    System.out.println("File does not exist.");
                }
            } else if (args[0].equals("docs")) {
                System.out.println("Documentation: https://bit.ly/3vM8G0a");
            }
            return;
        }

        while (true) {
            System.out.print("-> "); String input = in.nextLine();
            if (input.equals("quit"))
                break;
            Double a = run("<shell>", input);
            if (a.get(1) != null) System.out.println(((Error) a.get(1)).asString());
            else {
                List<Obj> results = (List<Obj>) ((PList) a.get(0)).value;
                if (results.size() > 0) {
                    StringBuilder out = new StringBuilder();
                    int size = results.size();
                    for (int i = 0; i < size; i++) {
                        if (!(results.get(i) instanceof Null)) out.append(results.get(i)).append(", ");
                    }
                    if (out.length() > 0) out.setLength(out.length() - 2);
                    System.out.println(out);
                }
            }
        }
    }

    public static Double run(String fn, String text) {
        Lexer lexer = new Lexer(fn, text);
        Double x = lexer.make_tokens();
        @SuppressWarnings("unchecked") List<Token> tokens = (List<Token>) x.get(0);
        Object error = x.get(1);
        if (error != null)
            return new Double(null, error);
        Parser parser = new Parser(tokens);
        ParseResult ast = parser.parse();
        if (ast.error != null)
            return new Double(null, ast.error);
        Context context = new Context(fn, null, null);
        context.symbolTable = globalSymbolTable;
        RTResult result = new Interpreter().visit((Node) ast.node, context);
        return new Double(result.value, result.error);
    }

    public static Double imprt(String fn, String text, Context parent, Position entry_pos) {
        Lexer lexer = new Lexer(fn, text);
        Double x = lexer.make_tokens();
        @SuppressWarnings("unchecked") List<Token> tokens = (List<Token>) x.get(0);
        Object error = x.get(1);
        if (error != null)
            return new Double(null, error);
        Parser parser = new Parser(tokens);
        ParseResult ast = parser.parse();
        if (ast.error != null)
            return new Double(null, ast.error);
        Context context = new Context(fn, parent, entry_pos);
        context.symbolTable = globalSymbolTable;
        RTResult result = new Interpreter().visit((Node) ast.node, context);
        return new Double(new ClassInstance(context), result.error);
    }

}
