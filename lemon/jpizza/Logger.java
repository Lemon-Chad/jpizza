package lemon.jpizza;

import com.diogonunes.jcolor.Attribute;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Bytes;
import lemon.jpizza.Objects.Primitives.Dict;
import lemon.jpizza.Objects.Primitives.PList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.diogonunes.jcolor.Ansi.colorize;

public class Logger {
    boolean log = true;
    int omitt = 5;
    int tape = 10;

    public String ots(Object text) {
        if (text instanceof PList) {
            StringBuilder sb = new StringBuilder();
            List<Obj> l = ((PList) text).trueValue();

            for (int i = 0; i < l.size(); i++)
                if (i >= omitt && i < l.size() - omitt) {
                    if (i == omitt + 1) sb.append("..., ");
                } else sb.append(ots(l.get(i))).append(", ");

            return "[ " + sb.toString() + "len=" + l.size() + " ]";
        }
        else if (text instanceof Dict) {
            StringBuilder sb = new StringBuilder();
            Map<Obj, Obj> d = ((Dict) text).trueValue();

            Obj[] keys = d.keySet().toArray(new Obj[0]);
            for (int i = 0; i < keys.length; i++)
                if (i >= omitt && i < keys.length - omitt) {
                    if (i == omitt + 1) sb.append("..., ");
                } else sb.append(ots(keys[i])).append(": ")
                        .append(ots(d.get(keys[i]))).append(", ");

            return "{ " + sb.toString() + "len=" + keys.length + " }";
        }
        else if (text instanceof Bytes) {
            StringBuilder sb = new StringBuilder();
            Bytes b = (Bytes) text;

            for (int i = 0; i < b.arr.length; i++)
                if (i >= omitt && i < b.arr.length - omitt) {
                    if (i == omitt + 1) sb.append("..., ");
                } else sb.append(b.arr[i]).append(", ");

            return "{ " + sb.toString() + "len=" + b.arr.length + " }";
        } else if (text instanceof Obj) {
            return ((Obj) text).astring().toString();
        }
        return text.toString();
    }

    public void out(Object text) {
        if (log) System.out.print(ots(text));
    }

    public void warn(Object text) {
        if (log)
            System.out.println(colorize(
                    getTape("WARNING") + "\n" + ots(text),
                    Attribute.YELLOW_TEXT()
            ));
    }

    private String getTape(String message) {
        return " ".repeat((tape - message.length()) / 2) + message + "\n" + "-".repeat(tape);
    }

    public void fail(Object text) {
        if (log)
            System.out.println(colorize(
                    getTape("FAIL") + "\n" + ots(text),
                    Attribute.RED_TEXT()
            ));
    }

    public void outln(Object text) {
        if (log) System.out.println(ots(text));
    }

    public void disableLogging() { log = false; }
    public void enableLogging() { log = true; }
}
