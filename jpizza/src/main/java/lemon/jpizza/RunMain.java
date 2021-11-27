package lemon.jpizza;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RunMain {
    public static void main(String[] args) throws IOException {
        String text = Files.readString(Path.of("main.devp"));

        double start = System.currentTimeMillis();
        var res = Shell.compile("main.devp", text, "main.jbox");
        if (res != null) {
            Shell.logger.fail(res.asString());
            return;
        }
        Shell.runCompiled("main.jbox", "main.jbox");
        double end = System.currentTimeMillis();
        Shell.logger.outln("Time: " + (end - start) + "ms");

        Shell.logger.reset();

        start = System.currentTimeMillis();
         var pair = Shell.run("main.devp", text, false);
         if (pair.b != null) {
             Shell.logger.fail(pair.b.asString());
         }
        end = System.currentTimeMillis();
        Shell.logger.outln("Time: " + (end - start) + "ms");
    }
}
