package lemon.jpizza.compiler.vm;

import lemon.jpizza.Shell;
import lemon.jpizza.compiler.types.GenericType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.objects.FuncType;
import lemon.jpizza.compiler.types.objects.NamespaceType;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.values.functions.NativeResult;

import java.util.HashMap;
import java.util.Map;

public abstract class JPExtension {
    protected final VM vm;
    protected final String lib;
    abstract public String name();

    Map<String, Type> fields = new HashMap<>();

    public JPExtension(VM vm) {
        this.vm = vm;
        this.lib = name();
    }

    private JPExtension(VM vm, String lib) {
        this.vm = vm;
        this.lib = lib;
    }

    protected void print(Object str) {
        Shell.logger.out(str);
    }

    protected void println(Object str) {
        Shell.logger.outln(str);
    }

    protected static final NativeResult Ok = NativeResult.Ok();

    protected static NativeResult Ok(Value val) {
        return NativeResult.Ok(val);
    }

    protected static NativeResult Ok(Object obj) {
        return Ok(Value.fromObject(obj));
    }

    protected static NativeResult Err(String title, String msg) {
        return NativeResult.Err(title, msg);
    }

    protected void func(String name, JNative.Method method, Type returnType, int argc) {
        if (vm == null) {
            Type[] types = new Type[argc];
            for (int i = 0; i < argc; i++)
                types[i] = Types.ANY;
            fields.put(name, new FuncType(returnType, types, new GenericType[0], false));
        } else
            vm.defineNative(lib, name, method, argc);
    }

    protected void func(String name, JNative.Method method, Type returnType, Type... types) {
        if (vm == null)
            fields.put(name, new FuncType(returnType, types, new GenericType[0], false));
        else
            vm.defineNative(lib, name, method, types);
    }

    protected void var(String name, Value val, Type type) {
        if (vm == null)
            fields.put(name, type);
        else
            vm.defineVar(lib, name, val);
    }

    protected void var(String name, Object val, Type type) {
        var(name, Value.fromObject(val), type);
    }

    public void Start() {
        setup();
        Shell.libraries.put(name(), new NamespaceType(fields));
    }

    abstract public void setup();

}
