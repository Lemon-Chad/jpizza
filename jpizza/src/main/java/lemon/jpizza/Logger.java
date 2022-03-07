package lemon.jpizza;

import com.github.tomaslanger.chalk.Chalk;
import lemon.jpizza.compiler.values.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Logger {
    boolean log = true;
    boolean tips = false;
    public final boolean debug = true;
    final int omitt = 5;
    final int tape = 40;
    final Scanner scanner = new Scanner(System.in);

    public void reset() {
        log = true;
        tips = false;
    }

    public String ots(Object text) {
        return ots(text, false);
    }

    @SuppressWarnings("DuplicatedCode")
    public String ots(Object text, boolean stringInner) {
        if (text instanceof Value) {
            Value val = (Value) text;
            if (val.isList) {
                StringBuilder sb = new StringBuilder();
                List<Value> l = val.asList();

                for (int i = 0; i < l.size(); i++)
                    if (i >= omitt && i < l.size() - omitt) {
                        if (i == omitt + 1) sb.append("..., ");
                    }
                    else sb.append(ots(l.get(i), true)).append(", ");

                return "[ " + sb + "len=" + l.size() + " ]";
            }
            else if (val.isMap) {
                StringBuilder sb = new StringBuilder();
                Map<Value, Value> d = val.asMap();

                Value[] keys = d.keySet().toArray(new Value[0]);
                for (int i = 0; i < keys.length; i++)
                    if (i >= omitt && i < keys.length - omitt) {
                        if (i == omitt + 1) sb.append("..., ");
                    }
                    else sb.append(ots(keys[i], true)).append(": ")
                            .append(ots(d.get(keys[i]), true)).append(", ");

                return "{ " + sb + "len=" + keys.length + " }";
            }
            else if (val.isNull) {
                return "null";
            }
            else if (val.isString && stringInner) {
                return "\"" + val.asString() + "\"";
            }
            return val.asString();
        }
        return text.toString();
    }

    public void out(Object text) {
        if (log)
            System.out.print(ots(text));
        System.out.flush();
    }

    public void warn(Object text) {
        if (log)
            System.out.println(Chalk.on(
                    getTape("WARNING") + "\n" + ots(text)
            ).yellow());
    }

    private String getTape(String message) {
        return String.join("", Collections.nCopies((tape - message.length()) / 2, " ")) + message + "\n" +
                String.join("", Collections.nCopies(tape, Shell.fileEncoding.equals("UTF-8") ? "─" : "_"));
    }

    public void fail(Object text) {
        if (log) {
            System.out.println(Chalk.on(
                    getTape("FAILURE") + "\n" + ots(text)
            ).red());
            System.exit(1);
        }
    }

    public void tip(Object text) {
        if (log && tips)
            System.out.println(Chalk.on(
                    getTape("TIP") + "\n" + ots(text)
            ).cyan());
    }

    public void outln(Object text) {
        if (log) System.out.println(ots(text));
    }

    public void disableLogging() { log = false; }
    public void enableLogging() { log = true; }

    public void enableTips() { tips = true; }
    public void disableTips() { tips = false; }

    public void debug(String format) {
        if (debug)
            System.out.print(Chalk.on(format).magenta());
    }

    public void debug(Value val) {
        if (debug)
            System.out.print(Chalk.on(ots(val)).magenta());
    }

    public String in() {
        return scanner.nextLine();
    }

}
