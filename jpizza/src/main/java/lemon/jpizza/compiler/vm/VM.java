package lemon.jpizza.compiler.vm;

import lemon.jpizza.Constants;
import lemon.jpizza.Pair;
import lemon.jpizza.Shell;
import lemon.jpizza.compiler.*;
import lemon.jpizza.compiler.headers.HeadCode;
import lemon.jpizza.compiler.headers.Memo;
import lemon.jpizza.compiler.values.Pattern;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.Var;
import lemon.jpizza.compiler.values.classes.*;
import lemon.jpizza.compiler.values.enums.JEnum;
import lemon.jpizza.compiler.values.enums.JEnumChild;
import lemon.jpizza.compiler.values.functions.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class VM {
    public static final int MAX_STACK_SIZE = 256;
    public static final int FRAMES_MAX = 256;
    public static final String VERSION = "2.0.1";

    private static record Traceback(String filename, String context, int offset, Chunk chunk) {}

    final JStack<Value> stack;
    int ip;

    List<String> exports = null;

    Stack<Traceback> tracebacks;
    final Map<String, Var> globals;

    final Stack<List<Value>> loopCache;
    List<Value> currentLoop;

    Pair<String, String> lastError;

    Map<String, Namespace> libraries;

    public CallFrame frame;
    public final JStack<CallFrame> frames;

    static Memo memo = new Memo();

    String mainFunction;
    String mainClass;

    public boolean safe = false;
    public boolean failed = false;

    JStack<Pair<Integer, Integer>> nehStack = new JStack<>(FRAMES_MAX);

    public VM(JFunc function) {
        this(function, new HashMap<>());
    }

    public VM(JFunc function, Map<String, Var> globals) {
        Shell.logger.debug("VM create\n");

        this.ip = 0;

        this.stack = new JStack<>(MAX_STACK_SIZE);
        push(new Value(function));

        this.globals = globals;
        this.tracebacks = new Stack<>();

        this.loopCache = new Stack<>();
        this.currentLoop = null;

        this.frames = new JStack<>(FRAMES_MAX);

        this.frame = new CallFrame(new JClosure(function), 0, 0, "void");
        frames.push(frame);

        setup();
    }

    void setup() {
        libraries = new HashMap<>();
        LibraryManager.Setup(this);
    }

    void defineNative(String name, JNative.Method method, int argc) {
        globals.put(name, new Var(
                "function",
                new Value(new JNative(name, method, argc)),
                true
        ));
    }

    public void defineNative(String library, String name, JNative.Method method, int argc) {
        if (!libraries.containsKey(library))
            libraries.put(library, new Namespace(library, new HashMap<>()));
        libraries.get(library).addField(name, new Value(
                new JNative(name, method, argc)
        ));
    }

    public void defineVar(String lib, String name, Value val) {
        if (!libraries.containsKey(lib))
            libraries.put(lib, new Namespace(lib, new HashMap<>()));
        libraries.get(lib).addField(name, val);
    }

    void defineNative(String name, JNative.Method method, List<String> types) {
        globals.put(name, new Var(
                "function",
                new Value(new JNative(name, method, types.size(), types)),
                true
        ));
    }

    public void defineNative(String library, String name, JNative.Method method, List<String> types) {
        if (!libraries.containsKey(library))
            libraries.put(library, new Namespace(library, new HashMap<>()));
        libraries.get(library).addField(name, new Value(
                new JNative(name, method, types.size(), types)
        ));
    }

    public VM trace(String name) {
        tracebacks.push(new Traceback(name, name, 0, frame.closure.function.chunk));
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
        stack.push(value);
    }

    public Value pop() {
        return stack.pop();
    }

    protected void runtimeError(String message, String reason) {
        runtimeError(message, reason, currentPos());
    }

    Stack<Traceback> copyTracebacks() {
        Stack<Traceback> copy = new Stack<>();
        for (Traceback traceback : tracebacks)
            copy.push(traceback);
        return copy;
    }

    protected void runtimeError(String message, String reason, FlatPosition position) {
        if (nehStack.count > 0)
            return;

        lastError = new Pair<>(message, reason);

        int idx = position.index;
        int len = position.len;

        String output = "";

        Stack<Traceback> copy = copyTracebacks();

        if (!tracebacks.empty()) {
            String arrow = Shell.fileEncoding.equals("UTF-8") ? "╰──►" : "--->";

            // Generate traceback
            Traceback last = tracebacks.peek();
            while (last == null) {
                tracebacks.pop();
                last = tracebacks.peek();
            }
            while (!tracebacks.empty()) {
                Traceback top = tracebacks.pop();
                if (top == null) continue;
                int line = Constants.indexToLine(top.chunk.source(), top.chunk.getPosition(top.offset).index);
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
            while (frames.count > 0) {
                CallFrame frame = frames.pop();
                Traceback traceback = copy.pop();
                if (frame.catchError) {
                    frames.push(frame);
                    copy.push(traceback);
                    tracebacks = copy;
                    this.frame = frame;
                    return;
                }
            }
            Shell.logger.fail(output);
            resetStack();
        }
        failed = true;
    }

    void resetStack() {
        stack.clear();
        frames.clear();
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
        return stack.peek(offset);
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

        BoundMethod bound = new BoundMethod(method.asClosure(), instance.self);
        if (!callValue(new Value(bound), args, new HashMap<>(), new String[0])) return VMResult.ERROR;

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
                else if (a.isList) {
                    List<Value> list = new ArrayList<>(a.asList());
                    list.addAll(b.asList());
                    push(new Value(list));
                }
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
                    return runBin("mul", b, a.asInstance());
                if (a.isString) {
                    push(new Value(a.asString().repeat(b.asNumber().intValue())));
                }
                else if (a.isList) {
                    List<Value> repeated = new ArrayList<>();
                    List<Value> list = a.asList();
                    for (int i = 0; i < b.asNumber().intValue(); i++)
                        repeated.addAll(list);
                    push(new Value(repeated));
                }
                else {
                    push(new Value(a.asNumber() * b.asNumber()));
                }
            }
            case OpCode.Divide -> {
                if (canOverride(a, "div"))
                    return runBin("div", b, a.asInstance());
                else if (a.isList) {
                    List<Value> list = new ArrayList<>(a.asList());
                    list.remove(b);
                    push(new Value(list));
                }
                else
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
                    runtimeError("Type", "Expected " + type + " but got " + valType);
                    yield VMResult.ERROR;
                }

                boolean usesRange = readByte() == 1;
                int min = Integer.MIN_VALUE;
                int max = Integer.MAX_VALUE;
                if (usesRange) {
                    min = readByte();
                    max = readByte();
                }

                globals.put(name, new Var(type, value, constant, min, max));
                yield VMResult.OK;
            }
            case OpCode.GetGlobal -> {
                String name = readString();
                Var value = globals.get(name);

                if (value == null) {
                    VMResult res = getBound(name, true);
                    if (res == VMResult.OK)
                        yield VMResult.OK;
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
                Value field = ns.getField(name, true);
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
            }
            else if (frame.bound.isClass) {
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
                if (b.isPattern) {
                    return matchPattern(a, b.asPattern());
                }

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

    private VMResult matchPattern(Value a, Pattern asPattern) {
        if (!a.isInstance) {
            push(new Value(false));
            return VMResult.OK;
        }

        Instance instance = a.asInstance();
        if (!instance.instanceOf(asPattern.value)) {
            push(new Value(false));
            return VMResult.OK;
        }

        for (Map.Entry<String, Value> entry : asPattern.cases.entrySet()) {
            Value val = instance.getField(entry.getKey(), false);
            if (val == null) {
                push(new Value(false));
                return VMResult.OK;
            }
            else if (!val.equals(entry.getValue())) {
                push(new Value(false));
                return VMResult.OK;
            }
        }

        for (String binding : asPattern.keys) {
            Value val = instance.getField(binding, false);
            if (val == null) {
                runtimeError("Scope", "Undefined attribute");
                return VMResult.ERROR;
            }
            else {
                push(new Value(new Var(
                        "any",
                        val,
                        false
                )));
            }
        }
        push(new Value(true));
        return VMResult.OK;
    }

    Value get(int index) {
        return stack.get(index + frame.slots);
    }

    VMResult set(Var var, Value val) {
        if (var.constant) {
            runtimeError("Scope", "Cannot reassign constant");
            return VMResult.ERROR;
        }
        if (var.min != Integer.MIN_VALUE || var.max != Integer.MAX_VALUE) {
            if (val.isNumber) {
                double d = val.asNumber();
                if (d < var.min || d > var.max) {
                    runtimeError("Range", "Value out of range");
                    return VMResult.ERROR;
                }
            }
            else {
                runtimeError("Range", "Value out of range");
                return VMResult.ERROR;
            }
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
                if (stack.count - slot <= 0) {
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

                boolean usesRange = readByte() == 1;
                int min = Integer.MIN_VALUE;
                int max = Integer.MAX_VALUE;
                if (usesRange) {
                    min = readByte();
                    max = readByte();
                }

                if (!type.equals("any") && !valType.equals(type)) {
                    runtimeError("Type", "Expected type " + type + ", got " + valType);
                    yield VMResult.ERROR;
                }

                Var var = new Var(type, val, constant, min, max);
                push(new Value(var));
                push(val);

                yield VMResult.OK;
            }

            default -> VMResult.OK;
        };
    }

    String readType() {
        return readType(readConstant().asType());
    }

    String readType(List<String> rawType) {
        GenericGetter getter = null;
        if (frame.bound != null && frame.bound.isInstance) {
            Instance instance = frame.bound.asInstance();
            getter = instance::getGeneric;
        }
        return readType(rawType, getter);
    }

    interface GenericGetter {
        String get(String key);
    }

    String readType(List<String> rawtype, GenericGetter getter) {
        StringBuilder sb = new StringBuilder();

        for (String raw : rawtype) {
            // @SLOT = Generic type slot
            if (raw.startsWith("@")) {
                int slot = Integer.parseInt(raw.substring(1));
                sb.append(get(slot).asVar().val.asString());
                continue;
            }
            else if (getter != null) {
                String gen = getter.get(raw);
                if (gen != null) {
                    sb.append(gen);
                    continue;
                }
            }
            sb.append(raw);
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
        double step = pop().asNumber();
        double end = pop().asNumber();

        int slot = readByte();
        int jump = readByte();

        get(slot).asVar().val(
                new Value(get(slot).asVar().val.asNumber() + step)
        );

        double i = get(slot).asVar().val.asNumber();
        moveIP(jump * (((i >= end && step >= 0) || (i <= end && step < 0)) ? 1 : 0));

        return VMResult.OK;
    }

    VMResult upvalueOps(int op) {
        switch (op) {
            case OpCode.GetUpvalue -> {
                int slot = readByte();
                if (frame.closure.upvalues[slot] == null) {
                    runtimeError("Scope", "Undefined variable");
                    return VMResult.ERROR;
                }
                push(frame.closure.upvalues[slot].val);
            }
            case OpCode.SetUpvalue -> {
                int slot = readByte();
                return set(frame.closure.upvalues[slot], peek(0));
            }
        }

        return VMResult.OK;
    }

    VMResult byteOps(int op) {
        return switch (op) {
            case OpCode.ToBytes -> {
                push(new Value(pop().asBytes()));
                yield VMResult.OK;
            }
            case OpCode.FromBytes -> {
                if (!peek(0).isBytes) {
                    runtimeError("Type", "Expected bytes");
                    yield VMResult.ERROR;
                }
                NativeResult res = Value.fromByte(pop().asBytes());
                if (res.ok()) {
                    push(res.value());
                    yield VMResult.OK;
                }
                else {
                    runtimeError(res.name(), res.reason());
                    yield VMResult.ERROR;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + op);
        };
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
        else if (val.isEnumParent) {
            return access(val, val.asEnum(), name);
        }
        else {
            runtimeError("Type", "Type " + val.type() + " does not have members");
            return VMResult.ERROR;
        }
    }

    VMResult access(Value val, JEnum jEnum, String name) {
        if (jEnum.has(name)) {
            push(jEnum.get(name));
            return VMResult.OK;
        }
        else {
            runtimeError("Enum", "Enum " + val.type() + " does not have member " + name);
            return VMResult.ERROR;
        }
    }

    VMResult access(Value val, Namespace namespace, String name) {
        return access(val, name, namespace.getField(name, false));
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
                    List<Value> list = collection.asList();
                    int idx = index.asNumber().intValue();
                    if (idx >= list.size()) {
                        runtimeError("Index", "Index out of bounds");
                        yield VMResult.ERROR;
                    }
                    else if (idx < 0) {
                        idx += list.size();
                        if (idx < 0) {
                            runtimeError("Index", "Index out of bounds");
                            yield VMResult.ERROR;
                        }
                    }
                    push(list.get(idx));
                }
                else if (canOverride(collection, op == OpCode.Get ? "get" : "bracket")) {
                    yield runBin(op == OpCode.Get ? "get" : "bracket", index, collection.asInstance());
                }
                else if (collection.isMap) {
                    push(collection.asMap().getOrDefault(index, new Value()));
                }
                else {
                    runtimeError("Type", "Type " + collection.type() + " does not have members");
                    yield VMResult.ERROR;
                }

                yield VMResult.OK;
            }

            default -> VMResult.OK;
        };
    }

    public interface BitCall {
        long call(long left, long right);
    }

    public static double bitOp(double left, double right, BitCall call) {
        long power = 0;
        while (left % 1 != 0 || right % 1 != 0) {
            left *= 10;
            right *= 10;
            power++;
        }
        return call.call((long) left, (long) right) / Math.pow(10, power);
    }

    VMResult bitOps(int instruction) {
        switch (instruction) {
            case OpCode.BitAnd -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left & right)
                ));
            }
            case OpCode.BitOr -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left | right)
                ));
            }
            case OpCode.BitXor -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left ^ right)
                ));
            }
            case OpCode.LeftShift -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left << right)
                ));
            }
            case OpCode.RightShift -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left >>> right)
                ));
            }
            case OpCode.SignRightShift -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left >> right)
                ));
            }
            case OpCode.BitCompl -> {
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), 0, (left, right) -> ~left)
                ));
            }
        }
        return VMResult.OK;
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

    VMResult call() {
        int argc = readByte();
        int kwargc = readByte();
        int genc = readByte();

        Value callee = pop();

        Map<String, Value> kwargs = new HashMap<>();
        for (int i = 0; i < kwargc; i++)
            kwargs.put(readString(), pop());

        Value[] args = new Value[argc];
        List<Value> argList = new ArrayList<>();
        for (int i = argc - 1; i >= 0; i--)
            args[i] = pop();

        push(callee);

        String[] generics = new String[genc];
        for (int i = 0; i < genc; i++) {
            generics[i] = readType();
            push(new Value(new Var("String", new Value(generics[i]), true)));
        }

        for (Value arg : args) {
            if (arg.isSpread) {
                Spread spread = arg.asSpread();
                for (Value val : spread.values) {
                    push(val);
                    argList.add(val);
                }
            }
            else {
                push(arg);
                argList.add(arg);
            }
        }

        // Stack:
        // [CALLEE] [GENERICS] [ARGUMENTS] [KWARGS]

        if (!callValue(callee, argList.toArray(new Value[0]), kwargs, generics)) {
            return VMResult.ERROR;
        }
        frame = frames.peek();
        return VMResult.OK;
    }

    public boolean callValue(Value callee, Value[] args, Map<String, Value> kwargs, String[] generics) {
        if (callee.isNativeFunc) {
            return call(callee.asNative(), args);
        }
        else if (callee.isClosure) {
            return call(callee.asClosure(), args, kwargs, generics);
        }
        else if (callee.isClass) {
            return call(callee.asClass(), args, kwargs, generics);
        }
        else if (callee.isBoundMethod) {
            BoundMethod bound = callee.asBoundMethod();
            stack.set(stack.count - args.length - generics.length - 1, new Value(new Var("any", bound.receiver, true)));
            return call(bound.closure, bound.receiver, args, kwargs, generics);
        }
        else if (callee.isEnumChild) {
            return call(callee.asEnumChild(), generics, args.length);
        }
        runtimeError("Type", "Can only call functions and classes");
        return false;
    }

    boolean call(JEnumChild child, String[] generics, int argCount) {
        int argc = child.arity;

        if (argCount != argc) {
            runtimeError("Argument Count", "Expected " + argc + " but got " + argCount);
            return false;
        }

        Value[] args = new Value[argCount];
        for (int i = argCount - 1; i >= 0; i--)
            args[i] = pop();

        if (generics.length != child.genericArity) {
            if ((generics = inferGenerics(
                generics,
                child.genericArity,
                argCount,
                child.genericSlots,
                args
            )) == null) return false;
        }
    

        List<String>[] gTypes = new ArrayList[argc];
        for (int i = 0; i < argc; i++) {
            List<String> pretype = child.propTypes.get(i);
            List<String> newType = new ArrayList<>();
            for (String t : pretype) {
                int idx = child.generics.indexOf(t);
                if (idx != -1) {
                    newType.add(generics[idx]);
                }
                else {
                    newType.add(t);
                }
            }
            gTypes[i] = newType;
        }

        String[] types = new String[argc];
        for (int i = 0; i < argc; i++)
            types[i] = readType(gTypes[i]);
        
        for (int i = 0; i < argc; i++) {
            String type = args[i].type();
            if (!types[i].equals("any") && !type.equals(types[i])) {
                runtimeError("Type", "Expected " + types[i] + " but got " + type);
                return false;
            }
        }

        pop();
        push(child.create(args, types, generics, this));
        return true;
    }

    boolean call(JClass clazz, Value[] args, Map<String, Value> kwargs, String[] generics) {
        Instance instance = new Instance(clazz, this);

        Value value = new Value(instance);
        instance.self = value;

        JClosure closure = clazz.constructor.asClosure();
        if ((generics = pushInference(closure, args, generics)) == null) return false;

        for (int i = 0; i < closure.function.genericArity; i++) {
            instance.putGeneric(clazz.generics.get(i), generics[i]);
        }

        for (Map.Entry<String, ClassAttr> var : instance.fields.entrySet())
            var.getValue().type = readType(var.getValue().rawType, instance::getGeneric);

        BoundMethod bound = new BoundMethod(closure, value);
        return callValue(new Value(bound), args, kwargs, generics);
    }

    boolean call(JClosure closure, Value[] args, Map<String, Value> kwargs, String[] generics) {
        return call(closure, frame.bound, args, kwargs, generics);
    }

    public String[] inferGenerics(String[] generics, int genericArity, int arity, List<Integer> genericSlots, Value[] args) {
        if (generics.length == 0) {
            // Try to infer generics
            String[] inferred = new String[genericArity];
            int totalInferred = 0;
            for (int i = 0; i < arity; i++) {
                int slot = genericSlots.get(i);
                if (slot != -1 && inferred[slot] == null) {
                    inferred[slot] = args[i].type();
                    totalInferred++;
                }
            }
            if (totalInferred == genericArity) {
                return inferred;
            }
        }
        runtimeError("Generic Count", "Expected " + genericArity + " but got " + generics.length);
        return null;
    }

    public boolean call(JClosure closure, Value binding, Value[] args, Map<String, Value> kwargs, String[] generics) {
        if (frame.memoize) {
            Value val = memo.get(closure.function.name, args);
            if (val != null) {
                for (int i = 0; i <= args.length; i++)
                    pop();
                push(val);
                return true;
            }
            memo.stackCache(closure.function.name, args);
        }

        if (pushInference(closure, args, generics) == null) return false;

        List<Value> extraArgs = new ArrayList<>();
        if (args.length < closure.function.arity) {
            if (args.length + closure.function.defaultCount < closure.function.arity) {
                runtimeError("Argument Count", "Expected " + closure.function.arity + " but got " + args.length);
                return false;
            }
            for (int i = args.length; i < closure.function.arity; i++)
                push(closure.function.defaults.get(i));
        }
        else if (args.length > closure.function.arity) {
            if (closure.function.args != null) {
                List<Value> argsList = new ArrayList<>();
                for (int i = closure.function.arity; i < args.length; i++)
                    argsList.add(pop());
                for (int i = argsList.size() - 1; i >= 0; i--)
                    extraArgs.add(argsList.get(i));
                push(new Value(extraArgs));
            }
            else {
                runtimeError("Argument Count", "Expected " + closure.function.arity + " but got " + args.length);
                return false;
            }
        }

        Map<Value, Value> keywordArgs = new HashMap<>();
        if (closure.function.kwargs != null) {
            for (Map.Entry<String, Value> entry : kwargs.entrySet()) {
                String name = entry.getKey();
                keywordArgs.put(new Value(name), entry.getValue());
            }
            push(new Value(keywordArgs));
        }

        Traceback traceback = new Traceback(tracebacks.peek().filename, closure.function.name, frame.ip - 1, frame.closure.function.chunk);
        if (closure.function.async) {
            VM thread = new VM(closure.function.copy());
            thread.tracebacks.push(traceback);
            Thread t = new Thread(thread::run);
            t.start();
        }
        else {
            tracebacks.push(traceback);

            addFrame(closure, stack.count - closure.function.totarity - 1, binding);
        }
        return true;
    }

    String[] pushInference(JClosure closure, Value[] args, String[] generics) {
        if (generics.length != closure.function.genericArity) {
            if ((generics = inferGenerics(
                generics,
                closure.function.genericArity,
                closure.function.arity,
                closure.function.genericSlots,
                args
            )) == null) return null;

            // Move arguments out of the way
            for (int i = 0; i < args.length; i++)
                pop();

            // Insert inferred arguments
            for (String type : generics) {
                push(new Value(new Var("String", new Value(type), true)));
            }

            // Re-add arguments
            for (Value arg : args)
                push(arg);
        }
        return generics;
    }

    void addFrame(JClosure closure, int slots, Value binding) {
        CallFrame newFrame = new CallFrame(closure, 0, slots, null, binding);

        // Inherited flags
        newFrame.memoize = frame.memoize;

        // General flags
        newFrame.catchError = closure.function.catcher;

        frames.push(newFrame);

        frame = newFrame;
        frame.returnType = readType(closure.function.returnType);
    }

    void addFrame(JClosure closure, int slots, Value binding, String returnType, int ip) {
        CallFrame newFrame = new CallFrame(closure, ip, slots, returnType, binding);

        // Inherited flags
        newFrame.memoize = frame.memoize;

        // General flags
        newFrame.catchError = closure.function.catcher;

        frames.push(newFrame);

        frame = newFrame;
    }

    boolean call(JNative nativeFunc, Value[] args) {
        NativeResult result = nativeFunc.call(args);

        if (!result.ok()) {
            runtimeError(result.name(), result.reason());
            return false;
        }

        stack.setTop(stack.count - args.length - 1);
        push(result.value());
        return true;
    }

    Var captureUpvalue(int slot) {
        return stack.get(slot).asVar();
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

    VMResult refOps(int op) {
        return switch (op) {
            case OpCode.Ref -> {
                push(new Value(pop()));
                yield VMResult.OK;
            }

            case OpCode.Deref -> {
                if (!peek(0).isRef) {
                    runtimeError("Type", "Can't dereference non-ref");
                    yield VMResult.ERROR;
                }
                push(pop().asRef());
                yield VMResult.OK;
            }

            case OpCode.SetRef -> {
                if (!peek(0).isRef) {
                    runtimeError("Type", "Can't set non-ref");
                    yield VMResult.ERROR;
                }
                push(pop().setRef(pop()));
                yield VMResult.OK;
            }

            default -> throw new IllegalArgumentException("Invalid ref op: " + op);
        };
    }

    VMResult freeOps(int op) {
        return switch (op) {
            case OpCode.DropGlobal -> {
                String name = readString();
                if (globals.containsKey(name)) {
                    globals.remove(name);
                    yield VMResult.OK;
                }
                else {
                    runtimeError("Scope", "No such global: " + name);
                    yield VMResult.ERROR;
                }
            }
            case OpCode.DropLocal -> {
                int slot = readByte();
                if (slot + frame.slots == stack.count - 1) {
                    stack.pop();
                }
                stack.set(slot + frame.slots, null);
                yield VMResult.OK;
            }
            case OpCode.DropUpvalue -> {
                int slot = readByte();
                if (slot == frame.closure.upvalueCount - 1) {
                    frame.closure.upvalueCount--;
                }
                frame.closure.upvalues[slot] = null;
                yield VMResult.OK;
            }
            default -> throw new IllegalArgumentException("Invalid free op: " + op);
        };
    }

    VMResult pattern(int op) {
        return switch (op) {
            case OpCode.PatternVars -> {
                push(Value.patternBinding(readString()));
                yield VMResult.OK;
            }
            case OpCode.Pattern -> {
                int fieldCount = readByte();

                Map<String, Value> cases = new HashMap<>();
                Map<String, String> matches = new HashMap<>();
                List<String> keys = new ArrayList<>();
                for (int i = 0; i < fieldCount; i++) {
                    String name = readString();
                    Value val = pop();

                    if (val.isPatternBinding) {
                        matches.put(name, val.asPatternBinding());
                        keys.add(0, name);
                    }
                    else {
                        cases.put(name, val);
                    }
                }

                Value pattern = pop();

                push(new Value(new Pattern(pattern, cases, keys.toArray(new String[0]), matches)));

                yield VMResult.OK;
            }
            default -> throw new IllegalArgumentException("How did you get here?");
        };
    }

    public VMResult run() {
        frame = frames.peek();
        int exitLevel = frames.count - 1;

        while (true) {
            if (Shell.logger.debug) {
                Shell.logger.debug("          ");
                for (int i = 0; i < stack.count; i++) {
                    Shell.logger.debug("[ ");
                    Shell.logger.debug(stack.get(i) == null ? "{ dropped }" : stack.get(i).toSafeString());
                    Shell.logger.debug(" ]");
                }
                Shell.logger.debug("\n");

                Disassembler.disassembleInstruction(frame.closure.function.chunk, frame.ip);
            }

            int instruction = readByte();
            VMResult res = switch (instruction) {
                case OpCode.Return -> {
                    Value result = pop();
                    if (frame.catchError) result = new Value(new Result(result));
                    CallFrame frame = frames.pop();
                    if (frames.count == 0) {
                        yield VMResult.EXIT;
                    }

                    String type = result.type();
                    if (!frame.returnType.equals("any") && !type.equals(frame.returnType)) {
                        runtimeError("Type", "Expected " + frame.returnType + " but got " + type);
                        yield VMResult.ERROR;
                    }

                    boolean isConstructor = frame.closure.function.name.equals("<make>");
                    Value bound = frame.bound;

                    stack.setTop(frame.slots);
                    this.frame = frames.peek();
                    popTraceback();

                    if (isConstructor) {
                        push(bound);
                    }
                    else {
                        push(result);
                        if (frame.memoize) {
                            memo.storeCache(result);
                        }
                    }

                    if (exitLevel == frames.count) {
                        yield VMResult.EXIT;
                    }

                    if (frame.addPeek) {
                        this.frame.ip = frame.ip;
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
                    push(value);
                    yield VMResult.OK;
                }

                case OpCode.Pattern,
                        OpCode.PatternVars -> pattern(instruction);

                case OpCode.Throw -> {
                    Value type = pop();
                    Value reason = pop();
                    runtimeError(type.asString(), reason.asString());
                    yield VMResult.ERROR;
                }

                case OpCode.Import -> {
                    String name = readString();
                    String varName = readString();

                    Value f = pop();
                    if (!f.isFunc) {
                        if (!libraries.containsKey(name)) {
                            runtimeError("Import", "Library '" + name + "' not found");
                            yield VMResult.ERROR;
                        }
                        Value lib = new Value(libraries.get(name));
                        globals.put(varName, new Var(
                                "namespace",
                                lib,
                                true
                        ));
                        push(lib);
                        yield VMResult.OK;
                    }

                    JFunc func = f.asFunc();

                    VM runner = new VM(func);
                    runner.trace(name);
                    VMResult importres = runner.run();
                    if (importres == VMResult.ERROR) {
                        yield VMResult.ERROR;
                    }

                    Value space = new Value(runner.asNamespace(name));
                    globals.put(varName, new Var(
                            "namespace",
                            space,
                            true
                    ));
                    push(space);
                    yield VMResult.OK;
                }

                case OpCode.Extend -> {
                    String fn = readString();

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
                            runtimeError("Imaginary File", "File '" + fn + "' not found");
                            yield VMResult.ERROR;
                        }
                        ClassLoader cl = new URLClassLoader(urls);
                        Class<?> loadedClass = cl.loadClass("jpext." + fn);
                        Constructor<?> constructor = loadedClass.getConstructor(VM.class);
                        Object loadedObject = constructor.newInstance(this);
                        if (loadedObject instanceof JPExtension) {
                            JPExtension extension = (JPExtension) loadedObject;
                            extension.setup();
                        }
                        else {
                            runtimeError("Imaginary File", "File '" + fn + "' is not a valid extension");
                            yield VMResult.ERROR;
                        }
                    } catch (Exception e) {
                        runtimeError("Internal", "Failed to load extension (" + e.getMessage() + ")");
                        yield VMResult.ERROR;
                    }

                    yield VMResult.OK;
                }

                case OpCode.Enum -> {
                    Value enumerator = readConstant();
                    globals.put(enumerator.asEnum().name(), new Var(
                            "Enum",
                            enumerator,
                            true
                    ));
                    push(enumerator);

                    boolean isPublic = readByte() == 1;
                    if (isPublic) {
                        JEnum enumObj = enumerator.asEnum();
                        for (Map.Entry<String, JEnumChild> name : enumObj.children().entrySet()) {
                                globals.put(name.getKey(), new Var(
                                        enumObj.name(),
                                        new Value(name.getValue()),
                                        true
                                ));
                        }
                    }

                    yield VMResult.OK;
                }

                case OpCode.Copy -> {
                    push(pop().shallowCopy());
                    yield VMResult.OK;
                }

                case OpCode.Iter -> iter();

                case OpCode.Spread -> {
                    push(new Value(new Spread(pop().asList())));
                    yield VMResult.OK;
                }

                case OpCode.Ref,
                        OpCode.Deref,
                        OpCode.SetRef -> refOps(instruction);

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

                case OpCode.FromBytes,
                        OpCode.ToBytes -> byteOps(instruction);

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
                    int defaultCount = readByte();
                    JClosure closure = new JClosure(func);

                    if (func.name == null) func.name = tracebacks.peek().context;

                    Value[] defaults = new Value[func.arity];
                    for (int i = func.arity - 1; i >= func.arity - defaultCount; i--) {
                        defaults[i] = pop();
                        func.defaultCount++;
                    }
                    func.defaults = new ArrayList<>(Arrays.asList(defaults));

                    push(new Value(closure));

                    for (int i = 0; i < closure.upvalueCount; i++) {
                        int isLocal = readByte();
                        int index = readByte();

                        switch (isLocal) {
                            case 0 -> closure.upvalues[i] = frame.closure.upvalues[index];
                            case 1 -> closure.upvalues[i] = captureUpvalue(frame.slots + index);
                            case 2 -> closure.upvalues[i] = globals.get(frame.closure.function.chunk.constants().valuesArray[index].asString());
                        }
                    }

                    yield VMResult.OK;
                }

                case OpCode.GetAttr,
                        OpCode.SetAttr -> attrOps(instruction);

                case OpCode.GetUpvalue,
                        OpCode.SetUpvalue -> upvalueOps(instruction);

                case OpCode.BitAnd,
                        OpCode.BitOr,
                        OpCode.BitXor,
                        OpCode.LeftShift,
                        OpCode.RightShift,
                        OpCode.SignRightShift,
                        OpCode.BitCompl -> bitOps(instruction);

                case OpCode.NullErr -> {
                    boolean bit = readByte() == 1;
                    if (bit) {
                        Pair<Integer, Integer> pair = new Pair<>(frames.count, stack.count);
                        nehStack.push(pair);
                    }
                    else {
                        nehStack.pop();
                    }
                    yield VMResult.OK;
                }

                case OpCode.Chain -> {
                    Value b = pop();
                    Value a = pop();
                    if (a.isNull) {
                        push(b);
                    }
                    else {
                        push(a);
                    }
                    yield VMResult.OK;
                }

                case OpCode.IncrNullErr -> {
                    nehStack.peek().b++;
                    yield VMResult.OK;
                }

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

                        List<String> rawType = readConstant().asType();
                        String type = readType(rawType);
                        attributes.put(attrname, new ClassAttr(pop(), type, rawType, isstatic, isprivate));
                    }

                    List<String> genericNames = new ArrayList<>();
                    int genericCount = readByte();
                    for (int i = 0; i < genericCount; i++)
                        genericNames.add(readString());


                    push(new Value(new JClass(name, attributes, genericNames, superClass)));
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

                    Value at = get(slot);
                    String atType = at.type();
                    if (!type.equals("any") && !atType.equals(type)) {
                        runtimeError("Type", "Expected type " + type + " but got " + atType);
                        yield VMResult.ERROR;
                    }

                    stack.set(frame.slots + slot, new Value(new Var(type, at, constant)));
                    yield VMResult.OK;
                }

                case OpCode.Access -> access();

                case OpCode.DropGlobal,
                        OpCode.DropLocal,
                        OpCode.DropUpvalue -> freeOps(instruction);

                case OpCode.Destruct -> destruct();

                case OpCode.Header -> header();

                default -> throw new RuntimeException("Unknown opcode: " + instruction);
            };
            if (res == VMResult.EXIT) {
                Shell.logger.debug("Exiting\n");
                return VMResult.OK;
            }
            else if (res == VMResult.ERROR) {
                if (frame.catchError) {
                    frames.pop();
                    if (frames.count == 0) {
                        return VMResult.OK;
                    }

                    stack.setTop(frame.slots);
                    frame = frames.peek();
                    popTraceback();

                    Value result = new Value(new Result(lastError.a, lastError.b));
                    push(result);
                    if (frame.memoize) {
                        memo.storeCache(result);
                    }

                    if (exitLevel == frames.count) {
                        return VMResult.OK;
                    }
                    continue;
                }
                if (nehStack.count > 0) {
                    Pair<Integer, Integer> neh = nehStack.peek();
                    while (frames.count > neh.a) {
                        tracebacks.pop();
                        frames.pop();
                    }
                    frame = frames.peek();
                    stack.setTop(neh.b);
                    push(new Value());
                    continue;
                }
                if (safe) {
                    while (frames.count > exitLevel) {
                        tracebacks.pop();
                        frames.pop();
                    }
                    frame = frames.peek();
                    stack.setTop(frames.peek(-1).slots);
                }
                return VMResult.ERROR;
            }
        }
    }

    VMResult destruct() {
        Namespace v = pop().asNamespace();
        push(new Value());
        int args = readByte();
        if (args == -1) {
            globals.putAll(v.values());
            return VMResult.OK;
        }
        String[] names = new String[args];
        for (int i = 0; i < args; i++)
            names[i] = readString();
        Map<String, Var> values = v.values();
        for (String name : names) {
            if (values.containsKey(name)) {
                globals.put(name, values.get(name));
            }
            else {
                runtimeError("Scope", "Undefined field: " + name);
                return VMResult.ERROR;
            }
        }
        return VMResult.OK;
    }

    VMResult header() {
        int command = readByte();
        int argc = readByte();
        String[] args = new String[argc];
        for (int i = 0; i < argc; i++)
            args[i] = readString();

        int rArgc = switch (command) {
            case HeadCode.Memoize -> 0;
            case HeadCode.SetMainClass, HeadCode.SetMainFunction -> 1;
            default -> -1;
        };
        if (argc != rArgc && rArgc != -1) {
            runtimeError("Argument Count", "Expected " + rArgc + " arguments, got " + argc);
            return VMResult.ERROR;
        }

        switch (command) {
            case HeadCode.Memoize -> frame.memoize = true;
            case HeadCode.SetMainFunction -> mainFunction = args[0];
            case HeadCode.SetMainClass -> mainClass = args[0];
            case HeadCode.Export -> {
                if (exports == null) {
                    exports = new ArrayList<>();
                }
                exports.addAll(Arrays.asList(args));
            }
        }

        push(new Value());
        return VMResult.OK;
    }

    public void finish(String[] args) {
        List<Value> argsV = new ArrayList<>();
        for (String arg : args) {
            argsV.add(new Value(arg));
        }
        Value val = new Value(argsV);

        if (mainFunction != null) {
            Var var = globals.get(mainFunction);
            if (var == null || !var.val.isClosure) {
                runtimeError("Scope", "Main function not found");
            }

            JClosure closure = var.val.asClosure();
            push(new Value(closure));

            push(val);

            boolean res = call(closure, new Value[]{ val }, new HashMap<>(), new String[0]);
            if (!res) {
                return;
            }
            run();
        }
        else if (mainClass != null) {
            Var var = globals.get(mainClass);
            if (var == null || !var.val.isClass) {
                runtimeError("Scope", "Main class not found");
            }

            JClass clazz = var.val.asClass();
            Value method = clazz.getField("main", true);
            if (method == null || !method.isClosure) {
                runtimeError("Scope", "Main method not found");
            }
            JClosure closure = method.asClosure();

            push(new Value(closure));
            push(val);

            boolean res = call(closure, new Value(clazz), new Value[]{ val }, new HashMap<>(), new String[0]);
            if (!res) {
                return;
            }
            run();
        }
    }

    VMResult iter() {
        int iterated = readByte();
        int variable = readByte();
        int jump = readByte();

        List<Value> vals = get(iterated).asVar().val.asList();
        if (vals.size() == 0) {
            moveIP(jump);
            return VMResult.OK;
        }

        get(variable).asVar().val(vals.get(0));
        vals.remove(0);
        return VMResult.OK;
    }

    public Namespace asNamespace(String name) {
        return new Namespace(name, globals, exports == null ? new ArrayList<>(globals.keySet()) : exports);
    }

}
