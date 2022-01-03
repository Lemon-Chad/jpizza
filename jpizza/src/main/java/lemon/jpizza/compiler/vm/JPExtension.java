package lemon.jpizza.compiler.vm;

import lemon.jpizza.Shell;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.values.functions.NativeResult;

import java.util.List;

public abstract class JPExtension {
    protected final VM vm;
    protected final String lib;
    abstract public String name();

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

    protected void func(String name, JNative.Method method, int argc) {
        vm.defineNative(lib, name, method, argc);
    }

    protected void func(String name, JNative.Method method, List<String> types) {
        vm.defineNative(lib, name, method, types);
    }

    protected void var(String name, Value val) {
        vm.defineVar(lib, name, val);
    }

    protected void var(String name, Object val) {
        var(name, Value.fromObject(val));
    }

    abstract public void setup();

}
