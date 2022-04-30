package lemon.jpizza.compiler;

import lemon.jpizza.*;
import lemon.jpizza.cases.Case;
import lemon.jpizza.compiler.headers.HeadCode;
import lemon.jpizza.compiler.types.GenericType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.objects.*;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.enums.JEnum;
import lemon.jpizza.compiler.values.enums.JEnumChild;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.LibraryManager;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.compiler.vm.VMResult;
import lemon.jpizza.errors.Error;
import lemon.jpizza.generators.Parser;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.definitions.*;
import lemon.jpizza.nodes.expressions.*;
import lemon.jpizza.nodes.operations.BinOpNode;
import lemon.jpizza.nodes.operations.UnaryOpNode;
import lemon.jpizza.nodes.values.*;
import lemon.jpizza.nodes.variables.AttrAccessNode;
import lemon.jpizza.nodes.variables.VarAccessNode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static lemon.jpizza.Constants.readString;

public class Compiler {

    static class LocalToken {
        String name;
        int idx;
        int len;

        public LocalToken(String name, int idx, int len) {
            this.name = name;
            this.idx = idx;
            this.len = len;
        }
    }
    static class Local {
        final LocalToken name;
        final Type type;
        int depth;

        Local(LocalToken name, Type type, int depth) {
            this.name = name;
            this.type = type;
            this.depth = depth;
        }

    }

    static class Upvalue {
        boolean isLocal;
        boolean isGlobal;

        String globalName;
        int index;
        final Type type;

        public Upvalue(int index, boolean isLocal, Type type) {
            this.index = index;
            this.isLocal = isLocal;
            this.isGlobal = false;
            this.type = type;
        }

        public Upvalue(String globalName, Type type) {
            this.isGlobal = true;
            this.globalName = globalName;
            this.type = type;
        }
    }

    final Compiler enclosing;

    boolean inPattern = false;
    Type patternType = Types.VOID;

    final Local[] locals;
    final Local[] generics;
    int localCount;
    int scopeDepth;

    final JFunc function;
    final FunctionType type;
    final FuncType funcType;

    final Stack<Integer> continueTo;
    final Stack<List<Integer>> breaks;

    final Map<String, Type> globals;

    String packageName;
    String target;

    final Upvalue[] upvalues;

    boolean catchErrors = false;

    Map<String, Node> macros;
    Map<String, Type> macroTypes;

    Type enclosingType;
    boolean staticContext;
    
    TypeLookup typeHandler;

    public Chunk chunk() {
        return this.function.chunk;
    }
    
    public Compiler(FunctionType type, String source, FuncType funcType) {
        this(null, type, source, funcType);
        LibraryManager.Setup(null);
        globals.putAll(Shell.globals);
    }

    public Compiler(Compiler enclosing, FunctionType type, String source, FuncType funcType) {
        this.function = new JFunc(source);
        this.type = type;
        this.funcType = funcType;

        if (enclosing != null) {
            this.enclosingType = enclosing.enclosingType;
        }
        else {
            this.enclosingType = Types.VOID;
        }

        this.locals = new Local[VM.MAX_STACK_SIZE];
        this.generics = new Local[VM.MAX_STACK_SIZE];
        this.globals = new HashMap<>();

        this.upvalues = new Upvalue[256];

        this.localCount = 0;
        this.scopeDepth = 0;

        locals[localCount++] = new Local(new LocalToken(type == FunctionType.Method || type == FunctionType.Constructor ? "this" : "", 0, 0), this.enclosingType, 0);

        this.enclosing = enclosing;

        this.continueTo = new Stack<>();
        this.breaks = new Stack<>();

        this.macros = new HashMap<>();
        this.macroTypes = new HashMap<>();
        
        this.typeHandler = new TypeLookup(this);
    }

    public void beginScope() {
        this.scopeDepth++;
    }

    public void endScope(@NotNull Position start, @NotNull Position end) {
        destack(locals, start, end);
        destack(generics, start, end);
        scopeDepth--;
    }

    void error(String type, String message, Position start, Position end) {
        Error error = new Error(start, end, type + " Error", message);
        Shell.logger.fail(error.asString());
    }

    int destack(Local[] locals) {
        int offs = 0;
        int count = 0;
        int localCount = this.localCount;
        while (localCount - offs > 0) {
            Local curr = locals[localCount - 1 - offs];
            if (curr == null) {
                offs++;
                continue;
            }
            if (curr.depth != scopeDepth) {
                break;
            }
            count++;
            localCount--;
        }
        return count;
    }

    void destack(Local[] locals, @NotNull Position start, @NotNull Position end) {
        int count = destack(locals);
        for (int i = 0; i < count; i++) {
            emit(OpCode.Pop, start, end);
            localCount--;
        }
    }

    int resolve(String name, Local[] locals) {
        for (int i = 0; i < localCount; i++) {
            Local local = locals[localCount - 1 - i];
            if (local == null) continue;
            if (local.name.name.equals(name)) return localCount - 1 - i;
        }
        return -1;
    }

    Type resolveType(String name, Local[] locals) {
        for (int i = 0; i < localCount; i++) {
            Local local = locals[localCount - 1 - i];
            if (local == null) continue;
            if (local.name.name.equals(name)) return locals[localCount - 1 - i].type;
        }
        return null;
    }

    int resolveLocal(String name) {
        return resolve(name, locals);
    }

    Type resolveLocalType(String name) {
        return resolveType(name, locals);
    }

    int addUpvalue(int index, boolean isLocal, Type type) {
        int upvalueCount = function.upvalueCount;

        for (int i = 0; i < upvalueCount; i++) {
            Upvalue upvalue = upvalues[i];
            if (upvalue.index == index && upvalue.isLocal == isLocal) {
                return i;
            }
        }

        upvalues[upvalueCount] = new Upvalue(index, isLocal, type);
        return function.upvalueCount++;
    }

    Type resolveUpvalueType(String name) {
        if (enclosing == null) return null;

        Type local = enclosing.resolveLocalType(name);
        if (local != null) {
            return local;
        }

        Type upvalue = enclosing.resolveUpvalueType(name);
        if (upvalue != null) {
            return upvalue;
        }

        return hasGlobal(name) ? getGlobal(name) : null;
    }

    int addUpvalue(String name) {
        int upvalueCount = function.upvalueCount;

        for (int i = 0; i < upvalueCount; i++) {
            Upvalue upvalue = upvalues[i];
            if (Objects.equals(upvalue.globalName, name) && upvalue.isGlobal) {
                return i;
            }
        }

        upvalues[upvalueCount] = new Upvalue(name, getGlobal(name));
        return function.upvalueCount++;
    }

    boolean hasGlobal(String name) {
        return globals.containsKey(name) || enclosing != null && enclosing.hasGlobal(name);
    }

    Type getGlobal(String name) {
        return globals.getOrDefault(name, enclosing != null ? enclosing.getGlobal(name) : null);
    }

    int resolveUpvalue(String name) {
        if (enclosing == null) return -1;

        int local = enclosing.resolveLocal(name);
        if (local != -1) {
            return addUpvalue(local, true, enclosing.resolveLocalType(name));
        }

        int upvalue = enclosing.resolveUpvalue(name);
        if (upvalue != -1) {
            return addUpvalue(upvalue, false, enclosing.resolveUpvalueType(name));
        }

        return hasGlobal(name) ? addUpvalue(name) : -1;
    }

    void patchBreaks() {
        for (int i : breaks.pop()) {
            patchJump(i);
        }
    }

