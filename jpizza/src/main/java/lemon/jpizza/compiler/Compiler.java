package lemon.jpizza.compiler;

import lemon.jpizza.Position;
import lemon.jpizza.cases.Case;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.definitions.VarAssignNode;
import lemon.jpizza.nodes.expressions.*;
import lemon.jpizza.nodes.operations.BinOpNode;
import lemon.jpizza.nodes.operations.UnaryOpNode;
import lemon.jpizza.nodes.values.*;
import lemon.jpizza.nodes.variables.VarAccessNode;

import java.util.ArrayList;
import java.util.List;

public class Compiler {
    static record LocalToken(String name, int idx, int len) {}
    static record Local(LocalToken name, int depth) {}

    public Chunk chunk;
    List<Local> locals;
    int localCount;
    int scopeDepth;

    public Compiler(Chunk chunk) {
        this.chunk = chunk;
        this.locals = new ArrayList<>();
        this.localCount = 0;
        this.scopeDepth = 0;
    }

    void beginScope() {
        this.scopeDepth++;
    }

    void endScope(Position start, Position end) {
        while (localCount > 0 && locals.get(localCount - 1).depth == scopeDepth) {
            emit(OpCode.Pop, start, end);
            locals.remove(this.locals.size() - 1);
            localCount--;
        }
        scopeDepth--;
    }

    int resolveLocal(String name) {
        for (int i = 0; i < locals.size(); i++) {
            Local local = locals.get(locals.size() - 1 - i);
            if (local.name.name.equals(name)) return locals.size() - 1 - i;
        }
        return -1;
    }

    void emit(int b, Position start, Position end) {
        chunk.write(b, start.idx, end.idx - start.idx);
    }

    void emit(int[] bs, Position start, Position end) {
        for (int b : bs) chunk.write(b, start.idx, end.idx - start.idx);
    }

    void emit(int op, int b, Position start, Position end) {
        chunk.write(op, start.idx, end.idx - start.idx);
        chunk.write(b, start.idx, end.idx - start.idx);
    }

    int emitJump(int op, Position start, Position end) {
        emit(op, start, end);
        emit(0xff, start, end);
        return chunk.code.size() - 1;
    }

    void emitLoop(int loopStart, Position start, Position end) {
        emit(OpCode.Loop, start, end);

        int offset = chunk.code.size() - loopStart + 1;

        emit(offset, start, end);
    }

    void patchJump(int offset) {
        int jump = chunk.code.size() - offset - 1;
        chunk.code.set(offset, jump);
    }

    public void compileBlock(List<Node> statements) {
        for (Node statement : statements) {
            compileStatement(statement);
            emit(OpCode.Pop, statement.pos_start, statement.pos_end);
        }
        emit(OpCode.Return, statements.get(0).pos_start, statements.get(statements.size() - 1).pos_end);
    }

    void compileStatement(Node statement) {
        if (statement instanceof BinOpNode) {
            compileBinOp((BinOpNode) statement);
        }
        else if (statement instanceof UnaryOpNode) {
            compileUnaryOp((UnaryOpNode) statement);
        }
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
        else if (statement instanceof NullNode) {
            compileNull(statement.pos_start, statement.pos_end);
        }
        else if (statement instanceof BodyNode) {
            BodyNode node = (BodyNode) statement;
            for (Node stmt : node.statements) {
                compileStatement(stmt);
                emit(OpCode.Pop, stmt.pos_start, stmt.pos_end);
            }
            compileNull(node.pos_start, node.pos_end);
        }
        else if (statement instanceof VarAssignNode) {
            compileVarAssign((VarAssignNode) statement);
        }
        else if (statement instanceof VarAccessNode) {
            compileVarAccess((VarAccessNode) statement);
        }
        else if (statement instanceof ScopeNode) {
            compileScope((ScopeNode) statement);
        }
        else if (statement instanceof QueryNode) {
            compileQuery((QueryNode) statement);
        }
        else if (statement instanceof WhileNode) {
            compileWhile((WhileNode) statement);
        }
        else if (statement instanceof ForNode) {
            compileFor((ForNode) statement);
        }
        else if (statement instanceof PassNode) {
            compileNull(statement.pos_start, statement.pos_end);
        }
        else {
            throw new RuntimeException("Unknown statement type: " + statement.getClass().getName());
        }
    }

    void compileNull(Position start, Position end) {
        int constant = chunk.addConstant(new Value());
        emit(OpCode.Constant, constant, start, end);
    }

    void compileBoolean(boolean val, Position start, Position end) {
        int constant = chunk.addConstant(new Value(val));
        emit(OpCode.Constant, constant, start, end);
    }

    void compileNumber(double val, Position start, Position end) {
        int constant = chunk.addConstant(new Value(val));
        emit(OpCode.Constant, constant, start, end);
    }

    void compileString(String val, Position start, Position end) {
        int constant = chunk.addConstant(new Value(val));
        emit(OpCode.Constant, constant, start, end);
    }

