package lemon.jpizza.compiler.vm;

import lemon.jpizza.compiler.values.functions.JNative;

import java.util.List;

abstract class Extension {
    private final VM vm;
    private final String lib;
    public Extension(VM vm, String name) {
        this.vm = vm;
        this.lib = name;
    }

    private void define(String name, JNative.Method method, int argc) {
        vm.defineNative(lib, name, method, argc);
    }

    private void define(String name, JNative.Method method, List<String> types) {
        vm.defineNative(lib, name, method, types);
    }

    abstract public void setup();

}
