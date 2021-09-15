package lemon.jpizza;

public class RunClient {
    public static void main(String[] args) {
        Shell.initLibs();
        var out = Shell.run("<test>", """
                run("client.devp");
                """, false);
        if (out.b != null)
            Shell.logger.fail(out.b.asString());
    }
}