    void compileBinOp(BinOpNode node) {

        compileStatement(node.left_node);
        compileStatement(node.right_node);
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

    void compileUnaryOp(UnaryOpNode node) {
        compileStatement(node.node);
        switch (node.op_tok) {
            case PLUS -> {}
            case MINUS -> emit(OpCode.Negate, node.pos_start, node.pos_end);
            case NOT -> emit(OpCode.Not, node.pos_start, node.pos_end);
            case INCR -> emit(OpCode.Increment, node.pos_start, node.pos_end);
            case DECR -> emit(OpCode.Decrement, node.pos_start, node.pos_end);
            default -> throw new RuntimeException("Unknown operator: " + node.op_tok);
        }
    }

    void compileVarAccess(VarAccessNode node) {
        String name = node.var_name_tok.value.toString();
        int arg = resolveLocal(name);

        if (arg == -1) {
            arg = chunk.addConstant(new Value(name));
            emit(OpCode.GetGlobal, arg, node.pos_start, node.pos_end);
        }
        else {
            emit(OpCode.GetLocal, arg, node.pos_start, node.pos_end);
        }
    }

    void compileVarAssign(VarAssignNode node) {
        if (node.defining)
            compileDecl(node);
        else
            compileAssign(node);
    }

    void defineVariable(int global, List<String> type, boolean constant, Position start, Position end) {
        int typepointer = chunk.addConstant(new Value(type, true));

        if (scopeDepth > 0) {
            emit(OpCode.DefineLocal, start, end);
            emit(constant ? 1 : 0, typepointer, start, end);
            compileNull(start, end);
            return;
        }

        emit(OpCode.DefineGlobal, global, start, end);
        emit(constant ? 1 : 0, typepointer, start, end);
    }

    void addLocal(String name, Position start, Position end) {
        Local local = new Local(new LocalToken(name, start.idx, end.idx - start.idx), scopeDepth);

        locals.add(local);
        localCount++;
    }

    void declareVariable(VarAssignNode node) {
        if (scopeDepth == 0)
            return;

        String name = node.var_name_tok.value.toString();

        addLocal(name, node.pos_start, node.pos_end);
    }

    int parseVariable(VarAssignNode node) {
        declareVariable(node);
        if (scopeDepth > 0)
            return 0;

        return chunk.addConstant(new Value(node.var_name_tok.value.toString()));
    }

    void compileDecl(VarAssignNode node) {
        int global = parseVariable(node);
        compileStatement(node.value_node);
        defineVariable(global, node.type, node.locked, node.pos_start, node.pos_end);
    }

    void compileAssign(VarAssignNode node) {
        String name = node.var_name_tok.value.toString();
        int arg = resolveLocal(name);

        compileStatement(node.value_node);

        if (arg == -1) {
            arg = chunk.addConstant(new Value(name));
            emit(OpCode.SetGlobal, arg, node.pos_start, node.pos_end);
        }
        else {
            emit(OpCode.SetLocal, arg, node.pos_start, node.pos_end);
        }
    }

    void compileScope(ScopeNode node) {
        beginScope();

        boolean hasName = node.scopeName != null;
        if (hasName) {
            int arg = chunk.addConstant(new Value(node.scopeName));
            emit(OpCode.PushTraceback, arg, node.pos_start, node.pos_end);
        }

        compileStatement(node.statements);

        if (hasName)
            emit(OpCode.PopTraceback, node.pos_start, node.pos_end);

        endScope(node.pos_start, node.pos_end);
    }

    void compileQuery(QueryNode node) {
        List<Integer> jumps = new ArrayList<>();
        int lastJump = 0;

        for (Case nodeCase : node.cases) {
            if (lastJump != 0) {
                patchJump(lastJump);
                emit(OpCode.Pop, nodeCase.condition.pos_start, nodeCase.condition.pos_end);
            }

            compileStatement(nodeCase.condition);
            lastJump = emitJump(OpCode.JumpIfFalse, nodeCase.condition.pos_start, nodeCase.condition.pos_end);
            compileStatement(nodeCase.statements);

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

            compileStatement(node.else_case.statements);

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
        beginScope();
        compileStatement(body);
        endScope(body.pos_start, body.pos_end);

        if (returnsNull) {
            emit(OpCode.Pop, body.pos_start, body.pos_end);
        }
        else {
            emit(OpCode.CollectLoop, body.pos_start, body.pos_end);
        }

        emitLoop(loopStart, body.pos_start, body.pos_end);
    }

    // CollectLoop adds the previous value to the loop stack in the VM
    // FlushLoop turns the values in the loop stack into an array, and
    // pushes it onto the stack, clearing the loop stack
    void compileWhile(WhileNode node) {
        boolean isDoWhile = node.conLast;

        if (!node.retnull) emit(OpCode.StartCache, node.pos_start, node.pos_end);

        int loopStart = chunk.code.size();

        int jump = -1;
        if (!isDoWhile) {
            compileStatement(node.condition_node);
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
    
    void compileFor(ForNode node) {
        if (!node.retnull) emit(OpCode.StartCache, node.pos_start, node.pos_end);
        beginScope();

        String name = node.var_name_tok.value.toString();
        addLocal(name, node.pos_start, node.pos_end);
        compileStatement(node.start_value_node);
        int firstSkip = emitJump(OpCode.Jump, node.start_value_node.pos_start, node.start_value_node.pos_end);

        int loopStart = chunk.code.size();

        compileStatement(node.end_value_node);
        if (node.step_value_node != null) {
            compileStatement(node.step_value_node);
        }
        else {
            compileNumber(1, node.pos_start, node.pos_end);
        }

        emit(OpCode.For, node.pos_start, node.pos_end);
        emit(resolveLocal(name), node.pos_start, node.pos_end);

        emit(0xff, node.pos_start, node.pos_end);
        int jump = chunk.code.size() - 1;

        patchJump(firstSkip);
        loopBody(node.body_node, node.retnull, loopStart);

        int offset = chunk.code.size() - jump - 1;
        chunk.code.set(jump, offset);
        endScope(node.pos_start, node.pos_end);

        if (node.retnull) {
            compileNull(node.pos_start, node.pos_end);
        }
        else {
            emit(OpCode.FlushLoop, node.pos_start, node.pos_end);
        }
    }
    
}
