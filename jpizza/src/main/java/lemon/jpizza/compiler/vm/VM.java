package lemon.jpizza.compiler.vm;

import lemon.jpizza.Constants;
import lemon.jpizza.Shell;
import lemon.jpizza.compiler.Disassembler;
import lemon.jpizza.compiler.FlatPosition;
import lemon.jpizza.compiler.OpCode;
import lemon.jpizza.compiler.values.*;

import java.util.*;

public class VM {
    public static final int MAX_STACK_SIZE = 256;
    public static final int FRAMES_MAX = 64;
    public static final int STACK_MAX = FRAMES_MAX * 256;

    private static record Traceback(String filename, String context, int offset) {}

    Value[] stack;
    int stackTop;
    int ip;

    Stack<Traceback> tracebacks;
    Map<String, Var> globals;
    Stack<List<Value>> loopCache;

    CallFrame frame;
    CallFrame[] frames;
    int frameCount;

    public VM(JFunc function) {
        this.ip = 0;

        this.stack = new Value[MAX_STACK_SIZE];
        this.stackTop = 0;
        push(new Value(function));

        this.globals = new HashMap<>();
        this.tracebacks = new Stack<>();
        this.loopCache = new Stack<>();

        this.frames = new CallFrame[FRAMES_MAX];
        this.frameCount = 0;

        this.frame = new CallFrame(new JClosure(function), 0, 0);
        frames[frameCount++] = frame;

        setup();
    }

