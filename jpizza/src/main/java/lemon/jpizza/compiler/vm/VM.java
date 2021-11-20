package lemon.jpizza.compiler.vm;

import lemon.jpizza.Constants;
import lemon.jpizza.Shell;
import lemon.jpizza.compiler.Chunk;
import lemon.jpizza.compiler.Disassembler;
import lemon.jpizza.compiler.FlatPosition;
import lemon.jpizza.compiler.OpCode;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.Var;

import java.util.*;

public class VM {
    private static final int MAX_STACK_SIZE = 256;

    private static record Traceback(String filename, String context, int offset) {}

    Chunk chunk;
    int ip;
    Value[] stack;
    int stackTop;
    Stack<Traceback> tracebacks;
    Map<String, Var> globals;
    Stack<List<Value>> loopCache;

    public VM() {
        this.chunk = null;
        this.globals = new HashMap<>();
        this.ip = 0;
        this.stack = new Value[MAX_STACK_SIZE];
        this.stackTop = 0;
        this.tracebacks = new Stack<>();
        this.loopCache = new Stack<>();
    }

    public VM setChunk(String name, Chunk chunk) {
        this.chunk = chunk;
        tracebacks.push(new Traceback(name, name, 0));
        return this;
    }

    void push(Value value) {
        stack[stackTop++] = value;
    }

    Value pop() {
        return stack[--stackTop];
    }

    protected void runtimeError(String message, String reason, FlatPosition position) {
        int idx = position.index;
        int len = position.len;

        String output = "";
        if (!tracebacks.empty()) {
            String arrow = Shell.fileEncoding.equals("UTF-8") ? "╰──►\uD83E\uDC12" : "--->";

            // Generate traceback
            Traceback last = tracebacks.peek();
            while (!tracebacks.empty()) {
                Traceback top = tracebacks.pop();
                int line = Constants.indexToLine(chunk.source(), top.offset);
                output = String.format("  %s  File %s, line %s, in %s\n%s", arrow, top.filename, line, top.context, output);
            }
            output = "Traceback (most recent call last):\n" + output;

            // Generate error message
            int line = Constants.indexToLine(chunk.source(), idx);
            output += String.format("\n%s Error (Runtime): %s\nFile %s, line %s\n%s\n",
                                    message, reason,
                                    last.filename, line + 1,
                                    Constants.highlightFlat(chunk.source(), idx, len));
        }
        else {
            output = String.format("%s Error (Runtime): %s\n", message, reason);
        }

        Shell.logger.fail(output);
        resetStack();
    }

    void resetStack() {
        stackTop = 0;
    }

    public void free() {
        chunk.free();
        chunk = null;
        globals.clear();
        ip = 0;
        resetStack();
    }

    public void init() {
        resetStack();
    }

    String readString() {
        return chunk.constants().values.get(chunk.code().get(ip++)).asString();
    }

    int readByte() {
        return chunk.code().get(ip++);
    }

    Value peek(int offset) {
        return stack[stackTop - 1 - offset];
    }

    int isFalsey(Value value) {
        return !value.asBool() ? 1 : 0;
    }

    FlatPosition currentPos() {
        return chunk.getPosition(ip - 1);
    }

    VMResult binary(int op) {
        Value b = pop();
        Value a = pop();

        switch (op) {
            case OpCode.Add -> {
                if (a.isString) {
                    push(new Value(a.asString() + b.asString()));
                }
                else {
                    push(new Value(a.asNumber() + b.asNumber()));
                }
            }
            case OpCode.Subtract -> push(new Value(a.asNumber() - b.asNumber()));
            case OpCode.Multiply -> push(new Value(a.asNumber() * b.asNumber()));
            case OpCode.Divide -> push(new Value(a.asNumber() / b.asNumber()));
            case OpCode.Modulo -> push(new Value(a.asNumber() % b.asNumber()));
            case OpCode.Power -> push(new Value(Math.pow(a.asNumber(), b.asNumber())));
        }

        return VMResult.OK;
    }

    VMResult unary(int op) {
        Value a = pop();

        switch (op) {
            case OpCode.Increment -> push(new Value(a.asNumber() + 1));
            case OpCode.Decrement -> push(new Value(a.asNumber() - 1));
            case OpCode.Negate -> push(new Value(-a.asNumber()));
            case OpCode.Not -> push(new Value(!a.asBool()));
        }

        return VMResult.OK;
    }

    VMResult globalOps(int op) {
        return switch (op) {
            case OpCode.DefineGlobal -> {
                String name = readString();
                Value value = peek(0);

                boolean constant = readByte() == 1;
                String type = readString();

                globals.put(name, new Var(type, value, constant));
                yield VMResult.OK;
            }
            case OpCode.GetGlobal -> {
                String name = readString();
                Var value = globals.get(name);

                if (value == null) {
                    runtimeError("Scope", "Undefined variable", currentPos());
                    yield VMResult.ERROR;
                }

                push(value.val);
                yield VMResult.OK;
            }
            case OpCode.SetGlobal -> {
                String name = readString();
                Value value = peek(0);

                Var var = globals.get(name);
                if (var != null) {
                    if (var.constant) {
                        runtimeError("Scope", "Cannot reassign constant", currentPos());
                        yield VMResult.ERROR;
                    }
                    if (!var.type.equals("any") && !var.type.equals(value.type())) {
                        runtimeError("Type",
                                String.format("Got type %s, expected type %s", value.type(), var.type),
                                currentPos());
                        yield VMResult.ERROR;
                    }
                    var.val(value);
                }
                else {
                    runtimeError("Scope", "Undefined variable", currentPos());
                    yield VMResult.ERROR;
                }
                yield VMResult.OK;
            }
            default -> VMResult.OK;
        };
    }

