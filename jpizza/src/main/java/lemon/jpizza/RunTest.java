package lemon.jpizza;

import java.io.IOException;

public class RunTest {
    public static void main(String[] args) {
        double start = System.currentTimeMillis();
        Shell.main("-c UnitTest.devp -r UnitTest.jbox".split(" "));
        double end = System.currentTimeMillis();
        Shell.logger.outln("Time: " + (end - start) + "ms");
    }
}