    void setup() {
        defineNative("print", (args) -> {
            Shell.logger.out(args[0]);
            return NativeResult.Ok();
        }, 1);
        defineNative("println", (args) -> {
            Shell.logger.outln(args[0]);
            return NativeResult.Ok();
        }, 1);

        // List Functions
        defineNative("append", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.append(value);
            return NativeResult.Ok();
        }, 2, List.of("list", "any"));
        defineNative("remove", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.remove(value);
            return NativeResult.Ok();
        }, 2, List.of("list", "any"));
        defineNative("pop", (args) -> {
            Value list = args[0];
            Value index = args[1];

            push(list.pop(index.asNumber()));
            return NativeResult.Ok();
        }, 2, List.of("list", "num"));
        defineNative("extend", (args) -> {
            Value list = args[0];
            Value other = args[1];

            list.add(other);
            return NativeResult.Ok();
        }, 2, List.of("list", "list"));
        defineNative("insert", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            list.insert(index.asNumber(), value);
            return NativeResult.Ok();
        }, 3, List.of("list", "any", "num"));
        defineNative("setIndex", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            list.set(index.asNumber(), value);
            return NativeResult.Ok();
        }, 3, List.of("list", "any", "num"));
        defineNative("size", (args) -> {
            Value list = args[0];
            push(new Value(list.asList().size()));
            return NativeResult.Ok();
        }, 1, List.of("list"));
        defineNative("choose", (args) -> {
            List<Value> list = args[0].asList();
            int max = list.size() - 1;
            int index = (int) (Math.random() * max);
            push(list.get(index));
            return NativeResult.Ok();
        }, 1, List.of("list"));
        defineNative("contains", (args) -> {
            Value list = args[0];
            Value val = args[1];
            push(new Value(list.asList().contains(val)));
            return NativeResult.Ok();
        }, 2, List.of("list", "any"));
        defineNative("sublist", (args) -> {
            Value list = args[0];
            Value start = args[1];
            Value end = args[2];

            push(new Value(list.asList().subList(start.asNumber().intValue(),
                    end.asNumber().intValue())));
            return NativeResult.Ok();
        }, 3, List.of("list", "num", "num"));
        defineNative("join", (args) -> {
            Value str = args[0];
            Value list = args[1];

            List<String> strings = new ArrayList<>();
            for (Value val : list.asList())
                strings.add(val.asString());

            push(new Value(String.join(str.asString(), strings)));
            return NativeResult.Ok();
        }, 2, List.of("str", "list"));
        defineNative("indexOf", (args) -> {
            Value list = args[0];
            Value val = args[1];
            push(new Value(list.asList().indexOf(val)));
            return NativeResult.Ok();
        }, 2, List.of("list", "any"));

    }

    void defineNative(String name, JNative.Method method, int argc) {
        globals.put(name, new Var(
                "function",
                new Value(new JNative(name, method, argc)),
                false
        ));
    }

    void defineNative(String name, JNative.Method method, int argc, List<String> types) {
        globals.put(name, new Var(
                "function",
                new Value(new JNative(name, method, argc, types)),
                false
        ));
    }

    public VM trace(String name) {
        tracebacks.push(new Traceback(name, name, 0));
        return this;
    }

    void moveIP(int offset) {
        frame.ip += offset;
        ip += offset;
    }

    void push(Value value) {
        stack[stackTop++] = value;
    }

    Value pop() {
        return stack[--stackTop];
    }

    protected void runtimeError(String message, String reason) {
        runtimeError(message, reason, currentPos());
    }

    protected void runtimeError(String message, String reason, FlatPosition position) {
        int idx = position.index;
        int len = position.len;

        String output = "";
        if (!tracebacks.empty()) {
            String arrow = Shell.fileEncoding.equals("UTF-8") ? "╰──►" : "--->";

            // Generate traceback
            Traceback last = tracebacks.peek();
            while (!tracebacks.empty()) {
                Traceback top = tracebacks.pop();
                int line = Constants.indexToLine(frame.closure.function.chunk.source(), top.offset);
                System.out.println(top.offset);
                output = String.format("  %s  File %s, line %s, in %s\n%s", arrow, top.filename, line + 1, top.context, output);
            }
            output = "Traceback (most recent call last):\n" + output;

            // Generate error message
            int line = Constants.indexToLine(frame.closure.function.chunk.source(), idx);
            output += String.format("\n%s Error (Runtime): %s\nFile %s, line %s\n%s\n",
                                    message, reason,
                                    last.filename, line + 1,
                                    Constants.highlightFlat(frame.closure.function.chunk.source(), idx, len));
        }
        else {
            output = String.format("%s Error (Runtime): %s\n", message, reason);
        }

        Shell.logger.fail(output);
        resetStack();
    }

    void resetStack() {
        stackTop = 0;
        frameCount = 0;
    }

    String readString() {
        return readConstant().asString();
    }

    Value readConstant() {
        return frame.closure.function.chunk.constants().values.get(readByte());
    }

    int readByte() {
        ip++;
        return frame.closure.function.chunk.code().get(frame.ip++);
    }

    Value peek(int offset) {
        return stack[stackTop - 1 - offset];
    }

    int isFalsey(Value value) {
        return !value.asBool() ? 1 : 0;
    }

    FlatPosition currentPos() {
        return frame.closure.function.chunk.getPosition(frame.ip - 1);
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
                String type = pop().asString();
                Value value = peek(0);

                boolean constant = readByte() == 1;

                globals.put(name, new Var(type, value, constant));
                yield VMResult.OK;
            }
            case OpCode.GetGlobal -> {
                String name = readString();
                Var value = globals.get(name);

                if (value == null) {
                    runtimeError("Scope", "Undefined variable");
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
                    yield set(var, value);
                }
                else {
                    runtimeError("Scope", "Undefined variable");
                    yield VMResult.ERROR;
                }
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

    Value get(int index) {
        return stack[index + frame.slots];
    }

    VMResult set(Var var, Value val) {
        if (var.constant) {
            runtimeError("Scope", "Cannot reassign constant");
            return VMResult.ERROR;
        }
        if (!"any".equals(var.type) && !val.type().equals(var.type)) {
            runtimeError("Type",
                    String.format("Got type %s, expected type %s", val.type(), var.type));
            return VMResult.ERROR;
        }
        var.val(val);
        return VMResult.OK;
    }

    VMResult localOps(int op) {
        return switch (op) {
            case OpCode.GetLocal -> {
                int slot = readByte();
                if (stackTop - slot <= 0) {
                    runtimeError("Scope", "Undefined variable");
                    yield VMResult.ERROR;
                }

                Value val = get(slot);
                if (val.isVar)
                    push(val.asVar().val);
                else
                    push(val);
                yield VMResult.OK;
            }
            case OpCode.SetLocal -> {
                int slot = readByte();

                Value var = get(slot);
                Value val = peek(0);
                yield set(var.asVar(), val);
            }
            case OpCode.DefineLocal -> {
                String type = pop().asString();
                Value val = pop();

                boolean constant = readByte() == 1;

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
                moveIP(-offset);
            }

            case OpCode.StartCache -> loopCache.push(new ArrayList<>());
            case OpCode.CollectLoop -> loopCache.peek().add(pop());
            case OpCode.FlushLoop -> push(new Value(new ArrayList<>(loopCache.pop())));
        }

        return VMResult.OK;
    }

    VMResult jumpOps(int op) {
        int offset = switch (op) {
            case OpCode.JumpIfFalse -> readByte() * isFalsey(peek(0));
            case OpCode.JumpIfTrue -> readByte() * (1 - isFalsey(peek(0)));
            case OpCode.Jump -> readByte();
            default -> throw new IllegalStateException("Unexpected value: " + op);
        };
        moveIP(offset);
        return VMResult.OK;
    }

    VMResult forLoop() {
        Value step = pop();
        double end = pop().asNumber();

        int slot = readByte();
        int jump = readByte();

        Var var = get(slot).asVar();
        VMResult res = var.val.add(step);
        if (res != VMResult.OK)
            return res;

        double i = var.val.asNumber();
        if ((i >= end && step.asNumber() >= 1) || (i <= end && step.asNumber() < 1)) {
            moveIP(jump);
        }

        return VMResult.OK;
    }

    VMResult call() {
        int argCount = readByte();
        if (!callValue(peek(argCount), argCount)) {
            return VMResult.ERROR;
        }
        frame = frames[frameCount - 1];
        return VMResult.OK;
    }

    VMResult upvalueOps(int op) {
        switch (op) {
            case OpCode.GetUpvalue -> {
                int slot = readByte();
                push(frame.closure.upvalues.get(slot).val);
            }
            case OpCode.SetUpvalue -> {
                int slot = readByte();
                return set(frame.closure.upvalues.get(slot), peek(0));
            }
        }

        return VMResult.OK;
    }

    boolean callValue(Value callee, int argCount) {
        if (callee.isNativeFunc) {
            return call(callee.asNative(), argCount);
        }
        else if (callee.isClosure) {
            return call(callee.asClosure(), argCount);
        }
        runtimeError("Type", "Can only call functions and classes");
        return false;
    }

    boolean call(JClosure closure, int argCount) {
        if (argCount != closure.function.arity) {
            runtimeError("Argument Count", "Expected " + closure.function.arity + " but got " + argCount);
            return false;
        }

        if (frameCount == FRAMES_MAX) {
            runtimeError("Stack", "Stack overflow");
            return false;
        }

        tracebacks.push(new Traceback(tracebacks.peek().filename, closure.function.name, currentPos().index));

        frames[frameCount++] = new CallFrame(closure, 0, stackTop - argCount - 1);
        return true;
    }

    boolean call(JNative nativeFunc, int argCount) {
        NativeResult result = nativeFunc.call(Arrays.copyOfRange(stack, stackTop - argCount, stackTop));

        if (!result.ok()) {
            runtimeError(result.name(), result.reason());
            return false;
        }

        stackTop -= argCount + 1;
        push(result.value());
        return true;
    }

    Var captureUpvalue(int slot) {
        return stack[slot].asVar();
    }

    public VMResult run() {
        frame = frames[frameCount - 1];

        while (true) {
            Shell.logger.debug("          ");
            for (int i = 0; i < stackTop; i++) {
                Shell.logger.debug("[ ");
                Shell.logger.debug(stack[i]);
                Shell.logger.debug(" ]");
            }
            Shell.logger.debug("\n");

            Disassembler.disassembleInstruction(frame.closure.function.chunk, frame.ip);

            int instruction = readByte();
            VMResult res = switch (instruction) {
                case OpCode.Return -> {
                    Value result = pop();
                    frameCount--;
                    if (frameCount == 0) {
                        yield VMResult.EXIT;
                    }

                    stackTop = frame.slots;
                    frame = frames[frameCount - 1];
                    tracebacks.pop();
                    push(result);
                    yield VMResult.OK;
                }
                case OpCode.Constant -> {
                    push(readConstant());
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

                case OpCode.Call -> call();
                case OpCode.Closure -> {
                    JFunc func = readConstant().asFunc();
                    JClosure closure = new JClosure(func);
                    push(new Value(closure));

                    for (int i = 0; i < closure.upvalueCount; i++) {
                        int isLocal = readByte();
                        int index = readByte();
                        if (isLocal == 1)
                            closure.upvalues.set(i, captureUpvalue(frame.slots + index));
                        else
                            closure.upvalues.set(i, frame.closure.upvalues.get(index));
                        System.out.println(frame.slots + index);
                        System.out.println(closure.upvalues);
                    }

                    yield VMResult.OK;
                }

                case OpCode.GetUpvalue,
                        OpCode.SetUpvalue -> upvalueOps(instruction);

                case OpCode.MakeArray -> {
                    int count = readByte();
                    List<Value> array = new ArrayList<>();
                    for (int i = 0; i < count; i++)
                        array.add(pop());
                    push(new Value(array));
                    yield VMResult.OK;
                }

                case OpCode.MakeMap -> {
                    int count = readByte();
                    Map<Value, Value> map = new HashMap<>();
                    for (int i = 0; i < count; i++) {
                        Value value = pop();
                        Value key = pop();
                        map.put(key, value);
                    }
                    push(new Value(map));
                    yield VMResult.OK;
                }

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