    void emit(int b, @NotNull Position start, @NotNull Position end) {
        chunk().write(b, start.idx, end.idx - start.idx);
    }

    void emit(int[] bs, @NotNull Position start, @NotNull Position end) {
        for (int b : bs) 
            chunk().write(b, start.idx, end.idx - start.idx);
    }

    void emit(int op, int b, @NotNull Position start, @NotNull Position end) {
        chunk().write(op, start.idx, end.idx - start.idx);
        chunk().write(b, start.idx, end.idx - start.idx);
    }

    int emitJump(int op, @NotNull Position start, @NotNull Position end) {
        emit(op, start, end);
        emit(0xff, start, end);
        return chunk().code.size() - 1;
    }

    void emitLoop(int loopStart, @NotNull Position start, @NotNull Position end) {
        emit(OpCode.Loop, start, end);

        int offset = chunk().code.size() - loopStart + 1;

        emit(offset, start, end);
    }

    void patchJump(int offset) {
        int jump = chunk().code.size() - offset - 1;
        chunk().code.set(offset, jump);
    }

    Type accessEnclosed(String name) {
        return staticContext ? enclosingType.access(name) : enclosingType.accessInternal(name);
    }

    public JFunc compileBlock(List<Node> statements) {
        for (Node statement : statements) {
            compile(statement);
            emit(OpCode.Pop, statement.pos_start, statement.pos_end);
        }

        emit(OpCode.Return, statements.get(0).pos_start, statements.get(statements.size() - 1).pos_end);

        return endCompiler();
    }

    public JFunc endCompiler() {
        if (Shell.logger.debug)
            Disassembler.disassembleChunk(chunk(), function.name != null ? function.name : "<script>");
        function.chunk.compile();
        return function;
    }

    Type compile(Node statement) {
        switch (statement.jptype) {
            case Cast:
                compile(((CastNode) statement).expr);
                break;

            case BinOp:
                compile((BinOpNode) statement);
                break;
            case UnaryOp:
                compile((UnaryOpNode) statement);
                break;

            case Use:
                compile((UseNode) statement);
                break;
            case Import:
                compile((ImportNode) statement);
                break;
            case Extend:
                compile((ExtendNode) statement);
                break;
            case Destruct:
                compile((DestructNode) statement);
                break;

            case Decorator:
                compile((DecoratorNode) statement);
                break;
            case FuncDef:
                compile((FuncDefNode) statement);
                break;
            case Call:
                compile((CallNode) statement);
                break;
            case Return:
                compile((ReturnNode) statement);
                break;
            case Spread:
                compile((SpreadNode) statement);
                break;

            case Number: {
                NumberNode node = (NumberNode) statement;
                compileNumber(node.val, node.pos_start, node.pos_end);
                break;
            }
            case String: {
                StringNode node = (StringNode) statement;
                compileString(node.val, node.pos_start, node.pos_end);
                break;
            }
            case Boolean: {
                BooleanNode node = (BooleanNode) statement;
                compileBoolean(node.val, node.pos_start, node.pos_end);
                break;
            }
            case List:
                compile((ListNode) statement);
                break;
            case Dict:
                compile((DictNode) statement);
                break;
            case Null:
            case Pass:
                compileNull(statement.pos_start, statement.pos_end);
                break;
            case Bytes:
                compile((BytesNode) statement);
                break;

            case Body: {
                BodyNode node = (BodyNode) statement;
                for (Node stmt : node.statements) {
                    compile(stmt);
                    emit(OpCode.Pop, stmt.pos_start, stmt.pos_end);
                }
                compileNull(node.pos_start, node.pos_end);
                break;
            }
            case Scope:
                compile((ScopeNode) statement);
                break;

            case Enum:
                return compile((EnumNode) statement);
            case ClassDef:
                return compile((ClassDefNode) statement);
            case Claccess: {
                ClaccessNode node = (ClaccessNode) statement;
                compile(node.class_tok);
                String attr = node.attr_name_tok.value.toString();
                int constant = chunk().addConstant(new Value(attr));
                emit(OpCode.Access, constant, node.pos_start, node.pos_end);
                break;
            }
            case AttrAssign: {
                AttrAssignNode node = (AttrAssignNode) statement;
                compile(node.value_node);
                int constant = chunk().addConstant(new Value(node.var_name_tok.value.toString()));
                emit(OpCode.SetAttr, constant, node.pos_start, node.pos_end);
                break;
            }
            case AttrAccess: {
                AttrAccessNode node = (AttrAccessNode) statement;
                String attr = node.var_name_tok.value.toString();
                int constant = chunk().addConstant(new Value(attr));
                emit(OpCode.GetAttr, constant, node.pos_start, node.pos_end);
                break;
            }

            case VarAssign:
                compile((VarAssignNode) statement);
                break;
            case DynAssign:
                compile((DynAssignNode) statement);
                break;
            case Let:
                compile((LetNode) statement);
                break;

            case VarAccess:
                compile((VarAccessNode) statement);
                break;
            case Drop:
                compile((DropNode) statement);
                break;

            case Throw:
                compile((ThrowNode) statement);
                break;
            case Assert:
                compile((AssertNode) statement);
                break;

            // This is an if statement
            case Query:
                compile((QueryNode) statement);
                break;
            case Switch:
                compile((SwitchNode) statement);
                break;
            case Pattern:
                compile((PatternNode) statement);
                break;


            case While:
                compile((WhileNode) statement);
                break;
            case For:
                compile((ForNode) statement);
                break;
            case Iter:
                compile((IterNode) statement);
                break;
            case Break:
                if (breaks.isEmpty())
                    error("Invalid Syntax", "Break statement outside of loop", statement.pos_start, statement.pos_end);
                compileNull(statement.pos_start, statement.pos_end);
                breaks.peek().add(emitJump(OpCode.Jump, statement.pos_start, statement.pos_end));
                break;
            case Continue:
                emitLoop(continueTo.peek(), statement.pos_start, statement.pos_end);
                break;

            case Ref:
                compile((RefNode) statement);
                break;
            case Deref:
                compile((DerefNode) statement);
                break;

            default:
                throw new RuntimeException("Unknown statement type: " + statement.jptype);
        }
        return typeHandler.resolve(statement);
    }

    void compile(ExtendNode node) {
        String name = node.file_name_tok.value.toString();
        bootExtend(name, (reason, message) -> error(reason, message, node.pos_start, node.pos_end), null);
        emit(OpCode.Extend, chunk().addConstant(new Value()), node.pos_start, node.pos_end);
    }

