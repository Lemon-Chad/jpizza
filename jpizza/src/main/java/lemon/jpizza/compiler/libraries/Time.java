package lemon.jpizza.compiler.libraries;

import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import java.util.Collections;
import java.util.List;

public class Time extends JPExtension {
    @Override
    public String name() { return "time"; }

    public Time(VM vm) {
        super(vm);
    }

    @Override
    public void setup() {
        func("epoch", (args) -> Ok(System.currentTimeMillis()), 0);
        func("halt", (args) -> {
            try {
                Thread.sleep(args[0].asNumber().intValue());
            } catch (InterruptedException e) {
                return Err("Internal", "Interrupted");
            }
            return Ok;
        }, Collections.singletonList("num"));
    }
}
