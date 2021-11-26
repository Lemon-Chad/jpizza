package lemon.jpizza.compiler;

import lemon.jpizza.Position;
import lemon.jpizza.Shell;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens;
import lemon.jpizza.cases.Case;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.definitions.*;
import lemon.jpizza.nodes.expressions.*;
import lemon.jpizza.nodes.operations.BinOpNode;
import lemon.jpizza.nodes.operations.UnaryOpNode;
import lemon.jpizza.nodes.values.*;
import lemon.jpizza.nodes.variables.VarAccessNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Compiler {

    static record LocalToken(String name, int idx, int len) {}
    static class Local {
        LocalToken name;
        int depth;

        Local(LocalToken name, int depth) {
            this.name = name;
            this.depth = depth;
        }

        public LocalToken name() {
            return name;
        }
    }

    static record Upvalue(int index, boolean isLocal) {}

    Compiler enclosing;

    Local[] locals;
    Local[] generics;
    int localCount;
    int scopeDepth;

    JFunc function;
    FunctionType type;

    int continueTo;
    List<Integer> breaks;

    Upvalue[] upvalues;

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

        this.upvalues = new Upvalue[256];

        this.localCount = 0;
        this.scopeDepth = 0;

        locals[localCount++] = new Local(new LocalToken(type == FunctionType.Method ? "this" : "", 0, 0), 0);

        this.enclosing = enclosing;

        this.continueTo = 0;
        this.breaks = new ArrayList<>();
    }

    void beginScope() {
        this.scopeDepth++;
    }

    void endScope(Position start, Position end) {
        destack(locals, start, end);
        destack(generics, start, end);
        scopeDepth--;
    }

    void destack(Local[] locals, Position start, Position end) {
        int offs = 0;
        while (localCount - offs > 0) {
            Local curr = locals[localCount - 1 - offs];
            if (curr == null) {
                offs++;
                continue;
            }
            if (curr.depth != scopeDepth) {
                break;
            }
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

        return -1;
    }

    void emit(int b, Position start, Position end) {
        chunk().write(b, start.idx, end.idx - start.idx);
    }

    void emit(int[] bs, Position start, Position end) {
        for (int b : bs) 
            chunk().write(b, start.idx, end.idx - start.idx);
    }

    void emit(int op, int b, Position start, Position end) {
        chunk().write(op, start.idx, end.idx - start.idx);
        chunk().write(b, start.idx, end.idx - start.idx);
    }

    int emitJump(int op, Position start, Position end) {
        emit(op, start, end);
        emit(0xff, start, end);
        return chunk().code.size() - 1;
    }

    void emitLoop(int loopStart, Position start, Position end) {
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
        return function;
    }

    void compile(Node statement) {
        if (statement instanceof BinOpNode)
            compile((BinOpNode) statement);

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

        else if (statement instanceof ContinueNode)
            emitLoop(continueTo, statement.pos_start, statement.pos_end);

        else if (statement instanceof BreakNode)
            breaks.add(emitJump(OpCode.Jump, statement.pos_start, statement.pos_end));

        else if (statement instanceof ClaccessNode) {
            ClaccessNode node = (ClaccessNode) statement;
            compile(node.class_tok);
            int constant = chunk().addConstant(new Value(node.attr_name_tok.value.toString()));
            emit(OpCode.Access, constant, node.pos_start, node.pos_end);
        }

        else
            throw new RuntimeException("Unknown statement type: " + statement.getClass().getName());
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
        compile(node.nodeToCall);
        for (Node arg : node.argNodes)
            compile(arg);
        emit(OpCode.Call, node.argNodes.size(), node.pos_start, node.pos_end);
    }

    void compile(FuncDefNode node) {
        int global = parseVariable(node.var_name_tok, node.pos_start, node.pos_end);
        markInitialized();
        function(FunctionType.Function, node);
        defineVariable(global, List.of("function"), false, node.pos_start, node.pos_end);
    }

    void markInitialized() {
        if (scopeDepth == 0) return;
        locals[localCount - 1].depth = scopeDepth;
    }

    void function(FunctionType type, FuncDefNode node) {
        Compiler compiler = new Compiler(this, type, chunk().source);

        compiler.beginScope();

        for (int i = 0; i < node.arg_name_toks.size(); i++) {
            compiler.function.arity++;
            Token param = node.arg_name_toks.get(i);
            Token paramType = node.arg_type_toks.get(i);
            compiler.parseVariable(param, param.pos_start, param.pos_end);
            compiler.makeVar(compiler.localCount - 1, (List<String>) paramType.value, false, param.pos_start, param.pos_end);
        }

        compiler.compile(node.body_node);
        compiler.emit(OpCode.Return, node.body_node.pos_start, node.body_node.pos_end);
        JFunc function = compiler.endCompiler();

        function.name = node.var_name_tok.value.toString();
        function.returnType = compileType(node.returnType, node.pos_start, node.pos_end, false);

        emit(OpCode.Closure, chunk().addConstant(new Value(function)), node.pos_start, node.pos_end);

        for (int i = 0; i < function.upvalueCount; i++)
            emit(compiler.upvalues[i].isLocal ? 1 : 0, compiler.upvalues[i].index,
                    node.pos_start, node.pos_end);

    }

    void compileNull(Position start, Position end) {
        int constant = chunk().addConstant(new Value());
        emit(OpCode.Constant, constant, start, end);
    }

    void compileBoolean(boolean val, Position start, Position end) {
        int constant = chunk().addConstant(new Value(val));
        emit(OpCode.Constant, constant, start, end);
    }

    void compileNumber(double val, Position start, Position end) {
        int constant = chunk().addConstant(new Value(val));
        emit(OpCode.Constant, constant, start, end);
    }

    void compileString(String val, Position start, Position end) {
        int constant = chunk().addConstant(new Value(val));
        emit(OpCode.Constant, constant, start, end);
    }

    void compile(BinOpNode node) {

        compile(node.left_node);
        compile(node.right_node);
        switch (node.op_tok) {
            case PLUS -> emit(OpCode.Add, node.pos_start, node.pos_end);
            case MINUS -> emit(OpCode.Subtract, node.pos_start, node.pos_end);
            case MUL -> emit(OpCode.Multiply, node.pos_start, node.pos_end);
            case DIV -> emit(OpCode.Divide, node.pos_start, node.pos_end);
            case MOD -> emit(OpCode.Modulo, node.pos_start, node.pos_end);
            case POWER -> emit(OpCode.Power, node.pos_start, node.pos_end);

            case EE -> emit(OpCode.Equal, node.pos_start, node.pos_end);
            case NE -> emit(new int[]{ OpCode.Equal, OpCode.Not }, node.pos_start, node.pos_end);
            case GT -> emit(OpCode.GreaterThan, node.pos_start, node.pos_end);
            case LT -> emit(OpCode.LessThan, node.pos_start, node.pos_end);
            case GTE -> emit(new int[]{ OpCode.LessThan, OpCode.Not }, node.pos_start, node.pos_end);
            case LTE -> emit(new int[]{ OpCode.GreaterThan, OpCode.Not }, node.pos_start, node.pos_end);

            default -> throw new RuntimeException("Unknown operator: " + node.op_tok);
        }
    }

    void compile(UnaryOpNode node) {
        compile(node.node);
        switch (node.op_tok) {
            case PLUS -> {}
            case MINUS -> emit(OpCode.Negate, node.pos_start, node.pos_end);
            case NOT -> emit(OpCode.Not, node.pos_start, node.pos_end);
            case INCR -> emit(OpCode.Increment, node.pos_start, node.pos_end);
            case DECR -> emit(OpCode.Decrement, node.pos_start, node.pos_end);
            default -> throw new RuntimeException("Unknown operator: " + node.op_tok);
        }
    }

    void compile(VarAccessNode node) {
        String name = node.var_name_tok.value.toString();
        accessVariable(name, node.pos_start, node.pos_end);
    }

    void accessVariable(String name, Position start, Position end) {
        int arg = resolveLocal(name);

        if (arg != -1) {
            emit(OpCode.GetLocal, arg, start, end);
        }
        else if ((arg = resolveUpvalue(name)) != -1) {
            emit(OpCode.GetUpvalue, arg, start, end);
        }
        else {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.GetGlobal, arg, start, end);
        }
    }

    void compile(VarAssignNode node) {
        if (node.defining)
            compileDecl(node.var_name_tok, node.type, node.locked, node.value_node, node.pos_start, node.pos_end);
        else
            compileAssign(node.var_name_tok, node.value_node, node.pos_start, node.pos_end);
    }

    void compile(LetNode node) {
        compileDecl(node.var_name_tok, List.of("<inferred>"), false, node.value_node, node.pos_start, node.pos_end);
    }

    void defineVariable(int global, List<String> type, boolean constant, Position start, Position end) {
        if (scopeDepth > 0) {
            markInitialized();
            emit(OpCode.DefineLocal, localCount - 1, start, end);
            compileType(type, start, end);
            emit(constant ? 1 : 0, start, end);
            compileNull(start, end);
            return;
        }

        emit(OpCode.DefineGlobal, global, start, end);
        compileType(type, start, end);
        emit(constant ? 1 : 0, start, end);
    }

    void makeVar(int slot, List<String> type, boolean constant, Position start, Position end) {
        emit(OpCode.MakeVar, slot, start, end);
        compileType(type, start, end);
        emit(constant ? 1 : 0, start, end);
    }

    void addLocal(String name, Position start, Position end) {
        Local local = new Local(new LocalToken(name, start.idx, end.idx - start.idx), scopeDepth);

        locals[localCount++] = local;
    }

    void addGeneric(String name, Position start, Position end) {
        Local local = new Local(new LocalToken(name, start.idx, end.idx - start.idx), scopeDepth);

        generics[localCount++] = local;
    }

    void declareVariable(Token varNameTok, Position start, Position end) {
        if (scopeDepth == 0)
            return;

        String name = varNameTok.value.toString();
        addLocal(name, start, end);
    }

    int parseVariable(Token varNameTok, Position start, Position end) {
        declareVariable(varNameTok, start, end);
        if (scopeDepth > 0)
            return 0;

        return chunk().addConstant(new Value(varNameTok.value.toString()));
    }

    void compileDecl(Token varNameTok, List<String> type, boolean locked, Node value, Position start, Position end) {
        int global = parseVariable(varNameTok, start, end);
        compile(value);
        defineVariable(global, type, locked, start, end);
    }

    void compileAssign(Token varNameTok, Node value, Position start, Position end) {
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

    void compile(ScopeNode node) {
        beginScope();

        boolean hasName = node.scopeName != null;
        if (hasName) {
            int arg = chunk().addConstant(new Value(node.scopeName));
            emit(OpCode.PushTraceback, arg, node.pos_start, node.pos_end);
        }

        compile(node.statements);

        if (hasName)
            emit(OpCode.PopTraceback, node.pos_start, node.pos_end);

        endScope(node.pos_start, node.pos_end);
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
        endScope(body.pos_start, body.pos_end);

        if (returnsNull) {
            emit(OpCode.Pop, body.pos_start, body.pos_end);
        }
        else {
            emit(OpCode.CollectLoop, body.pos_start, body.pos_end);
        }

        emitLoop(loopStart, body.pos_start, body.pos_end);

        continueTo = 0;
        for (int i : breaks)
            patchJump(i);
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

    void compileType(List<String> type, Position start, Position end) {
        compileType(type, start, end, true);
    }

    List<String> compileType(List<String> type, Position start, Position end, boolean emit) {
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

        emit(OpCode.Class, nameConstant, node.pos_start, node.pos_end);

        emit(node.attributes.size(), node.pos_start, node.pos_end);
        for (AttrDeclareNode attr : node.attributes)
            compile(attr);

        defineVariable(nameConstant, List.of("recipe"), true, node.pos_start, node.pos_end);

        for (MethDefNode method : node.methods)
            compile(method);

        Position constructorStart = node.make_node.pos_start;
        Position constructorEnd = node.make_node.pos_end;
        compile(new MethDefNode(
                new Token(Tokens.TT.IDENTIFIER, "<make>", constructorStart, constructorEnd),
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
        ), true);

        emit(OpCode.Pop, node.pos_start, node.pos_end);
        compileNull(node.pos_start, node.pos_end);
    }

    void compile(MethDefNode node) {
        compile(node, false);
    }

    void compile(MethDefNode node, boolean isConstructor) {
        String name = node.var_name_tok.value.toString();
        int nameConstant = chunk().addConstant(new Value(name));

        FunctionType type = isConstructor ? FunctionType.Constructor : FunctionType.Method;
        function(type, node.asFuncDef());

        emit(new int[]{
                OpCode.Method,
                nameConstant,
                node.stat ? 1 : 0,
                node.priv ? 1 : 0,
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

        int loopStart = chunk().code.size();

        int jump = -1;
        if (!isDoWhile) {
            compile(node.condition_node);
            jump = emitJump(OpCode.JumpIfFalse, node.condition_node.pos_start, node.condition_node.pos_end);
            emit(OpCode.Pop, node.body_node.pos_start, node.body_node.pos_end);
        }

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
        compileDecl(node.var_name_tok, List.of("num"), false, node.start_value_node, node.pos_start, node.pos_end);
        emit(OpCode.Pop, node.pos_start, node.pos_end);

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

        int offset = chunk().code.size() - jump - 1;
        chunk().code.set(jump, offset);
        endScope(node.pos_start, node.pos_end);

        if (node.retnull) {
            compileNull(node.pos_start, node.pos_end);
        }
        else {
            emit(OpCode.FlushLoop, node.pos_start, node.pos_end);
        }
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
