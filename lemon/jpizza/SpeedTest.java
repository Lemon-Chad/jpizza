package lemon.jpizza;

import lemon.jpizza.Errors.Error;
import lemon.jpizza.Objects.Obj;

public class SpeedTest {
    public static void main(String[] args) {

        Shell.initLibs();

        Pair<Obj, Error> out;
        String demoCode = """
for (i -> 1:1000001) => i;
        """;

        Shell.logger.enableLogging();
        out = Shell.run("<unit-test>", demoCode, false);
        if (out.b != null)
            System.out.println(out.b.asString());

    }
}
