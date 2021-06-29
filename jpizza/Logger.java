package lemon.jpizza;

public class Logger {
    boolean log = true;
    public void out(Object text) {
        if (log) System.out.print(text);
    }
    public void outln(Object text) {
        if (log) System.out.println(text);
    }
    public void disableLogging() { log = false; }
    public void enableLogging() { log = true; }
}
