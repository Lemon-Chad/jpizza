package lemon.jpizza;

import lemon.jpizza.errors.Error;
import lemon.jpizza.objects.Obj;

public class SpeedTest {
    public static void main(String[] args) {

        Shell.initLibs();

        Pair<Obj, Error> out;
        String demoCode = """
import time;
println(time::stopwatch(! -> (for (i -> 1:10000000) => i + 1)));
println("Why is camel so bad???");
        """;

        Shell.logger.enableLogging();
        out = Shell.run("<unit-test>", demoCode, false);
        if (out.b != null)
            Shell.logger.fail(out.b.asString());

    }
}
