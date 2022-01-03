package lemon.jpizza.compiler;

import lemon.jpizza.*;
import lemon.jpizza.cases.Case;
import lemon.jpizza.compiler.headers.HeadCode;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.enums.JEnum;
import lemon.jpizza.compiler.values.enums.JEnumChild;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.vm.VM;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Compiler {

    static record LocalToken(String name, int idx, int len) {}
    static class Local {
        final LocalToken name;
        int depth;

        Local(LocalToken name, int depth) {
            this.name = name;
            this.depth = depth;
        }

    }

    static class Upvalue {
        boolean isLocal;
        boolean isGlobal;

        String globalName;
        int index;

        public Upvalue(int index, boolean isLocal) {
            this.index = index;
            this.isLocal = isLocal;
            this.isGlobal = false;
        }

        public Upvalue(String globalName) {
            this.isGlobal = true;
            this.globalName = globalName;
        }
    }

    final Compiler enclosing;

    boolean inPattern = false;

    final Local[] locals;
    final Local[] generics;
    int localCount;
    int scopeDepth;

    final JFunc function;
    final FunctionType type;

    int continueTo;
    final List<Integer> breaks;

    final List<String> globals;

    String packageName;
    String target;

    final Upvalue[] upvalues;

    Map<String, Node> macros;

    public Chunk chunk() {
        return this.function.chunk;
    }
    
    public Compiler(FunctionType type, String source) {
        this(null, type, source);
    }

    public Compiler(Compiler enclosing, FunctionType type, String source) {
        this.function = new JFunc(source);
        this.type = type;

        this.locals = new Local[VM.MAX_STACK_SIZE];
        this.generics = new Local[VM.MAX_STACK_SIZE];
        this.globals = new ArrayList<>();

        this.upvalues = new Upvalue[256];

        this.localCount = 0;
        this.scopeDepth = 0;

        locals[localCount++] = new Local(new LocalToken(type == FunctionType.Method ? "this" : "", 0, 0), 0);

        this.enclosing = enclosing;

        this.continueTo = 0;
        this.breaks = new ArrayList<>();

        this.macros = new HashMap<>();
    }

    public void beginScope() {
        this.scopeDepth++;
    }

    public void endScope(@NotNull Position start, @NotNull Position end) {
        destack(locals, start, end);
        destack(generics, start, end);
        scopeDepth--;
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

    int resolveLocal(String name) {
        return resolve(name, locals);
    }

    int resolveGeneric(String name) {
        return resolve(name, generics);
    }

    int addUpvalue(int index, boolean isLocal) {
        int upvalueCount = function.upvalueCount;

        for (int i = 0; i < upvalueCount; i++) {
            Upvalue upvalue = upvalues[i];
            if (upvalue.index == index && upvalue.isLocal == isLocal) {
                return i;
            }
        }

        upvalues[upvalueCount] = new Upvalue(index, isLocal);
        return function.upvalueCount++;
    }

    int addUpvalue(String name) {
        int upvalueCount = function.upvalueCount;

        for (int i = 0; i < upvalueCount; i++) {
            Upvalue upvalue = upvalues[i];
            if (Objects.equals(upvalue.globalName, name) && upvalue.isGlobal) {
                return i;
            }
        }

        upvalues[upvalueCount] = new Upvalue(name);
        return function.upvalueCount++;
    }

    boolean hasGlobal(String name) {
        return globals.contains(name) || enclosing != null && enclosing.hasGlobal(name);
    }

    int resolveUpvalue(String name) {
        if (enclosing == null) return -1;

        int local = enclosing.resolveLocal(name);
        if (local != -1) {
            return addUpvalue(local, true);
        }

        int upvalue = enclosing.resolveUpvalue(name);
        if (upvalue != -1) {
            return addUpvalue(upvalue, false);
        }

        return hasGlobal(name) ? addUpvalue(name) : -1;
    }

    void patchBreaks() {
        for (int i : breaks) {
            patchJump(i);
        }
        breaks.clear();
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

    void compile(Node statement) {
        if (statement instanceof BinOpNode)
            compile((BinOpNode) statement);

        else if (statement instanceof ExtendNode)
            compile((ExtendNode) statement);

        else if (statement instanceof DecoratorNode)
            compile((DecoratorNode) statement);

        else if (statement instanceof UnaryOpNode)
            compile((UnaryOpNode) statement);

        else if (statement instanceof NumberNode) {
            NumberNode node = (NumberNode) statement;
            compileNumber(node.val, node.pos_start, node.pos_end);
        }

        else if (statement instanceof StringNode) {
            StringNode node = (StringNode) statement;
            compileString(node.val, node.pos_start, node.pos_end);
        }

        else if (statement instanceof BooleanNode) {
            BooleanNode node = (BooleanNode) statement;
            compileBoolean(node.val, node.pos_start, node.pos_end);
        }

        else if (statement instanceof NullNode)
            compileNull(statement.pos_start, statement.pos_end);

        else if (statement instanceof BodyNode) {
            BodyNode node = (BodyNode) statement;
            for (Node stmt : node.statements) {
                compile(stmt);
                emit(OpCode.Pop, stmt.pos_start, stmt.pos_end);
            }
            compileNull(node.pos_start, node.pos_end);
        }

        else if (statement instanceof VarAssignNode)
            compile((VarAssignNode) statement);

        else if (statement instanceof VarAccessNode)
            compile((VarAccessNode) statement);

        else if (statement instanceof AssertNode)
            compile((AssertNode) statement);

        else if (statement instanceof ScopeNode)
            compile((ScopeNode) statement);

        else if (statement instanceof QueryNode)
            compile((QueryNode) statement);

        else if (statement instanceof WhileNode)
            compile((WhileNode) statement);

        else if (statement instanceof ForNode)
            compile((ForNode) statement);

        else if (statement instanceof PassNode)
            compileNull(statement.pos_start, statement.pos_end);

        else if (statement instanceof LetNode)
            compile((LetNode) statement);

        else if (statement instanceof RefNode)
            compile((RefNode) statement);

        else if (statement instanceof DerefNode)
            compile((DerefNode) statement);

        else if (statement instanceof FuncDefNode)
            compile((FuncDefNode) statement);

        else if (statement instanceof CallNode)
            compile((CallNode) statement);

        else if (statement instanceof ReturnNode)
            compile((ReturnNode) statement);

        else if (statement instanceof ListNode)
            compile((ListNode) statement);

        else if (statement instanceof DictNode)
            compile((DictNode) statement);

        else if (statement instanceof ClassDefNode)
            compile((ClassDefNode) statement);

        else if (statement instanceof UseNode)
            compile((UseNode) statement);

        else if (statement instanceof ContinueNode) {
            compileNull(statement.pos_start, statement.pos_end);
            emitLoop(continueTo, statement.pos_start, statement.pos_end);
        }

        else if (statement instanceof BreakNode) {
            compileNull(statement.pos_start, statement.pos_end);
            breaks.add(emitJump(OpCode.Jump, statement.pos_start, statement.pos_end));
        }

        else if (statement instanceof BytesNode)
            compile((BytesNode) statement);

        else if (statement instanceof ClaccessNode) {
            ClaccessNode node = (ClaccessNode) statement;
            compile(node.class_tok);
            int constant = chunk().addConstant(new Value(node.attr_name_tok.value.toString()));
            emit(OpCode.Access, constant, node.pos_start, node.pos_end);
        }

        else if (statement instanceof AttrAssignNode) {
            AttrAssignNode node = (AttrAssignNode) statement;
            compile(node.value_node);
            int constant = chunk().addConstant(new Value(node.var_name_tok.value.toString()));
            emit(OpCode.SetAttr, constant, node.pos_start, node.pos_end);
        }

        else if (statement instanceof AttrAccessNode) {
            AttrAccessNode node = (AttrAccessNode) statement;
            int constant = chunk().addConstant(new Value(node.var_name_tok.value.toString()));
            emit(OpCode.GetAttr, constant, node.pos_start, node.pos_end);
        }

        else if (statement instanceof SwitchNode)
            compile((SwitchNode) statement);

        else if (statement instanceof ThrowNode)
            compile((ThrowNode) statement);

        else if (statement instanceof ImportNode)
            compile((ImportNode) statement);

        else if (statement instanceof EnumNode)
            compile((EnumNode) statement);

        else if (statement instanceof IterNode)
            compile((IterNode) statement);

        else if (statement instanceof SpreadNode)
            compile((SpreadNode) statement);

        else if (statement instanceof DynAssignNode)
            compile((DynAssignNode) statement);

        else if (statement instanceof DropNode)
            compile((DropNode) statement);

        else if (statement instanceof DestructNode)
            compile((DestructNode) statement);

        else if (statement instanceof PatternNode)
            compile((PatternNode) statement);

        else
            throw new RuntimeException("Unknown statement type: " + statement.getClass().getName());
    }

    void compile(ExtendNode node) {
        emit(OpCode.Extend, chunk().addConstant(new Value(node.file_name_tok.value.toString())), node.pos_start, node.pos_end);
    }

    void compile(DecoratorNode node) {
        compile(node.decorated);
        compile(node.decorator);
        emit(new int[]{
                OpCode.Call,
                1, 0,
                0
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
        compile(node.accessNode);
        inPattern = true;
        Token[] keySet = node.patterns.keySet().toArray(new Token[0]);
        for (Token token : keySet) {
            compile(node.patterns.get(token));
        }
        inPattern = false;
        emit(OpCode.Pattern, keySet.length, node.pos_start, node.pos_end);
        for (int i = keySet.length - 1; i >= 0; i--) {
            Token token = keySet[i];
            int constant = chunk().addConstant(new Value(token.value.toString()));
            emit(constant, token.pos_start, token.pos_end);
        }
    }

    void compile(DestructNode node) {
        compile(node.target);
        emit(OpCode.Destruct, node.glob ? -1 : node.subs.size(), node.pos_start, node.pos_end);
        if (!node.glob) for (Token sub : node.subs) {
            String name = sub.value.toString();
            globals.add(name);
            emit(chunk().addConstant(new Value(name)), sub.pos_start, sub.pos_end);
        }
    }

    void compile(UseNode node) {
        int code = switch (node.useToken.value.toString()) {
            case "memoize"  -> HeadCode.Memoize;
            case "func"     -> HeadCode.SetMainFunction;
            case "object"   -> HeadCode.SetMainClass;
            case "export"   -> HeadCode.Export;
            case "package" -> {
                StringBuilder sb = new StringBuilder();
                for (Token token : node.args) {
                    sb.append(token.asString());
                }
                packageName = sb.toString();
                chunk().packageName = packageName;
                yield HeadCode.Package;
            }
            case "export_to" -> {
                if (node.args.size() != 1) {
                    Shell.logger.fail(new Error(node.pos_start, node.pos_end, "Argument Count", "export_to() takes exactly one argument").asString());
                }
                else {
                    target = node.args.get(0).asString();
                    chunk().target = target;
                }
                yield HeadCode.ExportTo;
            }
            default         -> -1;
        };

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
        compileNull(node.pos_start, node.pos_end);
    }

    void compile(BytesNode node) {
        compile(node.toBytes);
        emit(OpCode.ToBytes, node.pos_start, node.pos_end);
    }

    void compile(DerefNode node) {
        compile(node.ref);
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

    void compile(EnumNode node) {
        Map<String, JEnumChild> children = new HashMap<>();

        int argc = node.children.size();
        for (int i = 0; i < argc; i++) {
            Parser.EnumChild child = node.children.get(i);

            List<Integer> genericSlots = new ArrayList<>();
            for (List<String> rawType : child.types())
                if (rawType.size() == 1 && child.generics().contains(rawType.get(0))) {
                    genericSlots.add(child.generics().indexOf(rawType.get(0)));
                }
                else {
                    genericSlots.add(-1);
                }

            String name = child.token().value.toString();
            children.put(name, new JEnumChild(
                    i,
                    child.params(),
                    child.types(),
                    child.generics(),
                    genericSlots
            ));

            if (node.pub)
                globals.add(name);
        }

        String name = node.tok.value.toString();
        globals.add(name);
        int constant = chunk().addConstant(new Value(new JEnum(
                name,
                children
        )));
        emit(new int[]{ OpCode.Enum, constant, node.pub ? 1 : 0 }, node.pos_start, node.pos_end);
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
            if (Objects.equals(target, "package") && !equalPackages(packageName, chunk.packageName)) {
                throw new IOException("Cannot import file outside of package");
            }
            else if (!target.equals("all")) {
                throw new IOException("File is not a module");
            }
        }
        return func;
    }

    void compile(ImportNode node) {
        String fn = node.file_name_tok.asString();
        String chrDir = System.getProperty("user.dir");

        String fileName = chrDir + "/" + fn;

        String modPath = Shell.root + "/modules/" + fn;
        String modFilePath = modPath + "/" + fn;

        //noinspection ResultOfMethodCallIgnored
        new File(Shell.root + "/modules").mkdirs();

        JFunc imp = null;
        try {
            if (Constants.STANDLIBS.containsKey(fn)) {
                Pair<JFunc, Error> res = Shell.compile(fn, Constants.STANDLIBS.get(fn));
                if (res.b != null)
                    Shell.logger.warn(res.b.asString());
                imp = res.a;
            }
            else if (Files.exists(Paths.get(modFilePath + ".jbox"))) {
                imp = canImport(Shell.load(Files.readString(Paths.get(modFilePath + ".jbox"))));
            }
            else if (Files.exists(Paths.get(fileName + ".jbox"))) {
                imp = canImport(Shell.load(Files.readString(Paths.get(fileName + ".jbox"))));
            }
            else if (Files.exists(Paths.get(fileName + ".devp"))) {
                //noinspection DuplicatedCode
                System.setProperty("user.dir", fileName + ".devp");
                Pair<JFunc, Error> res = Shell.compile(fn, Files.readString(Paths.get(fileName + ".devp")));
                if (res.b != null)
                    Shell.logger.warn(res.b.asString());
                imp = canImport(res.a);
                System.setProperty("user.dir", chrDir);
            }
            else if (Files.exists(Paths.get(modFilePath + ".devp"))) {
                //noinspection DuplicatedCode
                System.setProperty("user.dir", modPath + ".devp");
                Pair<JFunc, Error> res = Shell.compile(fn, Files.readString(Paths.get(modFilePath + ".devp")));
                if (res.b != null)
                    Shell.logger.warn(res.b.asString());
                imp = canImport(res.a);
                System.setProperty("user.dir", chrDir);
            }
        } catch (IOException e) {
            imp = null;
            Shell.logger.fail(new Error(node.pos_start, node.pos_end, "Import", "Couldn't import file (" + e.getMessage() + ")").asString());
        }

        if (imp != null) {
            int addr = chunk().addConstant(new Value(imp));
            emit(OpCode.Constant, addr, node.pos_start, node.pos_end);
        }
        else {
            compileNull(node.pos_start, node.pos_end);
        }

        int constant = chunk().addConstant(new Value(fn));
        emit(OpCode.Import, constant, node.pos_start, node.pos_end);
        globals.add(fn);

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
        for (Node arg : node.argNodes) {
            compile(arg);
        }
        List<String> kwargNames = node.kwargs.keySet().stream().toList();
        for (int i = kwargc - 1; i >= 0; i--) {
            compile(node.kwargs.get(kwargNames.get(i)));
        }
        compile(node.nodeToCall);
        emit(new int[]{
                OpCode.Call,
                argc, kwargc,
                node.generics.size()
        }, node.pos_start, node.pos_end);
        for (int i = 0; i < kwargc; i++) {
            emit(chunk().addConstant(new Value(kwargNames.get(i))), node.pos_start, node.pos_end);
        }
        for (Token generic : node.generics) {
            compileType((List<String>) generic.value, generic.pos_start, generic.pos_end);
        }
    }

    void compile(FuncDefNode node) {
        int global = -1;
        if (node.var_name_tok != null) {
            global = parseVariable(node.var_name_tok, node.pos_start, node.pos_end);
            markInitialized();
        }
        function(FunctionType.Function, node);
        if (node.var_name_tok != null) {
            defineVariable(global, List.of("function"), false, node.pos_start, node.pos_end);
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
        int[] jumps = new int[node.cases.size()];
        for (int i = 0; i < jumps.length; i++) {
            Case caze = node.cases.get(i);
            compile(node.reference);
            compile(caze.condition);
            emit(OpCode.Equal, node.pos_start, node.pos_end);
            int jump = emitJump(OpCode.JumpIfFalse, node.pos_start, node.pos_end);
            emit(OpCode.Pop, node.pos_start, node.pos_end);
            compile(caze.statements);
            jumps[i] = emitJump(OpCode.Jump, node.pos_start, node.pos_end);
            patchJump(jump);
            emit(OpCode.Pop, node.pos_start, node.pos_end);
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

    void function(FunctionType type, FuncDefNode node) {
        function(type, node, c -> {}, c -> {});
    }

    void function(FunctionType type, FuncDefNode node, CompilerWrapped pre, CompilerWrapped post) {
        Compiler compiler = new Compiler(this, type, chunk().source);

        compiler.beginScope();

        List<String> genericNames = new ArrayList<>();
        for (int i = 0; i < node.generic_toks.size(); i++) {
            compiler.function.totarity++;
            compiler.function.genericArity++;

            Token generic = node.generic_toks.get(i);
            compiler.addGeneric(generic.value.toString(), generic.pos_start, generic.pos_end);

            genericNames.add(generic.value.toString());
        }
        List<String> retype = compiler.compileType(node.returnType, node.pos_start, node.pos_end, false);

        for (int i = 0; i < node.arg_name_toks.size(); i++) {
            compiler.function.arity++;
            compiler.function.totarity++;

            Token param = node.arg_name_toks.get(i);
            Token paramType = node.arg_type_toks.get(i);
            List<String> rawType = (List<String>) paramType.value;

            compiler.parseVariable(param, param.pos_start, param.pos_end);
            compiler.makeVar(compiler.localCount - 1, rawType, false, param.pos_start, param.pos_end);

            if (rawType.size() == 1 && genericNames.contains(rawType.get(0))) {
                compiler.function.genericSlots.add(genericNames.indexOf(rawType.get(0)));
            }
            else {
                compiler.function.genericSlots.add(-1);
            }
        }

        if (node.argname != null) {
            Token argNameToken = new Token(TokenType.Identifier, node.argname, node.pos_start, node.pos_end);
            compiler.function.totarity++;
            compiler.parseVariable(argNameToken, argNameToken.pos_start, argNameToken.pos_end);
            compiler.makeVar(compiler.localCount - 1, List.of("list"), false, argNameToken.pos_start, argNameToken.pos_end);
        }

        if (node.kwargname != null) {
            Token kwargNameToken = new Token(TokenType.Identifier, node.kwargname, node.pos_start, node.pos_end);
            compiler.function.totarity++;
            compiler.parseVariable(kwargNameToken, kwargNameToken.pos_start, kwargNameToken.pos_end);
            compiler.makeVar(compiler.localCount - 1, List.of("dict"), false, kwargNameToken.pos_start, kwargNameToken.pos_end);
        }

        pre.compile(compiler);
        compiler.compile(node.body_node);
        post.compile(compiler);

        compiler.emit(OpCode.Return, node.body_node.pos_start, node.body_node.pos_end);

        JFunc function = compiler.endCompiler();

        function.name = node.var_name_tok != null ? node.var_name_tok.value.toString() : "<anonymous>";
        function.async = node.async;
        function.returnType = retype;

        function.args = node.argname;
        function.kwargs = node.kwargname;

        function.catcher = node.catcher;

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
            emit(OpCode.Pop, node.left_node.pos_start, node.left_node.pos_end);
            compile(node.right_node);
            patchJump(jump);
            return;
        }
        else if (node.op_tok == TokenType.Pipe) {
            compile(node.left_node);
            int jump = emitJump(OpCode.JumpIfTrue, node.left_node.pos_start, node.left_node.pos_end);
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
            emit(OpCode.NullErr, 1, node.pos_start, node.pos_end);
            compile(node.left_node);
            emit(OpCode.IncrNullErr, node.pos_start, node.pos_end);
            compile(node.right_node);
            emit(OpCode.Chain, node.pos_start, node.pos_end);
            emit(OpCode.NullErr, 0, node.pos_start, node.pos_end);
            return;
        }

        compile(node.left_node);
        compile(node.right_node);
        switch (node.op_tok) {
            case Plus -> emit(OpCode.Add, node.pos_start, node.pos_end);
            case Minus -> emit(OpCode.Subtract, node.pos_start, node.pos_end);
            case Star -> emit(OpCode.Multiply, node.pos_start, node.pos_end);
            case Slash -> emit(OpCode.Divide, node.pos_start, node.pos_end);
            case Percent -> emit(OpCode.Modulo, node.pos_start, node.pos_end);
            case Caret -> emit(OpCode.Power, node.pos_start, node.pos_end);

            case EqualEqual -> emit(OpCode.Equal, node.pos_start, node.pos_end);
            case BangEqual -> emit(new int[]{ OpCode.Equal, OpCode.Not }, node.pos_start, node.pos_end);
            case RightAngle -> emit(OpCode.GreaterThan, node.pos_start, node.pos_end);
            case LeftAngle -> emit(OpCode.LessThan, node.pos_start, node.pos_end);
            case GreaterEquals -> emit(new int[]{ OpCode.LessThan, OpCode.Not }, node.pos_start, node.pos_end);
            case LessEquals -> emit(new int[]{ OpCode.GreaterThan, OpCode.Not }, node.pos_start, node.pos_end);

            case LeftBracket -> emit(OpCode.Index, node.pos_start, node.pos_end);
            case Dot -> emit(OpCode.Get, node.pos_start, node.pos_end);

            case TildeAmpersand -> emit(OpCode.BitAnd, node.pos_start, node.pos_end);
            case TildePipe -> emit(OpCode.BitOr, node.pos_start, node.pos_end);
            case TildeCaret -> emit(OpCode.BitXor, node.pos_start, node.pos_end);
            case LeftTildeArrow -> emit(OpCode.LeftShift, node.pos_start, node.pos_end);
            case TildeTilde -> emit(OpCode.RightShift, node.pos_start, node.pos_end);
            case RightTildeArrow -> emit(OpCode.SignRightShift, node.pos_start, node.pos_end);

            default -> throw new RuntimeException("Unknown operator: " + node.op_tok);
        }
    }

    void compile(UnaryOpNode node) {
        compile(node.node);
        switch (node.op_tok) {
            case Plus -> {}
            case Minus -> emit(OpCode.Negate, node.pos_start, node.pos_end);
            case Bang -> emit(OpCode.Not, node.pos_start, node.pos_end);
            case PlusPlus -> emit(OpCode.Increment, node.pos_start, node.pos_end);
            case MinusMinus -> emit(OpCode.Decrement, node.pos_start, node.pos_end);
            case Tilde -> emit(OpCode.BitCompl, node.pos_start, node.pos_end);
            case DollarSign -> emit(OpCode.FromBytes, node.pos_start, node.pos_end);
            default -> throw new RuntimeException("Unknown operator: " + node.op_tok);
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
        else if (inPattern && !hasGlobal(name)) {
            arg = chunk().addConstant(new Value(name));
            addLocal(name, start, end);
            emit(OpCode.PatternVars, arg, start, end);
        }
        else {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.GetGlobal, arg, start, end);
        }
    }

    void compile(DropNode node) {
        String name = node.varTok.value.toString();
        if (macros.containsKey(name)) {
            macros.remove(name);
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
            emit(OpCode.DropGlobal, arg, node.pos_start, node.pos_end);
        }

        compileNull(node.pos_start, node.pos_end);
    }

    void compile(VarAssignNode node) {
        if (node.defining)
            compileDecl(node.var_name_tok,
                    node.type, node.locked, node.value_node,
                    node.min != null ? node.min : Integer.MIN_VALUE,
                    node.max != null ? node.max : Integer.MAX_VALUE,
                    node.pos_start, node.pos_end);
        else
            compileAssign(node.var_name_tok, node.value_node, node.pos_start, node.pos_end);
    }

    void compile(LetNode node) {
        compileDecl(node.var_name_tok, List.of("<inferred>"), false, node.value_node, Integer.MIN_VALUE, Integer.MAX_VALUE, node.pos_start, node.pos_end);
    }

    void defineVariable(int global, List<String> type, boolean constant, @NotNull Position start, @NotNull Position end) {
        defineVariable(global, type, constant, Integer.MIN_VALUE, Integer.MAX_VALUE, start, end);
    }

    void defineVariable(int global, List<String> type, boolean constant, int min, int max, @NotNull Position start, @NotNull Position end) {
        boolean usesRange = min != Integer.MIN_VALUE || max != Integer.MAX_VALUE;
        if (scopeDepth > 0) {
            markInitialized();
            emit(OpCode.DefineLocal, start, end);
            compileType(type, start, end);
            emit(constant ? 1 : 0, start, end);
            emit(usesRange ? 1 : 0, start, end);
            if (usesRange) {
                emit(min, max, start, end);
            }
            return;
        }

        globals.add(chunk().constants.values.get(global).asString());
        emit(OpCode.DefineGlobal, global, start, end);
        compileType(type, start, end);
        emit(constant ? 1 : 0, start, end);
        emit(usesRange ? 1 : 0, start, end);
        if (usesRange) {
            emit(min, max, start, end);
        }
    }

    void makeVar(int slot, List<String> type, boolean constant, @NotNull Position start, @NotNull Position end) {
        emit(OpCode.MakeVar, slot, start, end);
        compileType(type, start, end);
        emit(constant ? 1 : 0, start, end);
    }

    void addLocal(String name, @NotNull Position start, @NotNull Position end) {
        Local local = new Local(new LocalToken(name, start.idx, end.idx - start.idx), scopeDepth);

        locals[localCount++] = local;
    }

    void addGeneric(String name, @NotNull Position start, @NotNull Position end) {
        Local local = new Local(new LocalToken(name, start.idx, end.idx - start.idx), scopeDepth);

        generics[localCount++] = local;
        locals[localCount - 1] = local;
    }

    void declareVariable(Token varNameTok, @NotNull Position start, @NotNull Position end) {
        if (scopeDepth == 0)
            return;

        String name = varNameTok.value.toString();
        addLocal(name, start, end);
    }

    int parseVariable(Token varNameTok, @NotNull Position start, @NotNull Position end) {
        declareVariable(varNameTok, start, end);
        if (scopeDepth > 0)
            return 0;

        return chunk().addConstant(new Value(varNameTok.value.toString()));
    }

    void compileDecl(Token varNameTok, List<String> type, boolean locked, Node value,
                     int min, int max, @NotNull Position start, @NotNull Position end) {
        compile(value);
        int global = parseVariable(varNameTok, start, end);
        defineVariable(global, type, locked, min, max, start, end);
    }

    void compileAssign(Token varNameTok, Node value, @NotNull Position start, @NotNull Position end) {
        String name = varNameTok.value.toString();
        int arg = resolveLocal(name);

        compile(value);

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
        Compiler scope = new Compiler(this, FunctionType.Scope, chunk().source);

        scope.beginScope();
        method.compile(scope);
        scope.emit(OpCode.Return, start, end);
        scope.endScope(start, end);

        JFunc func = scope.endCompiler();
        func.name = scopeName;
        func.returnType = List.of("any");

        emit(new int[]{ OpCode.Closure, chunk().addConstant(new Value(func)), 0 }, start, end);
        for (int i = 0; i < func.upvalueCount; i++)
            emit(scope.upvalues[i].isLocal ? 1 : 0, scope.upvalues[i].index,
                    start, end);
        emit(new int[]{ OpCode.Call, 0, 0, 0 }, start, end);
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
            compile(nodeCase.statements);

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

            compile(node.else_case.statements);

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
        continueTo = loopStart;

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

        continueTo = 0;
        patchBreaks();
        for (int i = 0; i < popCount; i++)
            emit(OpCode.Pop, body.pos_start, body.pos_end);
        patchJump(pastJump);
    }

    String compileType(String type) {
        int g = resolveGeneric(type);
        if (g != -1) {
            return "@" + g;
        }
        else {
            return type;
        }
    }

    void compileType(List<String> type, @NotNull Position start, @NotNull Position end) {
        compileType(type, start, end, true);
    }

    List<String> compileType(List<String> type, @NotNull Position start, @NotNull Position end, boolean emit) {
        List<String> compiled = new ArrayList<>();
        for (String t : type) {
            compiled.add(compileType(t));
        }
        Value compiledval = Value.fromType(compiled);
        int constant = chunk().addConstant(compiledval);
        if (emit)
            emit(constant, start, end);
        return compiled;
    }

    void compile(ClassDefNode node) {
        String name = node.class_name_tok.value.toString();
        int nameConstant = chunk().addConstant(new Value(name));
        declareVariable(node.class_name_tok, node.class_name_tok.pos_start, node.class_name_tok.pos_end);

        for (int i = node.attributes.size() - 1; i >= 0; i--) {
            Node def = node.attributes.get(i).nValue;
            if (def != null)
                compile(def);
            else
                compileNull(node.attributes.get(i).pos_start, node.attributes.get(i).pos_end);
        }

        for (int i = 0; i < node.generic_toks.size(); i++)
            compileNull(node.pos_start, node.pos_end);

        if (node.parentToken != null) accessVariable(node.parentToken.value.toString(), node.parentToken.pos_start, node.parentToken.pos_end);

        emit(OpCode.Class, nameConstant, node.pos_start, node.pos_end);
        emit(node.parentToken != null ? 1 : 0, node.pos_start, node.pos_end);

        emit(node.attributes.size() + node.generic_toks.size(), node.pos_start, node.pos_end);

        for (Token tok : node.generic_toks) 
            compile(new AttrDeclareNode(
                tok,
                List.of("String"),
                false,
                true,
                null
            ));

        for (AttrDeclareNode attr : node.attributes)
            compile(attr);

        emit(node.generic_toks.size(), node.pos_start, node.pos_end);
        for (Token tok : node.generic_toks)
            emit(chunk().addConstant(new Value(tok.value.toString())), node.pos_start, node.pos_end);

        defineVariable(nameConstant, List.of("recipe"), true, node.pos_start, node.pos_end);

        for (MethDefNode method : node.methods)
            compile(method);

        Position constructorStart = node.make_node.pos_start;
        Position constructorEnd = node.make_node.pos_end;
        compile(new MethDefNode(
                new Token(TokenType.Identifier, "<make>", constructorStart, constructorEnd),
                node.arg_name_toks,
                node.arg_type_toks,
                node.make_node,
                false,
                false,
                false,
                List.of("void"),
                node.defaults,
                node.defaultCount,
                node.generic_toks,
                false,
                false,
                node.argname,
                node.kwargname
        ), true, node.generic_toks);

        emit(OpCode.Pop, node.pos_start, node.pos_end);
        compileNull(node.pos_start, node.pos_end);
    }

    void compile(MethDefNode node) {
        compile(node, false, null);
    }

    void compile(MethDefNode node, boolean isConstructor, List<Token> genericToks) {
        String name = node.var_name_tok.value.toString();
        int nameConstant = chunk().addConstant(new Value(name));

        FunctionType type = isConstructor ? FunctionType.Constructor : FunctionType.Method;

        function(type, node.asFuncDef(),
                (compiler) -> {
                    if (isConstructor) for (Token tok : genericToks) {
                        compiler.compile(new AttrAssignNode(
                            tok,
                            new VarAccessNode(tok)
                        ));
                    }
                },
                (compiler) -> {}
        );

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
        compileType(node.type, node.pos_start, node.pos_end);
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

        String name = node.var_name_tok.value.toString();
        copyVar(node.var_name_tok, "num", node.start_value_node);

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
        ), "list", node.iterable_node);
        compileDecl(node.var_name_tok,
                List.of("any"),
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

    void copyVar(Token varNameTok, String type, Node startValueNode) {
        int global = parseVariable(varNameTok, varNameTok.pos_start, varNameTok.pos_end);
        compile(startValueNode);
        emit(OpCode.Copy, startValueNode.pos_start, startValueNode.pos_end);
        defineVariable(global, List.of(type), false, varNameTok.pos_start, startValueNode.pos_end);
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

}
