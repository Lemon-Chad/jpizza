package lemon.jpizza.compiler.values;

import java.util.ArrayList;
import java.util.List;

public class JNative {

    public interface Method {
        NativeResult call(Value[] stack);
    }

    String name;
    Method method;
    int argc;
    List<String> types;

    public JNative(String name, Method method, int argc, List<String> types) {
        this.name = name;
        this.method = method;
        this.argc = argc;
        this.types = types;
    }

    public JNative(String name, Method method, int argc) {
        this(name, method, argc, new ArrayList<>());
        for (int i = 0; i < argc; i++)
            types.add("any");
    }

    public NativeResult call(Value[] args) {
        if (args.length != argc)
            return NativeResult.Err("Argument Count", "Expected " + argc + " arguments, got " + args.length);

        for (int i = 0; i < argc; i++)
            if (!types.get(i).equals("any") && !args[i].type().equals(types.get(i)))
                return NativeResult.Err("Type", "Expected " + types.get(i) + " for argument " + i + ", got " + args[i].type());

        return method.call(args);
    }

    public String toString() {
        return "<function-" + name + ">";
    }
}
