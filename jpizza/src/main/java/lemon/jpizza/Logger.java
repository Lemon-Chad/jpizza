package lemon.jpizza;

import com.diogonunes.jcolor.Attribute;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Bytes;
import lemon.jpizza.objects.primitives.Dict;
import lemon.jpizza.objects.primitives.PList;

import java.util.List;
import java.util.Map;

import static com.diogonunes.jcolor.Ansi.colorize;

public class Logger {
    boolean log = true;
    boolean tips = false;
    final int omitt = 5;
    final int tape = 40;

    public String ots(Object text) {
        if (text instanceof PList) {
            StringBuilder sb = new StringBuilder();
            List<Obj> l = ((Obj) text).list;

            for (int i = 0; i < l.size(); i++)
                if (i >= omitt && i < l.size() - omitt) {
                    if (i == omitt + 1) sb.append("..., ");
                } else sb.append(ots(l.get(i))).append(", ");

            return "[ " + sb.toString() + "len=" + l.size() + " ]";
        }
        else if (text instanceof Dict) {
            StringBuilder sb = new StringBuilder();
            Map<Obj, Obj> d = ((Obj) text).map;

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
            Obj b = (Obj) text;

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

    public String safeColorize(String text, Attribute color) {
        if (Shell.fileEncoding.equals("UTF-8"))
            return colorize(text, color);
        return text;
    }

    public void warn(Object text) {
        if (log)
            System.out.println(safeColorize(
                    getTape("WARNING") + "\n" + ots(text),
                    Attribute.YELLOW_TEXT()
            ));
    }

    private String getTape(String message) {
        return " ".repeat((tape - message.length()) / 2) + message + "\n" +
                (Shell.fileEncoding.equals("UTF-8") ? "─" : "_").repeat(tape);
    }

    public void fail(Object text) {
        if (log)
            System.out.println(safeColorize(
                    getTape("FAILURE") + "\n" + ots(text),
                    Attribute.RED_TEXT()
            ));
    }

    public void tip(Object text) {
        if (log && tips)
            System.out.println(safeColorize(
                    getTape("TIP") + "\n" + ots(text),
                    Attribute.CYAN_TEXT()
            ));
    }

    public void outln(Object text) {
        if (log) System.out.println(ots(text));
    }

    public void disableLogging() { log = false; }
    public void enableLogging() { log = true; }

    public void enableTips() { tips = true; }
    public void disableTips() { tips = false; }
}
