package lemon.jpizza.compiler.vm;

import lemon.jpizza.Shell;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.values.functions.NativeResult;

import java.util.List;

abstract class JPExtension {
    private final VM vm;
    private final String lib;
    private final static String name = "jpizza";

    public JPExtension(VM vm) {
        this(vm, name);
    }

    public JPExtension(VM vm, String lib) {
        this.vm = vm;
        this.lib = lib;
    }

    private void print(Object str) {
        Shell.logger.out(str);
    }

    private void println(Object str) {
        Shell.logger.outln(str);
    }

    private static final NativeResult Ok = NativeResult.Ok();

    private static NativeResult Ok(Value val) {
        return NativeResult.Ok(val);
    }

    private static NativeResult Ok(Object obj) {
        return Ok(Value.fromObject(obj));
    }

    private static NativeResult Err(String title, String msg) {
        return NativeResult.Err(title, msg);
    }

    private void define(String name, JNative.Method method, int argc) {
        vm.defineNative(lib, name, method, argc);
    }

    private void define(String name, JNative.Method method, List<String> types) {
        vm.defineNative(lib, name, method, types);
    }

    private void define(String name, Value val) {
        vm.defineVar(lib, name, val);
    }

    abstract public void setup();

}