    VMResult comparison(int op) {
        Value b = pop();
        Value a = pop();

        switch (op) {
            case OpCode.Equal -> push(new Value(a.equals(b)));
            case OpCode.GreaterThan -> push(new Value(a.asNumber() > b.asNumber()));
            case OpCode.LessThan -> push(new Value(a.asNumber() < b.asNumber()));
        }

        return VMResult.OK;
    }

    VMResult localOps(int op) {
        return switch (op) {
            case OpCode.GetLocal -> {
                int slot = readByte();
                if (stackTop - slot <= 0) {
                    runtimeError("Scope", "Undefined variable", currentPos());
                    yield VMResult.ERROR;
                }

                Value val = stack[slot];
                if (val.isVar)
                    push(stack[slot].asVar().val);
                else
                    push(stack[slot]);
                yield VMResult.OK;
            }
            case OpCode.SetLocal -> {
                int slot = readByte();

                Value var = stack[slot];
                Value val = peek(0);
                if (var != null && var.isVar) {
                    Var v = var.asVar();
                    if (v.constant) {
                        runtimeError("Scope", "Cannot reassign constant", currentPos());
                        yield VMResult.ERROR;
                    }
                    if (!"any".equals(v.type) && !val.type().equals(v.type)) {
                        runtimeError("Type",
                                String.format("Got type %s, expected type %s", val.type(), v.type),
                                currentPos());
                        yield VMResult.ERROR;
                    }
                    v.val(val);
                }

                yield VMResult.OK;
            }
            case OpCode.DefineLocal -> {
                Value val = pop();

                boolean constant = readByte() == 1;
                String type = readString();

                push(new Value(new Var(type, val, constant)));

                yield VMResult.OK;
            }

            default -> VMResult.OK;
        };
    }

    VMResult loopOps(int op) {
        switch (op) {
            case OpCode.Loop -> {
                int offset = readByte();
                ip -= offset;
            }

            case OpCode.StartCache -> loopCache.push(new ArrayList<>());
            case OpCode.CollectLoop -> loopCache.peek().add(pop());
            case OpCode.FlushLoop -> push(new Value(new ArrayList<>(loopCache.pop())));
        }

        return VMResult.OK;
    }

    VMResult jumpOps(int op) {
        switch (op) {
            case OpCode.JumpIfFalse -> {
                int offset = readByte();
                ip += offset * isFalsey(peek(0));
            }
            case OpCode.JumpIfTrue -> {
                int offset = readByte();
                ip += offset * (1 - isFalsey(peek(0)));
            }
            case OpCode.Jump -> {
                int offset = readByte();
                ip += offset;
            }
        }
        return VMResult.OK;
    }

    VMResult forLoop() {
        Value step = pop();
        double end = pop().asNumber();

        int slot = readByte();
        int jump = readByte();

        VMResult res = stack[slot].add(step);
        if (res != VMResult.OK) {
            return res;
        }

        double i = stack[slot].asNumber();
        if ((i >= end && step.asNumber() >= 1) || (i <= end && step.asNumber() < 1)) {
            ip += jump;
        }

        return VMResult.OK;
    }

    public VMResult run() {
        while (true) {
            Shell.logger.debug("          ");
            for (int i = 0; i < stackTop; i++) {
                Shell.logger.debug("[ ");
                Shell.logger.debug(stack[i].toString());
                Shell.logger.debug(" ]");
            }
            Shell.logger.debug("\n");

            Disassembler.disassembleInstruction(chunk, ip);

            int instruction = readByte();

            VMResult res = switch (instruction) {
                case OpCode.Return -> VMResult.EXIT;
                case OpCode.Constant -> {
                    push(chunk.constants().values.get(readByte()));
                    yield VMResult.OK;
                }

                case OpCode.Add,
                        OpCode.Subtract,
                        OpCode.Multiply,
                        OpCode.Divide,
                        OpCode.Modulo,
                        OpCode.Power -> binary(instruction);

                case OpCode.Increment,
                        OpCode.Decrement,
                        OpCode.Negate,
                        OpCode.Not -> unary(instruction);

                case OpCode.Equal,
                        OpCode.GreaterThan,
                        OpCode.LessThan -> comparison(instruction);

                case OpCode.Pop -> {
                    pop();
                    yield VMResult.OK;
                }

                case OpCode.DefineGlobal,
                        OpCode.GetGlobal,
                        OpCode.SetGlobal -> globalOps(instruction);

                case OpCode.GetLocal,
                        OpCode.SetLocal,
                        OpCode.DefineLocal -> localOps(instruction);

                case OpCode.PushTraceback -> {
                    String name = readString();
                    int idx = currentPos().index;

                    String filename;
                    if (tracebacks.empty()) {
                        filename = "";
                    }
                    else {
                        filename = tracebacks.peek().filename;
                    }

                    tracebacks.push(new Traceback(filename, name, idx));
                    yield VMResult.OK;
                }
                case OpCode.PopTraceback -> {
                    tracebacks.pop();
                    yield VMResult.OK;
                }

                case OpCode.JumpIfFalse,
                        OpCode.JumpIfTrue,
                        OpCode.Jump -> jumpOps(instruction);

                case OpCode.Loop,
                        OpCode.StartCache,
                        OpCode.CollectLoop,
                        OpCode.FlushLoop -> loopOps(instruction);

                case OpCode.For -> forLoop();

                default -> throw new RuntimeException("Unknown opcode: " + instruction);
            };
            if (res == VMResult.EXIT) {
                return VMResult.OK;
            }
            else if (res == VMResult.ERROR) {
                return VMResult.ERROR;
            }
        }
    }

}
