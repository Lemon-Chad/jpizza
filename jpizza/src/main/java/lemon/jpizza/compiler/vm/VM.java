package lemon.jpizza.compiler.vm;

import lemon.jpizza.Constants;
import lemon.jpizza.Shell;
import lemon.jpizza.compiler.Disassembler;
import lemon.jpizza.compiler.FlatPosition;
import lemon.jpizza.compiler.OpCode;
import lemon.jpizza.compiler.values.*;
import lemon.jpizza.compiler.values.classes.BoundMethod;
import lemon.jpizza.compiler.values.classes.ClassAttr;
import lemon.jpizza.compiler.values.classes.Instance;
import lemon.jpizza.compiler.values.classes.JClass;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.values.functions.NativeResult;

import java.util.*;

public class VM {
    public static final int MAX_STACK_SIZE = 256;
    public static final int FRAMES_MAX = 64;

    private static record Traceback(String filename, String context, int offset) {}

    final Value[] stack;
    public int stackTop;
    int ip;

    Stack<Traceback> tracebacks;
    final Map<String, Var> globals;

    final Stack<List<Value>> loopCache;
    List<Value> currentLoop;

    Map<String, Namespace> libraries;

    public CallFrame frame;
    public final CallFrame[] frames;
    public int frameCount;

    public boolean safe = false;
    public boolean failed = false;

    public VM(JFunc function) {
        Shell.logger.debug("VM created");

        this.ip = 0;

        this.stack = new Value[MAX_STACK_SIZE];
        this.stackTop = 0;
        push(new Value(function));

        this.globals = new HashMap<>();
        this.tracebacks = new Stack<>();

        this.loopCache = new Stack<>();
        this.currentLoop = null;

        this.frames = new CallFrame[FRAMES_MAX];
        this.frameCount = 0;

        this.frame = new CallFrame(new JClosure(function), 0, 0, "void");
        frames[frameCount++] = frame;

        setup();
    }

