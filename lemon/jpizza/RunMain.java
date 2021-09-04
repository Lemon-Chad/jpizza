package lemon.jpizza;

public class RunMain {
    public static void main(String[] args) {
        Shell.initLibs();
        var out = Shell.run("<test>", """
                run("main.devp");
                """, false);
        if (out.b != null)
            System.out.println(out.b.asString());
    }
}
