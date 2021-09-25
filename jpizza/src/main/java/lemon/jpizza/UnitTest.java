package lemon.jpizza;

public class UnitTest {
    public static void main(String[] args) {
        Shell.initLibs();
        var out = Shell.run("<test>", """
                run("UnitTest.devp");
                """, false);
        if (out.b != null)
            Shell.logger.fail(out.b.asString());
    }
}
