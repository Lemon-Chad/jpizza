package lemon.jpizza.libraries;

import lemon.jpizza.JPType;
import lemon.jpizza.TokenType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Lexer;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Shell;
import lemon.jpizza.Token;

import java.util.*;

@SuppressWarnings("unused")
public class JasonLib extends Library {

    static final List<TokenType> acceptableJason = Arrays.asList(
            TokenType.String,
            TokenType.Float,
            TokenType.Int,
            TokenType.LeftBracket,
            TokenType.RightBracket,
            TokenType.Comma,
            TokenType.Colon,
            TokenType.LeftBrace,
            TokenType.RightBrace,
            TokenType.EndOfFile,
            TokenType.Boolean
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
        if (value.jptype != JPType.String) return new RTResult().failure(RTError.Type(
                value.get_start(), value.get_end(),
                "Expected String",
                execCtx
        ));

        String jdata = value.string;
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

        return new RTResult().success(d.a.list.get(0));
    }

    public String toStr(Obj o) {
        if (o.jptype == JPType.String) return "\"" + o + "\"";
        else if (o.jptype == JPType.Dict) return visitDictionary(o);
        else if (o.jptype == JPType.List) return visitList(o);
        else return o.toString();
    }

    public String visitList(Obj l) {
        StringBuilder sb = new StringBuilder();

        List<Obj> lst = l.list;
        for (Obj item : lst)
            sb.append(toStr(item)).append(",");
        return "[" + sb.substring(0, sb.length() - 1) + "]";
    }

    public String visitDictionary(Obj d) {
        StringBuilder sb = new StringBuilder();

        Map<Obj, Obj> mp = d.map;
        for (Obj key : mp.keySet())
            sb.append(toStr(key)).append(":").append(toStr(mp.get(key))).append(",");

        return "{" + sb.substring(0, sb.length() - 1) + "}";
    }

    public RTResult execute_dumps(Context execCtx) {
        Obj value = ((Obj) execCtx.symbolTable.get("value")).dictionary();
        if (value.jptype != JPType.Dict && value.jptype != JPType.List)
            return new RTResult().failure(RTError.Type(
                value.get_start(), value.get_end(),
                "Expected Dict or List",
                execCtx
        ));
        return new RTResult().success(new Str(toStr(value)));
    }

}
