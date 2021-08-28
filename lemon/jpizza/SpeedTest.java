package lemon.jpizza;

import lemon.jpizza.Errors.Error;
import lemon.jpizza.Objects.Obj;

public class SpeedTest {
    public static void main(String[] args) {

        Shell.initLibs();

        Pair<Obj, Error> out;
        String demoCode = """
        import time;
        import gens;
        println("Test Equations:\\n\\tf(x) = x\\n\\tf(x) = 3x + 5\\n\\tf(x) = 5x^2 + 2x + 3\\nFor the first 1,000,000 numbers.\\n");
        println("Gens Library: " + time::stopwatch(! {
            gens::range(1, 1000000, 1);
            gens::linear(1, 1000000, 1, 3, 5);
            gens::quadratic(1, 1000000, 1, 5, 2, 3);
        }) + "ms");
        println("Native For Loop: " + time::stopwatch(! {
            for (i -> 1:1000001) => i;
            for (i -> 1:1000001) => 3 * i + 5;
            for (i -> 1:1000001) => 5 * i * i + 2 * i + 3;
        }) + "ms");
        """;

        Shell.logger.enableLogging();
        out = Shell.run("<unit-test>", demoCode);
        if (out.b != null)
            System.out.println(out.b.asString());

    }
}
