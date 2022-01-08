package lemon.jpizza.compiler.libraries;

import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Generators extends JPExtension {
    @Override
    public String name() { return "gens"; }

    public Generators(VM vm) {
        super(vm);
    }

    @Override
    public void setup() {
        func("range", (args) -> {
            double start = args[0].asNumber();
            double end = args[1].asNumber();
            double step = args[2].asNumber();
            List<Double> list = new ArrayList<>();
            for (double i = start; i < end; i += step)
                list.add(i);
            return Ok(list);
        }, Arrays.asList("num", "num", "num"));
        func("linear", (args) -> {
            double start = args[0].asNumber();
            double end = args[1].asNumber();
            double step = args[2].asNumber();

            double m = args[3].asNumber();
            double b = args[4].asNumber();

            List<Double> list = new ArrayList<>();
            for (double i = start; i < end; i += step)
                list.add(m * i + b);
            return Ok(list);
        }, Arrays.asList("num", "num", "num", "num", "num"));
        func("quadratic", (args) -> {
            double start = args[0].asNumber();
            double end = args[1].asNumber();
            double step = args[2].asNumber();

            double a = args[3].asNumber();
            double b = args[4].asNumber();
            double c = args[5].asNumber();

            List<Double> list = new ArrayList<>();
            for (double i = start; i < end; i += step)
                list.add(a * i * i + b * i + c);
            return Ok(list);
        }, Arrays.asList("num", "num", "num", "num", "num", "num"));
    }
}
