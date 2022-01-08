package lemon.jpizza;

import lemon.jpizza.errors.Error;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class RunTest {
    public static void main(String[] args) throws IOException {
        String text = Files.lines(Paths.get("UnitTest.devp")).collect(Collectors.joining("\n"));

        Error e = Shell.compile("UnitTest.devp", text, "UnitTest.jbox");
        if (e != null) {
            Shell.logger.fail(e.asString());
            return;
        }
        double start = System.currentTimeMillis();
        Shell.runCompiled("UnitTest.jbox", "UnitTest.jbox", args);
        double end = System.currentTimeMillis();
        Shell.logger.outln("Time: " + (end - start) + "ms");
    }
}
