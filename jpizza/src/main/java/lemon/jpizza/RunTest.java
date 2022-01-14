package lemon.jpizza;

import lemon.jpizza.errors.Error;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class RunTest {
    public static void main(String[] args) throws IOException {
        double start = System.currentTimeMillis();
        Shell.main(new String[]{ "UnitTest.devp", "-rf", "-r" });
        double end = System.currentTimeMillis();
        Shell.logger.outln("Time: " + (end - start) + "ms");
    }
}
