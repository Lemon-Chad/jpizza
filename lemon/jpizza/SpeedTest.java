package lemon.jpizza;

import lemon.jpizza.Errors.Error;
import lemon.jpizza.Objects.Obj;

public class SpeedTest {
    public static void main(String[] args) {

        Shell.initLibs();

        Pair<Obj, Error> out;
        String demoCode = """
        import time;
        println(time::stopwatch(! -> for (i -> 0:10000000) => i + 1 + 1 + 1));
        """;

        Shell.logger.enableLogging();
        out = Shell.run("<unit-test>", demoCode);
        if (out.b != null)
            System.out.println(out.b.asString());

    }
}
