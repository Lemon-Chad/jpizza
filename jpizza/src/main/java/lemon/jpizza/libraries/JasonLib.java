package lemon.jpizza.libraries;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Lexer;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Dict;
import lemon.jpizza.objects.primitives.PList;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Shell;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens.TT;

import java.util.*;

@SuppressWarnings("unused")
public class JasonLib extends Library {

    static List<TT> acceptableJason = Arrays.asList(
            TT.STRING,
            TT.FLOAT,
            TT.INT,
            TT.LSQUARE,
            TT.RSQUARE,
            TT.COMMA,
            TT.BITE,
            TT.OPEN,
            TT.CLOSE,
            TT.EOF
    );

    public JasonLib(String name) { super(name, "json"); }

    public static void initialize() {
        initialize("json", JasonLib.class, new HashMap<>(){{
            put("loads", Collections.singletonList("value"));
            put("dumps", Collections.singletonList("value"));
        }});
    }

    public RTResult execute_loads(Context execCtx) {
        Obj value = ((Obj) execCtx.symbolTable.get("value")).astring();
        if (value.jptype != Constants.JPType.String) return new RTResult().failure(RTError.Type(
                value.get_start(), value.get_end(),
                "Expected String",
                execCtx
        ));

        String jdata = ((Str) value).trueValue();
        if (!jdata.startsWith("[") && !jdata.startsWith("{")) return new RTResult().failure(RTError.Type(
                value.get_start(), value.get_end(),
                "Expected List or Dict",
                execCtx
        ));

        var toks = new Lexer("json-loads", jdata).make_tokens();
        if (toks.b != null) return new RTResult().failure(RTError.MalformedData(
                toks.b.pos_start, toks.b.pos_end,
                "Malformed JSON",
                execCtx
        ));
        for (Token tok: toks.a) {
            if (!acceptableJason.contains(tok.type)) return new RTResult().failure(RTError.MalformedData(
                    tok.pos_start, tok.pos_end,
                    "Malformed JSON",
                    execCtx
            ));
        }

        var d = Shell.run("json-loads", jdata + ";", true);

        if (d.b != null) return new RTResult().failure(RTError.MalformedData(
                d.b.pos_start, d.b.pos_end,
                "Malformed JSON",
                execCtx
        ));

        return new RTResult().success(((PList) d.a).trueValue().get(0));
    }

    public String toStr(Obj o) {
        if (o.jptype == Constants.JPType.String) return "\"" + o.toString() + "\"";
        else if (o.jptype == Constants.JPType.Dict) return visitDictionary((Dict) o);
        else if (o.jptype == Constants.JPType.List) return visitList((PList) o);
        else return o.toString();
    }

    public String visitList(PList l) {
        StringBuilder sb = new StringBuilder();

        List<Obj> lst = l.trueValue();
        for (Obj item : lst)
            sb.append(toStr(item)).append(",");
        return "[" + sb.substring(0, sb.length() - 1) + "]";
    }

    public String visitDictionary(Dict d) {
        StringBuilder sb = new StringBuilder();

        Map<Obj, Obj> mp = d.trueValue();
        for (Obj key : mp.keySet())
            sb.append(toStr(key)).append(":").append(toStr(mp.get(key))).append(",");

        return "{" + sb.substring(0, sb.length() - 1) + "}";
    }

    public RTResult execute_dumps(Context execCtx) {
        Obj value = ((Obj) execCtx.symbolTable.get("value")).dictionary();
        if (value.jptype != Constants.JPType.Dict && value.jptype != Constants.JPType.List)
            return new RTResult().failure(RTError.Type(
                value.get_start(), value.get_end(),
                "Expected Dict or List",
                execCtx
        ));
        return new RTResult().success(new Str(toStr(value)));
    }

}
