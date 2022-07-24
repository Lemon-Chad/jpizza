package lemon.jpizza.compiler.values.functions;

import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.values.Value;

public class JNative {

    public static Object Method;

    public interface Method {
        NativeResult call(Value[] stack);
    }

    final String name;
    final Method method;
    final int argc;
    final Type[] types;

    public JNative(String name, Method method, int argc, Type[] types) {
        this.name = name;
        this.method = method;
        this.argc = argc;
        this.types = types;
    }

    public JNative(String name, Method method, int argc) {
        this(name, method, argc, new Type[argc == -1 ? 0 : argc]);
        for (int i = 0; i < argc; i++)
            types[i] = Types.ANY;
    }

    public NativeResult call(Value[] args) {
        if (args.length != argc && argc != -1)
            return NativeResult.Err("Argument Count", "Expected " + argc + " arguments, got " + args.length);
        return method.call(args);
    }

    public String toString() {
        return "<function-" + name + ">";
    }
}