    void compile(DecoratorNode node) {
        Type decorated = compile(node.decorated);
        Type decorator = compile(node.decorator);
        if (!(decorator instanceof FuncType) || !(decorated instanceof FuncType)) {
            error("Decorator", "Decorator and decorated must be a function", node.pos_start, node.pos_end);
        }
        Type result = decorator.call(new Type[]{ decorated }, new Type[0]);
        if (!decorated.equals(result)) {
            error("Decorator", "Decorator must return the decorated function", node.pos_start, node.pos_end);
        }
        emit(new int[]{
                OpCode.Call,
                1, 0
        }, node.pos_start, node.pos_end);
        String name = node.name.value.toString();
        int arg = resolveLocal(name);

        if (arg != -1) {
            emit(OpCode.SetLocal, arg, node.pos_start, node.pos_end);
        }
        else if ((arg = resolveUpvalue(name)) != -1) {
            emit(OpCode.SetUpvalue, arg, node.pos_start, node.pos_end);
        }
        else {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.SetGlobal, arg, node.pos_start, node.pos_end);
        }
    }

    void compile(PatternNode node) {
        Type target = compile(node.accessNode);
        if (!(target instanceof ClassType) && !(target instanceof EnumChildType)) {
            error("Pattern", "Pattern must be a class or enum", node.pos_start, node.pos_end);
        }
        inPattern = true;
        Token[] keySet = node.patterns.keySet().toArray(new Token[0]);
        for (Token token : keySet) {
            String name = token.value.toString();
            patternType = target.accessInternal(name);
            if (patternType == null) {
                error("Pattern", "Attribute does not exist", node.pos_start, node.pos_end);
            }
            compile(node.patterns.get(token));
        }
        inPattern = false;
        patternType = Types.VOID;
        emit(OpCode.Pattern, keySet.length, node.pos_start, node.pos_end);
        for (int i = keySet.length - 1; i >= 0; i--) {
            Token token = keySet[i];
            int constant = chunk().addConstant(new Value(token.value.toString()));
            emit(constant, token.pos_start, token.pos_end);
        }
    }

    void compile(DestructNode node) {
        Type destructed = compile(node.target);
        emit(OpCode.Destruct, node.glob ? -1 : node.subs.size(), node.pos_start, node.pos_end);
        if (node.glob) {
            List<String> names = destructed.accessors();
            if (names == null) {
                error("Destruct", "Destructing a non-destructable type", node.pos_start, node.pos_end);
            }
            for (String name : names) {
                Type type = destructed.access(name);
                if (type == null) {
                    error("Attribute", "Cannot access " + name + " in " + destructed, node.pos_start, node.pos_end);
                }
                globals.put(name, type);
            }
        } else for (Token sub : node.subs) {
            String name = sub.value.toString();
            Type type = destructed.access(name);
            if (type == null) {
                error("Attribute", "Cannot access " + name + " in " + destructed, node.pos_start, node.pos_end);
            }
            globals.put(name, type);
            emit(chunk().addConstant(new Value(name)), sub.pos_start, sub.pos_end);
        }
    }

    void compile(UseNode node) {
        int code;
        int argc;
        String name;

        switch (name = node.useToken.value.toString()) {
            case "memoize":
                argc = 0;
                code = HeadCode.Memoize;
            break;

            case "func":
                argc = 1;
                code = HeadCode.SetMainFunction;
            break;

            case "object":
                argc = 1;
                code = HeadCode.SetMainClass;
            break;

            case "export":
                argc = -1;
                code = HeadCode.Export;
            break;

            case "package":
                argc = -1;
                StringBuilder sb = new StringBuilder();
                for (Token token : node.args) {
                    sb.append(token.asString()).append(".");
                }
                packageName = sb.substring(0, sb.length() - 1);
                chunk().packageName = packageName;
                code = HeadCode.Package;
            break;

            case "export_to":
                argc = 1;
                if (node.args.size() == 1) {
                    target = node.args.get(0).asString();
                    chunk().target = target;
                }
                code = HeadCode.ExportTo;
            break;

            default:
                code = -1;
                argc = -1;
            break;
        }

        if (code == -1) {
            error("Header Type", "Header does not exist", node.useToken.pos_start, node.useToken.pos_end);
        }

        if (node.args.size() != argc && argc != -1) {
            error("Argument Count", name + "() takes exactly " + argc + " arguments", node.pos_start, node.pos_end);
        }

        emit(OpCode.Header, node.pos_start, node.pos_end);
        emit(code, node.args.size(), node.pos_start, node.pos_end);
        for (Token arg : node.args) {
            int constant = chunk().addConstant(new Value(arg.value.toString()));
            emit(constant, arg.pos_start, arg.pos_end);
        }
    }

    void compile(DynAssignNode node) {
        macros.put(
                node.var_name_tok.value.toString(),
                node.value_node
        );
        macroTypes.put(
                node.var_name_tok.value.toString(),
                typeHandler.resolve(node.value_node)
        );
        compileNull(node.pos_start, node.pos_end);
    }

    void compile(BytesNode node) {
        compile(node.toBytes);
        emit(OpCode.ToBytes, node.pos_start, node.pos_end);
    }

    void compile(DerefNode node) {
        Type type = compile(node.ref);
        if (!(type instanceof ReferenceType)) {
            error("Type", "Cannot dereference " + type, node.pos_start, node.pos_end);
        }
        emit(OpCode.Deref, node.pos_start, node.pos_end);
    }

    void compile(RefNode node) {
        compile(node.inner);
        emit(OpCode.Ref, node.pos_start, node.pos_end);
    }

    void compile(SpreadNode node) {
        compile(node.internal);
        emit(OpCode.Spread, node.pos_start, node.pos_end);
    }

    Type compile(EnumNode node) {
        Map<String, JEnumChild> children = new HashMap<>();
        EnumChildType[] types = new EnumChildType[node.children.size()];

        int argc = node.children.size();
        for (int i = 0; i < argc; i++) {
            Parser.EnumChild child = node.children.get(i);

            String name = child.token().value.toString();
            children.put(name, new JEnumChild(
                    i,
                    child.params()
            ));

            GenericType[] generics = new GenericType[child.generics().size()];
            Set<String> removeLater = new HashSet<>();
            for (int j = 0; j < generics.length; j++) {
                generics[j] = new GenericType(child.generics().get(j));
                if (!typeHandler.types.containsKey(generics[j].name)) {
                    removeLater.add(generics[j].name);
                    typeHandler.types.put(generics[j].name, generics[j]);
                }
            }

            String[] properties = child.params().toArray(new String[0]);
            Type[] propertyTypes = new Type[child.types().size()];
            for (int j = 0; j < propertyTypes.length; j++) {
                Type type = typeLookup(child.types().get(j), child.token().pos_start, child.token().pos_end);
                if (type == null) {
                    error("Type", "Type does not exist", node.pos_start, node.pos_end);
                }
                propertyTypes[j] = type;
            }

            EnumChildType type = new EnumChildType(name, propertyTypes, generics, properties);
            types[i] = type;

            for (String generic : removeLater) {
                typeHandler.types.remove(generic);
            }

            if (node.pub)
                globals.put(name, type);
        }

        String name = node.tok.value.toString();
        EnumType type = new EnumType(name, types);
        globals.put(name, type);
        int constant = chunk().addConstant(new Value(new JEnum(
                name,
                children
        )));
        emit(new int[]{ OpCode.Enum, constant, node.pub ? 1 : 0 }, node.pos_start, node.pos_end);
        return type;
    }

    private Type typeLookup(Token token) {
        return typeLookup((List<String>) token.value, token.pos_start, token.pos_end);
    }

    private Type typeLookup(List<String> strings, Position start, Position end) {
        return typeHandler.resolve(strings, start, end);
    }


    Type variableType(String toString, Position start, Position end) {
        // TODO: THIS TOO
        int index;
        if (hasGlobal(toString)) {
            return getGlobal(toString);
        }
        else if ((index = resolveLocal(toString)) != -1) {
            return locals[index].type;
        }
        else if ((index = resolveUpvalue(toString)) != -1) {
            return upvalues[index].type;
        }
        else if (macroTypes.containsKey(toString)) {
            return macroTypes.get(toString);
        }
        else if (accessEnclosed(toString) != null) {
            return accessEnclosed(toString);
        }
        error("Scope", "Variable " + toString + " does not exist", start, end);
        return null;
    }

    static boolean equalPackages(String a, String b) {
        if (a == null || b == null)
            return a == null && b == null;
        return a.startsWith(b) || b.startsWith(a) || a.equals(b);
    }

    JFunc canImport(JFunc func) throws IOException {
        if (func == null || func.chunk == null)
            throw new IOException("Failed to load file");
        Chunk chunk = func.chunk;
        if (chunk.target != null) {
            String target = chunk.target;
            if (Objects.equals(target, "package")) {
                if (!equalPackages(packageName, chunk.packageName))
                    throw new IOException("Cannot import file outside of package");
            }
            else if (!target.equals("all")) {
                throw new IOException("File is not a module");
            }
        }
        return func;
    }

    JFunc getImport(ImportNode node) {
        String fn = node.file_name_tok.asString();
        String chrDir = System.getProperty("user.dir");

        String fileName = chrDir + "/" + fn;

        String modPath = Shell.root + "/modules/" + fn;
        String modFilePath = modPath + "/" + fn;

        //noinspection ResultOfMethodCallIgnored
        new File(Shell.root + "/modules").mkdirs();

        JFunc imp;
        try {
            if (Constants.STANDLIBS.containsKey(fn)) {
                Pair<JFunc, Error> res = Shell.compile(fn, Constants.STANDLIBS.get(fn));
                if (res.b != null)
                    Shell.logger.fail(res.b.asString());
                imp = res.a;
            }
            else if (Files.exists(Paths.get(modFilePath + ".jbox"))) {
                imp = canImport(Shell.load(readString(Paths.get(modFilePath + ".jbox"))));
            }
            else if (Files.exists(Paths.get(fileName + ".jbox"))) {
                imp = canImport(Shell.load(readString(Paths.get(fileName + ".jbox"))));
            }
            else if (Files.exists(Paths.get(fileName + ".devp"))) {
                //noinspection DuplicatedCode
                Pair<JFunc, Error> res = Shell.compile(fn, readString(Paths.get(fileName + ".devp")));
                if (res.b != null)
                    Shell.logger.fail(res.b.asString());
                imp = canImport(res.a);
                System.setProperty("user.dir", chrDir);
            }
            else if (Files.exists(Paths.get(fn + ".devp"))) {
                String[] split = Shell.getFNDirs(fn);
                System.setProperty("user.dir", split[1]);
                Pair<JFunc, Error> res = Shell.compile(split[0], readString(Paths.get(fn + ".devp")));
                if (res.b != null)
                    Shell.logger.fail(res.b.asString());
                imp = canImport(res.a);
                System.setProperty("user.dir", chrDir);
            }
            else if (Files.exists(Paths.get(fn + ".jbox"))) {
                String[] split = Shell.getFNDirs(fn);
                System.setProperty("user.dir", split[1]);
                imp = canImport(Shell.load(readString(Paths.get(fn + ".jbox"))));
                System.setProperty("user.dir", chrDir);
            }
            else if (Files.exists(Paths.get(modFilePath + ".devp"))) {
                System.setProperty("user.dir", modPath);
                //noinspection DuplicatedCode
                Pair<JFunc, Error> res = Shell.compile(fn, readString(Paths.get(modFilePath + ".devp")));
                if (res.b != null)
                    Shell.logger.fail(res.b.asString());
                imp = canImport(res.a);
                System.setProperty("user.dir", chrDir);
            }
            else if (Shell.libraries.containsKey(fn)) {
                imp = null;
            }
            else {
                throw new IOException("File not found");
            }
        } catch (IOException e) {
            imp = null;
            error("Import", "Couldn't import file (" + e.getMessage() + ")", node.pos_start, node.pos_end);
        }

        return imp;
    }

    void compile(ImportNode node) {
        String fn = node.file_name_tok.asString();

        JFunc imp = getImport(node);
        Type type = null;

        if (imp != null) {
            int addr = chunk().addConstant(new Value(imp));
            emit(OpCode.Constant, addr, node.pos_start, node.pos_end);
            type = new NamespaceType(imp.chunk.globals);
        }
        else {
            if (Shell.libraries.containsKey(fn)) {
                type = Shell.libraries.get(fn);
            }
            compileNull(node.pos_start, node.pos_end);
        }

        if (type == null) {
            error("Import", "Couldn't import file", node.pos_start, node.pos_end);
        }

        int constant = chunk().addConstant(new Value(fn));
        emit(OpCode.Import, constant, node.pos_start, node.pos_end);
        globals.put(node.as_tok != null ? node.as_tok.value.toString() : fn, type);

        if (node.as_tok != null)
            constant = chunk().addConstant(new Value(node.as_tok.value.toString()));
        emit(constant, node.pos_start, node.pos_end);
    }

    void compile(AssertNode node) {
        compile(node.condition);
        emit(OpCode.Assert, node.pos_start, node.pos_end);
    }

    void compile(ThrowNode node) {
        compile(node.thrown);
        compile(node.throwType);
        emit(OpCode.Throw, node.pos_start, node.pos_end);
    }

    void compile(ReturnNode node) {
        if (node.nodeToReturn != null) {
            compile(node.nodeToReturn);
        }
        else {
            compileNull(node.pos_start, node.pos_end);
        }
        emit(OpCode.Return, node.pos_start, node.pos_end);
    }

    void compile(CallNode node) {
        int argc = node.argNodes.size();
        int kwargc = node.kwargs.size();
        Type[] argTypes = new Type[argc];
        for (int i = 0; i < argc; i++) {
            argTypes[i] = compile(node.argNodes.get(i));
        }
        List<String> kwargNames = new ArrayList<>(node.kwargs.keySet());
        for (int i = kwargc - 1; i >= 0; i--) {
            compile(node.kwargs.get(kwargNames.get(i)));
        }
        Type function = compile(node.nodeToCall);
        if (!function.callable()) {
            error("Type", "Can't call non-function", node.pos_start, node.pos_end);
        }
        emit(new int[]{
                OpCode.Call,
                argc, kwargc
        }, node.pos_start, node.pos_end);
        for (int i = 0; i < kwargc; i++) {
            emit(chunk().addConstant(new Value(kwargNames.get(i))), node.pos_start, node.pos_end);
        }
        Type[] generics = new Type[node.generics.size()];
        for (int i = 0; i < generics.length; i++) {
            Token generic = node.generics.get(i);
            Type type = typeLookup(generic);
            generics[i] = type;
        }
        Type res = function.call(argTypes, generics);
        if (res == null) {
            error("Call", "Can't call function with given arguments", node.pos_start, node.pos_end);
        }
    }

    void compile(FuncDefNode node) {
        FuncType type = (FuncType) typeHandler.resolve(node);

        int global = -1;
        if (node.var_name_tok != null) {
            global = parseVariable(node.var_name_tok, type, node.pos_start, node.pos_end);
            markInitialized();
            if (scopeDepth == 0)
                globals.put(node.var_name_tok.value.toString(), type);
        }

        function(FunctionType.Function, type, node);

        if (node.var_name_tok != null) {
            defineVariable(global, type, false, node.pos_start, node.pos_end);
        }
    }

    void compile(SwitchNode node) {
        wrapScope(compiler -> {
            if (node.match)
                compiler.compileMatch(node);
            else
                compiler.compileSwitch(node);
        },
        null, node.pos_start, node.pos_end);
    }

    void compileSwitch(SwitchNode node) {
        breaks.add(new ArrayList<>());
        int[] jumps = new int[node.cases.size()];
        for (int i = 0; i < jumps.length; i++) {
            Case caze = node.cases.get(i);

            compile(node.reference);
            compile(caze.condition);
            emit(OpCode.Equal, node.pos_start, node.pos_end);

            jumps[i] = emitJump(OpCode.JumpIfTrue, node.pos_start, node.pos_end);
            emit(OpCode.Pop, node.pos_start, node.pos_end);
        }
        int defaultJump = emitJump(OpCode.Jump, node.pos_start, node.pos_end);

        for (int i = 0; i < jumps.length; i++) {
            Case caze = node.cases.get(i);
            int jump = jumps[i];

            patchJump(jump);
            emit(OpCode.Pop, node.pos_start, node.pos_end);
            compile(caze.statements);
        }

        patchJump(defaultJump);
        if (node.elseCase != null)
            compile(node.elseCase.statements);

        patchBreaks();

        compileNull(node.pos_start, node.pos_end);

    }

    void compileMatch(SwitchNode node) {
        breaks.add(new ArrayList<>());
        int[] jumps = new int[node.cases.size()];
        for (int i = 0; i < jumps.length; i++) {
            Case caze = node.cases.get(i);

            compile(node.reference);
            int height = localCount;
            compile(caze.condition);
            emit(OpCode.Equal, node.pos_start, node.pos_end);

            int jump = emitJump(OpCode.JumpIfFalse, node.pos_start, node.pos_end);
            emit(OpCode.Pop, node.pos_start, node.pos_end);

            compile(caze.statements);
            jumps[i] = emitJump(OpCode.Jump, node.pos_start, node.pos_end);
            patchJump(jump);

            emit(OpCode.Pop, node.pos_start, node.pos_end);

            localCount = height;
        }

        if (node.elseCase != null) {
            compile(node.elseCase.statements);
        }
        else {
            compileNull(node.pos_start, node.pos_end);
        }

        for (int jump : jumps)
            patchJump(jump);

        patchBreaks();
    }

    void markInitialized() {
        if (scopeDepth == 0) return;
        locals[localCount - 1].depth = scopeDepth;
    }

    void function(FunctionType type, FuncType funcType, FuncDefNode node) {
        function(type, funcType, node, c -> {}, c -> {});
    }

    void function(FunctionType type, FuncType funcType, FuncDefNode node,CompilerWrapped pre, CompilerWrapped post) {
        Compiler compiler = new Compiler(this, type, chunk().source, funcType).catchErrors(node.catcher);
        compiler.beginScope();

        for (GenericType generic : funcType.generics) {
            compiler.typeHandler.types.put(generic.name, generic);
        }

        for (int i = 0; i < node.arg_name_toks.size(); i++) {
            compiler.function.arity++;
            compiler.function.totarity++;

            Token param = node.arg_name_toks.get(i);
            Token paramType = node.arg_type_toks.get(i);

            compiler.parseVariable(param, compiler.typeLookup(paramType), param.pos_start, param.pos_end);
            compiler.makeVar(compiler.localCount - 1, false, param.pos_start, param.pos_end);
        }

        if (node.argname != null) {
            Token argNameToken = new Token(TokenType.Identifier, node.argname, node.pos_start, node.pos_end);
            compiler.function.totarity++;
            compiler.parseVariable(argNameToken, Types.LIST, argNameToken.pos_start, argNameToken.pos_end);
            compiler.makeVar(compiler.localCount - 1, false, argNameToken.pos_start, argNameToken.pos_end);
        }

        if (node.kwargname != null) {
            Token kwargNameToken = new Token(TokenType.Identifier, node.kwargname, node.pos_start, node.pos_end);
            compiler.function.totarity++;
            compiler.parseVariable(kwargNameToken, Types.DICT, kwargNameToken.pos_start, kwargNameToken.pos_end);
            compiler.makeVar(compiler.localCount - 1, false, kwargNameToken.pos_start, kwargNameToken.pos_end);
        }

        pre.compile(compiler);
        compiler.compile(node.body_node);
        post.compile(compiler);

        compiler.emit(OpCode.Return, node.body_node.pos_start, node.body_node.pos_end);

        JFunc function = compiler.endCompiler();

        function.name = node.var_name_tok != null ? node.var_name_tok.value.toString() : "<anonymous>";
        function.async = node.async;

        function.catcher = node.catcher;

        function.varargs = node.argname != null;
        function.kwargs = node.kwargname != null;

        for (Node defaultValue : node.defaults) {
            if (defaultValue != null)
                compile(defaultValue);
        }

        emit(new int[]{ OpCode.Closure, chunk().addConstant(new Value(function)), node.defaultCount }, node.pos_start, node.pos_end);

        for (int i = 0; i < function.upvalueCount; i++) {
            Upvalue upvalue = compiler.upvalues[i];
            emit(upvalue.isLocal ? 1 : upvalue.isGlobal ? 2 : 0, node.pos_start, node.pos_end);
            if (!upvalue.isGlobal) {
                emit(upvalue.index, node.pos_start, node.pos_end);
            }
            else {
                emit(chunk().addConstant(new Value(upvalue.globalName)), node.pos_start, node.pos_end);
            }
        }
    }

    private Compiler catchErrors(boolean catcher) {
        catchErrors = catcher;
        return this;    }

    void compileNull(@NotNull Position start, @NotNull Position end) {
        emit(OpCode.Null, start, end);
    }

    void compileBoolean(boolean val, @NotNull Position start, @NotNull Position end) {
        int constant = chunk().addConstant(new Value(val));
        emit(OpCode.Constant, constant, start, end);
    }

    void compileNumber(double val, @NotNull Position start, @NotNull Position end) {
        int constant = chunk().addConstant(new Value(val));
        emit(OpCode.Constant, constant, start, end);
    }

    void compileString(String val, @NotNull Position start, @NotNull Position end) {
        int constant = chunk().addConstant(new Value(val));
        emit(OpCode.Constant, constant, start, end);
    }

    void compile(BinOpNode node) {
        if (node.op_tok == TokenType.Ampersand) {
            compile(node.left_node);
            int jump = emitJump(OpCode.JumpIfFalse, node.left_node.pos_start, node.left_node.pos_end);
            //noinspection DuplicatedCode
            emit(OpCode.Pop, node.left_node.pos_start, node.left_node.pos_end);
            compile(node.right_node);
            patchJump(jump);
            return;
        }
        else if (node.op_tok == TokenType.Pipe) {
            compile(node.left_node);
            int jump = emitJump(OpCode.JumpIfTrue, node.left_node.pos_start, node.left_node.pos_end);
            //noinspection DuplicatedCode
            emit(OpCode.Pop, node.left_node.pos_start, node.left_node.pos_end);
            compile(node.right_node);
            patchJump(jump);
            return;
        }
        else if (node.op_tok == TokenType.FatArrow) {
            compile(node.right_node);
            compile(node.left_node);
            emit(OpCode.SetRef, node.pos_start, node.pos_end);
            return;
        }
        else if (node.op_tok == TokenType.Colon) {
            compile(node.left_node);
            compile(node.right_node);
            emit(OpCode.Chain, node.pos_start, node.pos_end);
            return;
        }

        compile(node.left_node);
        compile(node.right_node);
        switch (node.op_tok) {
            case Plus:
                emit(OpCode.Add, node.pos_start, node.pos_end);
                break;
            case Minus:
                emit(OpCode.Subtract, node.pos_start, node.pos_end);
                break;
            case Star:
                emit(OpCode.Multiply, node.pos_start, node.pos_end);
                break;
            case Slash:
                emit(OpCode.Divide, node.pos_start, node.pos_end);
                break;
            case Percent:
                emit(OpCode.Modulo, node.pos_start, node.pos_end);
                break;
            case Caret:
                emit(OpCode.Power, node.pos_start, node.pos_end);
                break;

            case EqualEqual:
                emit(OpCode.Equal, node.pos_start, node.pos_end);
                break;
            case BangEqual:
                emit(new int[]{ OpCode.Equal, OpCode.Not }, node.pos_start, node.pos_end);
                break;
            case RightAngle:
                emit(OpCode.GreaterThan, node.pos_start, node.pos_end);
                break;
            case LeftAngle:
                emit(OpCode.LessThan, node.pos_start, node.pos_end);
                break;
            case GreaterEquals:
                emit(new int[]{ OpCode.LessThan, OpCode.Not }, node.pos_start, node.pos_end);
                break;
            case LessEquals:
                emit(new int[]{ OpCode.GreaterThan, OpCode.Not }, node.pos_start, node.pos_end);
                break;

            case LeftBracket:
                emit(OpCode.Index, node.pos_start, node.pos_end);
                break;
            case Dot:
                emit(OpCode.Get, node.pos_start, node.pos_end);
                break;

            case TildeAmpersand:
                emit(OpCode.BitAnd, node.pos_start, node.pos_end);
                break;
            case TildePipe:
                emit(OpCode.BitOr, node.pos_start, node.pos_end);
                break;
            case TildeCaret:
                emit(OpCode.BitXor, node.pos_start, node.pos_end);
                break;
            case LeftTildeArrow:
                emit(OpCode.LeftShift, node.pos_start, node.pos_end);
                break;
            case TildeTilde:
                emit(OpCode.RightShift, node.pos_start, node.pos_end);
                break;
            case RightTildeArrow:
                emit(OpCode.SignRightShift, node.pos_start, node.pos_end);
                break;

            default:
                throw new RuntimeException("Unknown operator: " + node.op_tok);
        }
    }

    void compile(UnaryOpNode node) {
        compile(node.node);
        switch (node.op_tok) {
            case Plus:
                break;
            case Minus:
                emit(OpCode.Negate, node.pos_start, node.pos_end);
                break;
            case Bang:
                emit(OpCode.Not, node.pos_start, node.pos_end);
                break;
            case PlusPlus:
                emit(OpCode.Increment, node.pos_start, node.pos_end);
                break;
            case MinusMinus:
                emit(OpCode.Decrement, node.pos_start, node.pos_end);
                break;
            case Tilde:
                emit(OpCode.BitCompl, node.pos_start, node.pos_end);
                break;
            case DollarSign:
                emit(OpCode.FromBytes, node.pos_start, node.pos_end);
                break;
            default: throw new RuntimeException("Unknown operator: " + node.op_tok);
        }
    }

    void compile(VarAccessNode node) {
        String name = node.var_name_tok.value.toString();
        accessVariable(name, node.pos_start, node.pos_end);
    }

    void accessVariable(String name, @NotNull Position start, @NotNull Position end) {
        if (macros.containsKey(name)) {
            compile(macros.get(name));
            return;
        }

        int arg = resolveLocal(name);

        if (arg != -1) {
            emit(OpCode.GetLocal, arg, start, end);
        }
        else if ((arg = resolveUpvalue(name)) != -1) {
            emit(OpCode.GetUpvalue, arg, start, end);
        }
        else if (hasGlobal(name)) {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.GetGlobal, arg, start, end);
        }
        else if (accessEnclosed(name) != null) {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.GetAttr, arg, start, end);
        }
        else if (inPattern) {
            arg = chunk().addConstant(new Value(name));
            addLocal(name, patternType, start, end);
            emit(OpCode.PatternVars, arg, start, end);
        }
        else {
            error("Scope", "Undefined variable '" + name + "'", start, end);
        }
    }

    void compile(DropNode node) {
        String name = node.varTok.value.toString();
        if (macros.containsKey(name)) {
            macros.remove(name);
            macroTypes.remove(name);
            return;
        }

        int arg = resolveLocal(name);

        if (arg != -1) {
            locals[arg] = null;
            emit(OpCode.DropLocal, arg, node.pos_start, node.pos_end);
        }
        else if ((arg = resolveUpvalue(name)) != -1) {
            upvalues[arg] = null;
            emit(OpCode.DropUpvalue, arg, node.pos_start, node.pos_end);
        }
        else {
            arg = chunk().addConstant(new Value(name));
            globals.remove(name);
            emit(OpCode.DropGlobal, arg, node.pos_start, node.pos_end);
        }

        compileNull(node.pos_start, node.pos_end);
    }

    void compile(VarAssignNode node) {
        if (node.defining)
            compileDecl(node.var_name_tok,
                    typeLookup(node.type, node.pos_start, node.pos_end), node.locked, node.value_node,
                    node.min != null ? node.min : Integer.MIN_VALUE,
                    node.max != null ? node.max : Integer.MAX_VALUE,
                    node.pos_start, node.pos_end);
        else
            compileAssign(node.var_name_tok, node.value_node, node.pos_start, node.pos_end);
    }

    void compile(LetNode node) {
        compileDecl(node.var_name_tok, null, false, node.value_node, Integer.MIN_VALUE, Integer.MAX_VALUE, node.pos_start, node.pos_end);
    }

    void defineVariable(int global, Type type, boolean constant, @NotNull Position start, @NotNull Position end) {
        defineVariable(global, type, constant, Integer.MIN_VALUE, Integer.MAX_VALUE, start, end);
    }

    void defineVariable(int global, Type type, boolean constant, int min, int max, @NotNull Position start, @NotNull Position end) {
        boolean usesRange = min != Integer.MIN_VALUE || max != Integer.MAX_VALUE;
        if (scopeDepth > 0) {
            markInitialized();
            emit(OpCode.DefineLocal, start, end);
            emit(constant ? 1 : 0, start, end);
            emit(usesRange ? 1 : 0, start, end);
            if (usesRange) {
                emit(min, max, start, end);
            }
            return;
        }

        globals.put(chunk().constants.values.get(global).asString(), type);
        emit(OpCode.DefineGlobal, global, start, end);
        emit(constant ? 1 : 0, start, end);
        emit(usesRange ? 1 : 0, start, end);
        if (usesRange) {
            emit(min, max, start, end);
        }
    }

    void makeVar(int slot, boolean constant, @NotNull Position start, @NotNull Position end) {
        emit(OpCode.MakeVar, slot, start, end);
        emit(constant ? 1 : 0, start, end);
    }

    void addLocal(String name, Type type, @NotNull Position start, @NotNull Position end) {
        Local local = new Local(new LocalToken(name, start.idx, end.idx - start.idx), type, scopeDepth);

        locals[localCount++] = local;
    }

    void addGeneric(String name, @NotNull Position start, @NotNull Position end) {
        Local local = new Local(new LocalToken(name, start.idx, end.idx - start.idx), new GenericType(name), scopeDepth);

        generics[localCount++] = local;
        locals[localCount - 1] = local;
    }

    void declareVariable(Token varNameTok, Type type, @NotNull Position start, @NotNull Position end) {
        if (scopeDepth == 0)
            return;

        String name = varNameTok.value.toString();
        addLocal(name, type, start, end);
    }

    int parseVariable(Token varNameTok, Type type, @NotNull Position start, @NotNull Position end) {
        declareVariable(varNameTok, type, start, end);
        if (scopeDepth > 0)
            return 0;

        return chunk().addConstant(new Value(varNameTok.value.toString()));
    }

    void compileDecl(Token varNameTok, Type type, boolean locked, Node value,
                     int min, int max, @NotNull Position start, @NotNull Position end) {
        Type t = compile(value);
        if (type == null) type = t;
        if (!t.equals(type)) {
            error("Type", "Type mismatch in declaration", start, end);
        }
        int global = parseVariable(varNameTok, type, start, end);
        defineVariable(global, type, locked, min, max, start, end);
    }

    void compileAssign(Token varNameTok, Node value, @NotNull Position start, @NotNull Position end) {
        String name = varNameTok.value.toString();
        int arg = resolveLocal(name);

        Type expected = variableType(name, start, end);
        Type actual = compile(value);

        if (!expected.equals(actual)) {
            error("Type", "Expected " + expected + " but got " + actual, start, end);
        }

        if (arg != -1) {
            emit(OpCode.SetLocal, arg, start, end);
        }
        else if ((arg = resolveUpvalue(name)) != -1) {
            emit(OpCode.SetUpvalue, arg, start, end);
        }
        else {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.SetGlobal, arg, start, end);
        }
    }

    interface CompilerWrapped {
        void compile(Compiler compiler);
    }

    void wrapScope(CompilerWrapped method, String scopeName, Position start, Position end) {
        // ()<> -> Any
        Compiler scope = new Compiler(this, FunctionType.Scope, chunk().source, new FuncType(Types.ANY, new Type[0], new GenericType[0], false));

        scope.beginScope();
        method.compile(scope);
        scope.emit(OpCode.Return, start, end);
        scope.endScope(start, end);

        JFunc func = scope.endCompiler();
        func.name = scopeName;

        emit(new int[]{ OpCode.Closure, chunk().addConstant(new Value(func)), 0 }, start, end);
        for (int i = 0; i < func.upvalueCount; i++) {
            Upvalue upvalue = scope.upvalues[i];
            emit(upvalue.isLocal ? 1 : upvalue.isGlobal ? 2 : 0, start, end);
            if (!upvalue.isGlobal) {
                emit(upvalue.index, start, end);
            }
            else {
                emit(chunk().addConstant(new Value(upvalue.globalName)), start, end);
            }
        }
        emit(new int[]{ OpCode.Call, 0, 0 }, start, end);
    }

    void compile(ScopeNode node) {
        wrapScope(compiler -> compiler.compile(node.statements), node.scopeName, node.pos_start, node.pos_end);
    }

    void compile(QueryNode node) {
        List<Integer> jumps = new ArrayList<>();
        int lastJump = 0;

        for (Case nodeCase : node.cases) {
            if (lastJump != 0) {
                patchJump(lastJump);
                emit(OpCode.Pop, nodeCase.condition.pos_start, nodeCase.condition.pos_end);
            }

            compile(nodeCase.condition);
            lastJump = emitJump(OpCode.JumpIfFalse, nodeCase.condition.pos_start, nodeCase.condition.pos_end);
            emit(OpCode.Pop, nodeCase.condition.pos_start, nodeCase.condition.pos_end);
            beginScope();
            compile(nodeCase.statements);
            endScope(nodeCase.statements.pos_start, nodeCase.statements.pos_end);

            Position start = nodeCase.statements.pos_start;
            Position end = nodeCase.statements.pos_end;
            if (!nodeCase.returnValue) {
                emit(OpCode.Pop, start, end);
                compileNull(start, end);
            }

            jumps.add(emitJump(OpCode.Jump, start, end));
        }

        if (node.else_case != null) {
            if (lastJump != 0) {
                patchJump(lastJump);
                emit(OpCode.Pop, node.else_case.statements.pos_start, node.else_case.statements.pos_end);
                lastJump = 0;
            }

            beginScope();
            compile(node.else_case.statements);
            endScope(node.else_case.statements.pos_start, node.else_case.statements.pos_end);

            if (!node.else_case.returnValue) {
                Position start = node.else_case.statements.pos_start;
                Position end = node.else_case.statements.pos_end;

                emit(OpCode.Pop, start, end);
                compileNull(start, end);
            }
        }

        if (lastJump != 0) {
            emit(OpCode.Pop, node.pos_start, node.pos_end);
            patchJump(lastJump);
        }

        for (int jump : jumps)
            patchJump(jump);

    }

    void loopBody(Node body, boolean returnsNull, int loopStart) {
        breaks.add(new ArrayList<>());
        continueTo.push(loopStart);

        beginScope();
        compile(body);

        if (returnsNull) {
            emit(OpCode.Pop, body.pos_start, body.pos_end);
        }
        else {
            emit(OpCode.CollectLoop, body.pos_start, body.pos_end);
        }

        int popCount = destack(locals) + destack(generics);

        endScope(body.pos_start, body.pos_end);
        emitLoop(loopStart, body.pos_start, body.pos_end);
        int pastJump = emitJump(OpCode.Jump, body.pos_start, body.pos_end);

        continueTo.pop();
        patchBreaks();
        for (int i = 0; i < popCount; i++)
            emit(OpCode.Pop, body.pos_start, body.pos_end);
        patchJump(pastJump);
    }

    Type compile(ClassDefNode node) {
        String name = node.class_name_tok.value.toString();

        Position constructorStart = node.make_node.pos_start;
        Position constructorEnd = node.make_node.pos_end;
        MethDefNode constructor = new MethDefNode(
                new Token(TokenType.Identifier, "<make>", constructorStart, constructorEnd),
                node.arg_name_toks,
                node.arg_type_toks,
                node.make_node,
                false,
                false,
                false,
                Collections.singletonList("void"),
                node.defaults,
                node.defaultCount,
                node.generic_toks,
                false,
                false,
                node.argname,
                node.kwargname
        );

        int nameConstant = chunk().addConstant(new Value(name));
        Type type = typeHandler.resolve(node);
        declareVariable(node.class_name_tok, type, node.class_name_tok.pos_start, node.class_name_tok.pos_end);

        for (int i = node.attributes.size() - 1; i >= 0; i--) {
            AttrDeclareNode attr = node.attributes.get(i);
            Node def = attr.nValue;
            if (def != null)
                compile(def);
            else
                compileNull(attr.pos_start, attr.pos_end);
        }

        enclosingType = type;

        for (int i = 0; i < node.generic_toks.size(); i++)
            compileNull(node.pos_start, node.pos_end);

        if (node.parentToken != null) accessVariable(node.parentToken.value.toString(), node.parentToken.pos_start, node.parentToken.pos_end);

        emit(OpCode.Class, nameConstant, node.pos_start, node.pos_end);
        emit(node.parentToken != null ? 1 : 0, node.pos_start, node.pos_end);

        emit(node.attributes.size() + node.generic_toks.size(), node.pos_start, node.pos_end);

        for (Token tok : node.generic_toks)
            compile(new AttrDeclareNode(
                tok,
                    Collections.singletonList("String"),
                false,
                true,
                null
            ));

        for (AttrDeclareNode attr : node.attributes)
            compile(attr);

        emit(node.generic_toks.size(), node.pos_start, node.pos_end);
        for (Token tok : node.generic_toks)
            emit(chunk().addConstant(new Value(tok.value.toString())), node.pos_start, node.pos_end);

        defineVariable(nameConstant, type, true, node.pos_start, node.pos_end);

        for (MethDefNode method : node.methods) {
            staticContext = method.stat;
            compile(method);
        }

        compile(constructor, true);

        enclosingType = Types.VOID;

        emit(OpCode.Pop, node.pos_start, node.pos_end);
        compileNull(node.pos_start, node.pos_end);

        return type;
    }

    void compile(MethDefNode node) {
        compile(node, false);
    }

    void compile(MethDefNode node, boolean isConstructor) {
        String name = node.var_name_tok.value.toString();
        int nameConstant = chunk().addConstant(new Value(name));

        FunctionType type = isConstructor ? FunctionType.Constructor : FunctionType.Method;

        FuncDefNode func = node.asFuncDef();
        FuncType funcType = (FuncType) typeHandler.resolve(func);

        function(type, funcType, func);

        emit(new int[]{
                OpCode.Method,
                nameConstant,
                node.stat ? 1 : 0,
                node.priv ? 1 : 0,
                node.bin ? 1 : 0,
        }, node.pos_start, node.pos_end);
    }

    void compile(AttrDeclareNode node) {
        int global = chunk().addConstant(new Value(node.name));
        emit(new int[]{
                global,
                node.isprivate ? 1 : 0,
                node.isstatic ? 1 : 0,
        }, node.pos_start, node.pos_end);
    }

    // CollectLoop adds the previous value to the loop stack in the VM
    // FlushLoop turns the values in the loop stack into an array, and
    // pushes it onto the stack, clearing the loop stack
    void compile(WhileNode node) {
        boolean isDoWhile = node.conLast;

        if (!node.retnull) emit(OpCode.StartCache, node.pos_start, node.pos_end);

        int skipFirst = isDoWhile ? emitJump(OpCode.Jump, node.pos_start, node.pos_end) : -1;
        int loopStart = chunk().code.size();

        compile(node.condition_node);
        int jump = emitJump(OpCode.JumpIfFalse, node.condition_node.pos_start, node.condition_node.pos_end);
        emit(OpCode.Pop, node.body_node.pos_start, node.body_node.pos_end);

        if (isDoWhile)
            patchJump(skipFirst);

        loopBody(node.body_node, node.retnull, loopStart);
        patchJump(jump);
        emit(OpCode.Pop, node.body_node.pos_start, node.body_node.pos_end);

        if (node.retnull) {
            compileNull(node.pos_start, node.pos_end);
        }
        else {
            emit(OpCode.FlushLoop, node.pos_start, node.pos_end);
        }
    }
    
    void compile(ForNode node) {
        if (!node.retnull) emit(OpCode.StartCache, node.pos_start, node.pos_end);
        beginScope();

        Type startType = typeHandler.resolve(node.start_value_node);
        Type stepType;
        if (node.step_value_node != null) {
            stepType = typeHandler.resolve(node.step_value_node);
        }
        else {
            stepType = Types.INT;
        }
        
        String name = node.var_name_tok.value.toString();
        copyVar(node.var_name_tok, startType.isCompatible(TokenType.Plus, stepType), node.start_value_node);

        int firstSkip = emitJump(OpCode.Jump, node.start_value_node.pos_start, node.start_value_node.pos_end);

        int loopStart = chunk().code.size();

        compile(node.end_value_node);
        if (node.step_value_node != null) {
            compile(node.step_value_node);
        }
        else {
            compileNumber(1, node.pos_start, node.pos_end);
        }

        emit(OpCode.For, node.pos_start, node.pos_end);
        emit(resolveLocal(name), node.pos_start, node.pos_end);

        emit(0xff, node.pos_start, node.pos_end);
        int jump = chunk().code.size() - 1;

        patchJump(firstSkip);
        loopBody(node.body_node, node.retnull, loopStart);

        endIter(node.pos_start, node.pos_end, node.retnull, jump);
    }

    void compile(IterNode node) {
        if (!node.retnull) emit(OpCode.StartCache, node.pos_start, node.pos_end);
        beginScope();

        String name = node.var_name_tok.value.toString();
        copyVar(new Token(
                TokenType.Identifier,
                "@" + name,
                node.var_name_tok.pos_start,
                node.var_name_tok.pos_end
        ), Types.LIST, node.iterable_node);
        compileDecl(node.var_name_tok,
                Types.ANY,
                false,
                new NullNode(new Token(
                        TokenType.Identifier,
                        "null",
                        node.var_name_tok.pos_start,
                        node.var_name_tok.pos_end
                )),
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                node.var_name_tok.pos_start, node.var_name_tok.pos_end);
        emit(OpCode.Pop, node.pos_start, node.pos_end);

        int loopStart = chunk().code.size();

        emit(OpCode.Iter, node.pos_start, node.pos_end);
        emit(resolveLocal("@" + name), resolveLocal(name), node.pos_start, node.pos_end);

        emit(0xff, node.pos_start, node.pos_end);
        int jump = chunk().code.size() - 1;

        loopBody(node.body_node, node.retnull, loopStart);

        endIter(node.pos_start, node.pos_end, node.retnull, jump);
    }

    void endIter(Position start, Position end, boolean retnull, int jump) {
        int offset = chunk().code.size() - jump - 1;
        chunk().code.set(jump, offset);
        endScope(start, end);

        if (retnull) {
            compileNull(start, end);
        }
        else {
            emit(OpCode.FlushLoop, start, end);
        }
    }

    void copyVar(Token varNameTok, Type type, Node startValueNode) {
        int global = parseVariable(varNameTok, type, varNameTok.pos_start, varNameTok.pos_end);
        compile(startValueNode);
        emit(OpCode.Copy, startValueNode.pos_start, startValueNode.pos_end);
        defineVariable(global, type, false, varNameTok.pos_start, startValueNode.pos_end);
        emit(OpCode.Pop, startValueNode.pos_start, startValueNode.pos_end);
    }

    void compile(ListNode node) {
        int size = node.elements.size();
        for (int i = node.elements.size() - 1; i >= 0; i--)
            compile(node.elements.get(i));
        emit(OpCode.MakeArray, size, node.pos_start, node.pos_end);
    }

    void compile(DictNode node) {
        Set<Map.Entry<Node, Node>> entrys = node.dict.entrySet();
        int size = entrys.size();
        for (Map.Entry<Node, Node> entry : entrys) {
            compile(entry.getKey());
            compile(entry.getValue());
        }
        emit(OpCode.MakeMap, size, node.pos_start, node.pos_end);
    }

    public interface ExtendFailure {
        void failure(String reason, String message);
    }

    public static boolean bootExtend(String fn, ExtendFailure function, VM vm) {
        String file_name = System.getProperty("user.dir") + "/" + fn + ".jar";
        String modPath = Shell.root + "/extensions/" + fn;
        String modFilePath = modPath + "/" + fn + ".jar";

        //noinspection ResultOfMethodCallIgnored
        new File(Shell.root + "/extensions").mkdirs();

        try {
            URL[] urls;
            if (Files.exists(Paths.get(modFilePath))) {
                urls = new URL[]{new URL("file://" + modFilePath)};
            }
            else if (Files.exists(Paths.get(file_name))) {
                urls = new URL[]{new URL("file://" + file_name)};
            }
            else {
                function.failure("Imaginary File", "File '" + fn + "' not found");
                return false;
            }
            ClassLoader cl = new URLClassLoader(urls);
            Class<?> loadedClass = cl.loadClass("jpext." + fn);
            Constructor<?> constructor = loadedClass.getConstructor(VM.class);
            Object loadedObject = constructor.newInstance(vm);
            if (loadedObject instanceof JPExtension) {
                JPExtension extension = (JPExtension) loadedObject;
                extension.Start();
            }
            else {
                function.failure("Imaginary File", "File '" + fn + "' is not a valid extension");
                return false;
            }
        } catch (Exception e) {
            function.failure("Internal", "Failed to load extension (" + e.getMessage() + ")");
            return false;
        }
        return true;
    }

}
