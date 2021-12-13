package lemon.jpizza;

import com.github.tomaslanger.chalk.Chalk;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Bytes;
import lemon.jpizza.objects.primitives.Dict;
import lemon.jpizza.objects.primitives.PList;

import java.util.List;
import java.util.Map;

public class Logger {
    boolean log = true;
    boolean tips = false;
    public final boolean debug = true;
    final int omitt = 5;
    final int tape = 40;

    public void reset() {
        log = true;
        tips = false;
    }

    @SuppressWarnings("DuplicatedCode")
    public String ots(Object text) {
        if (text instanceof PList) {
            StringBuilder sb = new StringBuilder();
            List<Obj> l = ((Obj) text).list;

            for (int i = 0; i < l.size(); i++)
                if (i >= omitt && i < l.size() - omitt) {
                    if (i == omitt + 1) sb.append("..., ");
                }
                else sb.append(ots(l.get(i))).append(", ");

            return "[ " + sb + "len=" + l.size() + " ]";
        }
        else if (text instanceof Dict) {
            StringBuilder sb = new StringBuilder();
            Map<Obj, Obj> d = ((Obj) text).map;

            Obj[] keys = d.keySet().toArray(new Obj[0]);
            for (int i = 0; i < keys.length; i++)
                if (i >= omitt && i < keys.length - omitt) {
                    if (i == omitt + 1) sb.append("..., ");
                }
                else sb.append(ots(keys[i])).append(": ")
                        .append(ots(d.get(keys[i]))).append(", ");

            return "{ " + sb + "len=" + keys.length + " }";
        }
        else if (text instanceof Bytes) {
            StringBuilder sb = new StringBuilder();
            Obj b = (Obj) text;

            for (int i = 0; i < b.arr.length; i++)
                if (i >= omitt && i < b.arr.length - omitt) {
                    if (i == omitt + 1) sb.append("..., ");
                }
                else sb.append(b.arr[i]).append(", ");

            return "{ " + sb + "len=" + b.arr.length + " }";
        }
        else if (text instanceof Obj) {
            return ((Obj) text).astring().toString();
        }
        else if (text instanceof Value) {
            Value val = (Value) text;
            if (val.isList) {
                StringBuilder sb = new StringBuilder();
                List<Value> l = val.asList();

                for (int i = 0; i < l.size(); i++)
                    if (i >= omitt && i < l.size() - omitt) {
                        if (i == omitt + 1) sb.append("..., ");
                    }
                    else sb.append(ots(l.get(i))).append(", ");

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
                    else sb.append(ots(keys[i])).append(": ")
                            .append(ots(d.get(keys[i]))).append(", ");

                return "{ " + sb + "len=" + keys.length + " }";
            }
            else if (val.isNull) {
                return "null";
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
        return " ".repeat((tape - message.length()) / 2) + message + "\n" +
                (Shell.fileEncoding.equals("UTF-8") ? "─" : "_").repeat(tape);
    }

    public void fail(Object text) {
        if (log)
            System.out.println(Chalk.on(
                    getTape("FAILURE") + "\n" + ots(text)
            ).red());
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
}