    void setup() {
        libraries = new HashMap<>();

        defineNative("print", (args) -> {
            Shell.logger.out(args[0]);
            return NativeResult.Ok();
        }, 1);
        defineNative("println", (args) -> {
            Shell.logger.outln(args[0]);
            return NativeResult.Ok();
        }, 1);

        defineNative("random", (args) -> NativeResult.Ok(new Value(Math.random())), 0);

        defineNative("floor", (args) -> NativeResult.Ok(new Value(Math.floor(args[0].asNumber()))), List.of("num"));

        // List Functions
        defineNative("append", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.append(value);
            return NativeResult.Ok();
        }, List.of("list", "any"));
        defineNative("remove", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.remove(value);
            return NativeResult.Ok();
        }, List.of("list", "any"));
        defineNative("pop", (args) -> {
            Value list = args[0];
            Value index = args[1];

            return NativeResult.Ok(list.pop(index.asNumber()));
        }, List.of("list", "num"));
        defineNative("extend", (args) -> {
            Value list = args[0];
            Value other = args[1];

            list.add(other);
            return NativeResult.Ok();
        }, List.of("list", "list"));
        defineNative("insert", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            list.insert(index.asNumber(), value);
            return NativeResult.Ok();
        }, List.of("list", "any", "num"));
        defineNative("setIndex", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            list.set(index.asNumber(), value);
            return NativeResult.Ok();
        }, List.of("list", "any", "num"));
        defineNative("sublist", (args) -> {
            Value list = args[0];
            Value start = args[1];
            Value end = args[2];

            return NativeResult.Ok(new Value(list.asList().subList(start.asNumber().intValue(),
                    end.asNumber().intValue())));
        }, List.of("list", "num", "num"));
        defineNative("join", (args) -> {
            Value str = args[0];
            Value list = args[1];

            List<String> strings = new ArrayList<>();
            for (Value val : list.asList())
                strings.add(val.asString());

            return NativeResult.Ok(new Value(String.join(str.asString(), strings)));
        }, List.of("str", "list"));

        defineNative("choose", args -> {
            List<Value> list = args[0].asList();
            int max = list.size() - 1;
            int index = (int) (Math.random() * max);
            return NativeResult.Ok(list.get(index));
        }, 1);
        defineNative("size", args -> {
            Value list = args[0];
            return NativeResult.Ok(new Value(list.asList().size()));
        }, 1);
        defineNative("contains", args -> {
            Value list = args[0];
            Value val = args[1];
            return NativeResult.Ok(new Value(list.asList().contains(val)));
        }, 2);
        defineNative("indexOf", args -> {
            Value list = args[0];
            Value val = args[1];
            return NativeResult.Ok(new Value(list.asList().indexOf(val)));
        }, 2);

        // Time library
        defineNative("time", "epoch", (args) -> NativeResult.Ok(new Value(System.currentTimeMillis())), 0);
        defineNative("time", "halt", (args) -> {
            try {
                Thread.sleep(args[0].asNumber().intValue());
            } catch (InterruptedException e) {
                runtimeError("Internal", "Interrupted");
            }
            return NativeResult.Ok();
        }, List.of("num"));
    }

    void defineNative(String name, JNative.Method method, int argc) {
        globals.put(name, new Var(
                "function",
                new Value(new JNative(name, method, argc)),
                false
        ));
    }

    void defineNative(String library, String name, JNative.Method method, int argc) {
        if (!libraries.containsKey(library))
            libraries.put(library, new Namespace(library, new HashMap<>()));
        libraries.get(library).addField(name, new Value(
                new JNative(name, method, argc)
        ));
    }

    void defineNative(String name, JNative.Method method, List<String> types) {
        globals.put(name, new Var(
                "function",
                new Value(new JNative(name, method, types.size(), types)),
                false
        ));
    }

    void defineNative(String library, String name, JNative.Method method, List<String> types) {
        if (!libraries.containsKey(library))
            libraries.put(library, new Namespace(library, new HashMap<>()));
        libraries.get(library).addField(name, new Value(
                new JNative(name, method, types.size(), types)
        ));
    }

    public VM trace(String name) {
        tracebacks.push(new Traceback(name, name, 0));
        return this;
    }

    private void popTraceback() {
        tracebacks.pop();
    }

    void moveIP(int offset) {
        frame.ip += offset;
        ip += offset;
    }

    public void push(Value value) {
        stack[stackTop++] = value;
    }

    public Value pop() {
        return stack[--stackTop];
    }

    protected void runtimeError(String message, String reason) {
        runtimeError(message, reason, currentPos());
    }

    protected void runtimeError(String message, String reason, FlatPosition position) {
        int idx = position.index;
        int len = position.len;

        String output = "";

        Stack<Traceback> copy = new Stack<>();
        if (safe) for (Traceback traceback : tracebacks)
            copy.push(traceback);

        if (!tracebacks.empty()) {
            String arrow = Shell.fileEncoding.equals("UTF-8") ? "╰──►" : "--->";

            // Generate traceback
            Traceback last = tracebacks.peek();
            while (!tracebacks.empty()) {
                Traceback top = tracebacks.pop();
                int line = Constants.indexToLine(frame.closure.function.chunk.source(), top.offset);
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

        tracebacks = copy;

        if (safe) {
            Shell.logger.warn(output);
        }
        else {
            Shell.logger.fail(output);
            resetStack();
        }
        failed = true;
    }

    void resetStack() {
        stackTop = 0;
        frameCount = 0;
    }

    String readString() {
        return readConstant().asString();
    }

    Value readConstant() {
        return frame.closure.function.chunk.constants().valuesArray[readByte()];
    }

    int readByte() {
        ip++;
        return frame.closure.function.chunk.codeArray[frame.ip++];
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

    VMResult runBin(String name, Value arg, Instance instance) {
        return runBin(name, new Value[]{arg}, instance);
    }

    VMResult runBin(String name, Value[] args, Instance instance) {
        Value method = instance.binMethods.get(name);

        push(method);
        for (int i = 0; i < args.length; i++) {
            push(args[i]);
        }

        if (!callValue(method, args.length)) return VMResult.ERROR;

        VMResult res = run();
        return res != VMResult.ERROR ? VMResult.OK : VMResult.ERROR;
    }

    boolean canOverride(Value value, String name) {
        return value.isInstance && value.asInstance().binMethods.containsKey(name);
    }

    VMResult binary(int op) {
        Value b = pop();
        Value a = pop();

        switch (op) {
            case OpCode.Add -> {
                if (a.isString)
                    push(new Value(a.asString() + b.asString()));
                else if (canOverride(a, "add"))
                    return runBin("add", b, a.asInstance());
                else
                    push(new Value(a.asNumber() + b.asNumber()));
            }
            case OpCode.Subtract -> {
                if (canOverride(a, "sub"))
                    return runBin("sub", b, a.asInstance());
                push(new Value(a.asNumber() - b.asNumber()));
            }
            case OpCode.Multiply -> {
                if (canOverride(a, "mul"))
                    return runBin("mult", b, a.asInstance());
                push(new Value(a.asNumber() * b.asNumber()));
            }
            case OpCode.Divide -> {
                if (canOverride(a, "div"))
                    return runBin("div", b, a.asInstance());
                push(new Value(a.asNumber() / b.asNumber()));
            }
            case OpCode.Modulo -> {
                if (canOverride(a, "mod"))
                    return runBin("mod", b, a.asInstance());
                push(new Value(a.asNumber() % b.asNumber()));
            }
            case OpCode.Power -> {
                if (canOverride(a, "fastpow"))
                    return runBin("fastpow", b, a.asInstance());
                push(new Value(Math.pow(a.asNumber(), b.asNumber())));
            }
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
                String type = readType();
                Value value = peek(0);

                //noinspection DuplicatedCode
                String valType = value.type();
                if (type.equals("<inferred>"))
                    type = valType;

                boolean constant = readByte() == 1;

                if (!type.equals("any") && !valType.equals(type)) {
                    runtimeError("Type", "Type mismatch");
                    yield VMResult.ERROR;
                }

                globals.put(name, new Var(type, value, constant));
                yield VMResult.OK;
            }
            case OpCode.GetGlobal -> {
                String name = readString();

                VMResult res = getBound(name, true);
                if (res == VMResult.OK)
                    yield VMResult.OK;

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

                VMResult res = setBound(name, value, true);
                if (res == VMResult.OK)
                    yield VMResult.OK;

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

    VMResult getBound(String name, boolean suppress) {
        if (frame.bound != null) {
            if (frame.bound.isInstance) {
                Instance instance = frame.bound.asInstance();
                Value field = instance.getField(name, true);
                if (field != null) {
                    push(field);
                    return VMResult.OK;
                }
                else {
                    if (!suppress)
                        runtimeError("Scope", "Undefined attribute");
                    return VMResult.ERROR;
                }
            }
            else if (frame.bound.isClass) {
                JClass clazz = frame.bound.asClass();
                Value field = clazz.getField(name, true);
                if (field != null) {
                    push(field);
                    return VMResult.OK;
                }
                else {
                    if (!suppress)
                        runtimeError("Scope", "Undefined attribute");
                    return VMResult.ERROR;
                }
            }
            else if (frame.bound.isNamespace) {
                Namespace ns = frame.bound.asNamespace();
                Value field = ns.getField(name);
                if (field != null) {
                    push(field);
                    return VMResult.OK;
                }
                else {
                    if (!suppress)
                        runtimeError("Scope", "Undefined attribute");
                    return VMResult.ERROR;
                }
            }
            if (!suppress)
                runtimeError("Scope", "Not in class or instance");
            return VMResult.ERROR;
        }
        if (!suppress)
            runtimeError("Scope", "Not in class or instance");
        return VMResult.ERROR;
    }

    VMResult setBound(String name, Value value, boolean suppress) {
        if (frame.bound != null) {
            if (frame.bound.isInstance) {
                Instance instance = frame.bound.asInstance();
                return boundNeutral(suppress, instance.setField(name, value, true));
            } else if (frame.bound.isClass) {
                JClass clazz = frame.bound.asClass();
                return boundNeutral(suppress, clazz.setField(name, value, true));
            }
        }
        if (!suppress)
            runtimeError("Scope", "Not in class or instance");
        return VMResult.ERROR;
    }

    VMResult boundNeutral(boolean suppress, NativeResult nativeResult) {
        if (nativeResult.ok())
            return VMResult.OK;
        else {
            if (!suppress)
                runtimeError(nativeResult.name(), nativeResult.reason());
            return VMResult.ERROR;
        }
    }

    VMResult attrOps(int op) {
        return switch (op) {
            case OpCode.GetAttr -> getBound(readString(), false);
            case OpCode.SetAttr -> setBound(readString(), pop(), false);
            default -> VMResult.OK;
        };
    }

    VMResult comparison(int op) {
        Value b = pop();
        Value a = pop();

        switch (op) {
            case OpCode.Equal -> {
                if (canOverride(a, "eq")) {
                    return runBin("eq", b, a.asInstance());
                }
                else if (canOverride(b, "eq")) {
                    return runBin("eq", a, b.asInstance());
                }
                push(new Value(a.equals(b)));
            }
            case OpCode.GreaterThan -> {
                if (canOverride(b, "lte")) {
                    return runBin("lte", a, b.asInstance());
                }
                push(new Value(a.asNumber() > b.asNumber()));
            }
            case OpCode.LessThan -> {
                if (canOverride(a, "lt")) {
                    return runBin("lt", b, a.asInstance());
                }
                push(new Value(a.asNumber() < b.asNumber()));
            }
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
        String type = val.type();
        if (!"any".equals(var.type) && !type.equals(var.type)) {
            runtimeError("Type",
                    String.format("Got type %s, expected type %s", type, var.type));
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
                push(val.asVar().val);
                yield VMResult.OK;
            }
            case OpCode.SetLocal -> {
                int slot = readByte();

                Value var = get(slot);
                Value val = peek(0);
                yield set(var.asVar(), val);
            }
            case OpCode.DefineLocal -> {
                String type = readType();
                Value val = pop();

                //noinspection DuplicatedCode
                String valType = val.type();
                if (type.equals("<inferred>"))
                    type = valType;

                boolean constant = readByte() == 1;

                if (!type.equals("any") && !valType.equals(type)) {
                    runtimeError("Type", "Type mismatch");
                    yield VMResult.ERROR;
                }

                Var var = new Var(type, val, constant);
                push(new Value(var));

                yield VMResult.OK;
            }

            default -> VMResult.OK;
        };
    }

    String readType() {
        return readType(readConstant().asType());
    }

    String readType(List<String> rawtype) {
        StringBuilder sb = new StringBuilder();

        for (String raw : rawtype) {
            // @SLOT = Generic type slot
            if (raw.startsWith("@")) {
                int slot = Integer.parseInt(raw.substring(1));
                sb.append(get(slot).asString());
            }
            else {
                sb.append(raw);
            }
        }

        // ['MyCool', '(', '@1', ')', 'Type'] -> MyCool(GenericAtSlot1)Type

        return sb.toString();
    }

    VMResult loopOps(int op) {
        switch (op) {
            case OpCode.Loop -> {
                int offset = readByte();
                moveIP(-offset);
            }

            case OpCode.StartCache -> {
                currentLoop = new ArrayList<>();
                loopCache.push(currentLoop);
            }
            case OpCode.CollectLoop -> currentLoop.add(pop());
            case OpCode.FlushLoop -> {
                push(new Value(loopCache.pop()));
                if (loopCache.isEmpty())
                    currentLoop = null;
                else
                    currentLoop = loopCache.peek();
            }
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
        moveIP(jump * (((i >= end && step.asNumber() >= 1) || (i <= end && step.asNumber() < 1)) ? 1 : 0));

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

    VMResult access() {
        String name = readString();
        Value val = pop();

        if (canOverride(val,  "access")) {
            return runBin("access", new Value(name), val.asInstance());
        }

        if (val.isInstance) {
            return access(val, val.asInstance(), name);
        }
        else if (val.isClass) {
            return access(val, val.asClass(), name);
        }
        else if (val.isNamespace) {
            return access(val, val.asNamespace(), name);
        }
        else {
            runtimeError("Type", "Type " + val.type() + " does not have members");
            return VMResult.ERROR;
        }
    }

    VMResult access(Value val, Namespace namespace, String name) {
        return access(val, name, namespace.getField(name));
    }

    VMResult access(Value val, Instance instance, String name) {
        return access(val, name, instance.getField(name, false));
    }

    VMResult access(Value val, JClass clazz, String name) {
        return access(val, name, clazz.getField(name, false));
    }

    VMResult collections(int op) {
        return switch (op) {
            case OpCode.Get, OpCode.Index -> {
                Value index = pop();
                Value collection = pop();

                if (collection.isList || collection.isString) {
                    push(collection.asList().get(index.asNumber().intValue()));
                }
                else if (canOverride(collection, op == OpCode.Get ? "get" : "bracket")) {
                    yield runBin(op == OpCode.Get ? "get" : "bracket", index, collection.asInstance());
                }
                else if (collection.isMap) {
                    push(collection.asMap().get(index));
                }

                yield VMResult.OK;
            }

            default -> VMResult.OK;
        };
    }

    private VMResult access(Value val, String name, Value member) {
        if (member == null) {
            runtimeError("Scope", "No member named " + name);
            return VMResult.ERROR;
        }
        if (member.isClosure) {
            member = new Value(new BoundMethod(member.asClosure(), val));
        }
        push(member);
        return VMResult.OK;
    }

    boolean callValue(Value callee, int argCount) {
        if (callee.isNativeFunc) {
            return call(callee.asNative(), argCount);
        }
        else if (callee.isClosure) {
            return call(callee.asClosure(), argCount);
        }
        else if (callee.isClass) {
            return call(callee.asClass(), argCount);
        }
        else if (callee.isBoundMethod) {
            BoundMethod bound = callee.asBoundMethod();
            stack[stackTop - argCount - 1] = new Value(new Var("any", bound.receiver, true));
            return call(bound.closure, argCount, bound.receiver);
        }
        runtimeError("Type", "Can only call functions and classes");
        return false;
    }

    boolean call(JClass clazz, int argCount) {
        Instance instance = new Instance(clazz, this);

        Value value = new Value(instance);
        instance.self = value;

        BoundMethod bound = new BoundMethod(clazz.constructor.asClosure(), value);
        return callValue(new Value(bound), argCount);
    }

    boolean call(JClosure closure, int argCount) {
        return call(closure, argCount, frame.bound);
    }

    public boolean call(JClosure closure, int argCount, Value binding) {
        if (argCount != closure.function.arity) {
            runtimeError("Argument Count", "Expected " + closure.function.arity + " but got " + argCount);
            return false;
        }

        if (frameCount == FRAMES_MAX) {
            runtimeError("Stack", "Stack overflow");
            return false;
        }

        Traceback traceback = new Traceback(tracebacks.peek().filename, closure.function.name, 0);
        if (closure.function.async) {
            VM thread = new VM(closure.function.copy());
            thread.tracebacks.push(traceback);
            Thread t = new Thread(thread::run);
            t.start();
        }
        else {
            tracebacks.push(traceback);

            frames[frameCount++] = new CallFrame(closure, 0, stackTop - argCount - 1,
                    readType(closure.function.returnType), binding);
        }
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

    void defineMethod(String name) {
        Value val = peek(0);
        JClass clazz = peek(1).asClass();

        boolean isStatic = readByte() == 1;
        boolean isPrivate = readByte() == 1;
        boolean isBin = readByte() == 1;

        JClosure method = val.asClosure();
        method.asMethod(isStatic, isPrivate, isBin, clazz.name);

        clazz.addMethod(name, val);
        pop();
    }

    public VMResult run() {
        frame = frames[frameCount - 1];
        int exitLevel = frameCount - 1;

        while (true) {
            if (Shell.logger.debug) {
                Shell.logger.debug("          ");
                for (int i = 0; i < stackTop; i++) {
                    Shell.logger.debug("[ ");
                    Shell.logger.debug(stack[i].toSafeString());
                    Shell.logger.debug(" ]");
                }
                Shell.logger.debug("\n");

                Disassembler.disassembleInstruction(frame.closure.function.chunk, frame.ip);
            }

            int instruction = readByte();
            VMResult res = switch (instruction) {
                case OpCode.Return -> {
                    Value result = pop();
                    frameCount--;
                    if (frameCount == 0) {
                        yield VMResult.EXIT;
                    }

                    String type = result.type();
                    if (!frame.returnType.equals("any") && !type.equals(frame.returnType)) {
                        runtimeError("Type", "Expected " + frame.returnType + " but got " + type);
                        yield VMResult.ERROR;
                    }

                    boolean isConstructor = frame.closure.function.name.equals("<make>");
                    Value bound = frame.bound;

                    stackTop = frame.slots;
                    frame = frames[frameCount - 1];
                    popTraceback();

                    if (isConstructor) {
                        push(bound);
                    }
                    else {
                        push(result);
                    }

                    if (exitLevel == frameCount) {
                        yield VMResult.EXIT;
                    }

                    yield VMResult.OK;
                }
                case OpCode.Constant -> {
                    push(readConstant());
                    yield VMResult.OK;
                }

                case OpCode.Assert -> {
                    Value value = pop();
                    if (isFalsey(value) == 1) {
                        runtimeError("Assertion", "Assertion failed");
                        yield VMResult.ERROR;
                    }
                    yield VMResult.OK;
                }

                case OpCode.Throw -> {
                    Value type = pop();
                    Value reason = pop();
                    runtimeError(type.asString(), reason.asString());
                    yield VMResult.ERROR;
                }

                case OpCode.Import -> {
                    String name = readString();

                    if (!peek(0).isFunc) {
                        if (!libraries.containsKey(name)) {
                            runtimeError("Import", "Library '" + name + "' not found");
                            yield VMResult.ERROR;
                        }
                        globals.put(name, new Var(
                                "namespace",
                                new Value(libraries.get(name)),
                                true
                        ));
                        yield VMResult.OK;
                    }

                    JFunc func = pop().asFunc();

                    VM runner = new VM(func);
                    runner.trace(name);
                    VMResult importres = runner.run();
                    if (importres == VMResult.ERROR) {
                        yield VMResult.ERROR;
                    }

                    globals.put(name, new Var(
                            "namespace",
                            new Value(runner.asNamespace(name)),
                            true
                    ));
                    push(new Value());
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

                case OpCode.Null -> {
                    push(new Value());
                    yield VMResult.OK;
                }

                case OpCode.Get,
                        OpCode.Index -> collections(instruction);

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
                    popTraceback();
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
                    }

                    yield VMResult.OK;
                }

                case OpCode.GetAttr,
                        OpCode.SetAttr -> attrOps(instruction);

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

                case OpCode.Class -> {
                    String name = readString();
                    boolean hasSuper = readByte() == 1;
                    JClass superClass = hasSuper ? pop().asClass() : null;

                    int attributeCount = readByte();
                    Map<String, ClassAttr> attributes = new HashMap<>();
                    for (int i = 0; i < attributeCount; i++) {
                        String attrname = readString();
                        boolean isprivate = readByte() == 1;
                        boolean isstatic = readByte() == 1;
                        String type = readType();
                        attributes.put(attrname, new ClassAttr(pop(), type, isstatic, isprivate));
                    }


                    push(new Value(new JClass(name, attributes, superClass)));
                    yield VMResult.OK;
                }

                case OpCode.Method -> {
                    defineMethod(readString());
                    yield VMResult.OK;
                }

                case OpCode.MakeVar -> {
                    int slot = readByte();
                    String type = readType();
                    boolean constant = readByte() == 1;

                    stack[frame.slots + slot] = new Value(new Var(type, get(slot), constant));
                    yield VMResult.OK;
                }

                case OpCode.Access -> access();

                default -> throw new RuntimeException("Unknown opcode: " + instruction);
            };
            if (res == VMResult.EXIT) {
                Shell.logger.debug("Exiting");
                return VMResult.OK;
            }
            else if (res == VMResult.ERROR) {
                if (safe) {
                    while (frameCount != exitLevel) {
                        tracebacks.pop();
                        frameCount--;
                    }
                    frame = frames[frameCount - 1];
                    stackTop = frames[frameCount].slots;
                }
                return VMResult.ERROR;
            }
        }
    }

    public Namespace asNamespace(String name) {
        return new Namespace(name, globals);
    }

}
