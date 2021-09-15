package lemon.jpizza;

import lemon.jpizza.Errors.Error;
import lemon.jpizza.Objects.Obj;

public class SpeedTest {
    public static void main(String[] args) {

        Shell.initLibs();

        Pair<Obj, Error> out;
        String demoCode = """
for (i -> 1:1000001) => i;
println("Why is camel so bad???");
        """;

        Shell.logger.enableLogging();
        out = Shell.run("<unit-test>", demoCode, false);
        if (out.b != null)
            Shell.logger.fail(out.b.asString());

    }
}
