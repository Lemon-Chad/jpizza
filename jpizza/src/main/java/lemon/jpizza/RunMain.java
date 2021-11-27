package lemon.jpizza;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RunMain {
    public static void main(String[] args) throws IOException {
        Shell.initLibs();

        String text = Files.readString(Path.of("main.devp"));

        var res = Shell.compile("main.devp", text, "main.jbox");
        if (res != null) {
            Shell.logger.fail(res.asString());
            return;
        }
        double start = System.currentTimeMillis();
        Shell.runCompiled("main.jbox", "main.jbox");
        double end = System.currentTimeMillis();
        Shell.logger.outln("Time: " + (end - start) + "ms");

//        Shell.logger.reset();
//
//        start = System.currentTimeMillis();
//        var pair = Shell.run("main.devp", text, false);
//        end = System.currentTimeMillis();
//        if (pair.b != null) {
//         Shell.logger.fail(pair.b.asString());
//        }
//        Shell.logger.outln("Time: " + (end - start) + "ms");
    }
}
