package lemon.jpizza;

import java.util.HashMap;
import java.util.Map;

public class Tokens {
    public enum TT {
        EQS,
        INT,
        FLOAT,
        STRING,
        BOOL,
        PLUS,
        MINUS,
        MUL,
        DIV,
        LPAREN,
        RPAREN,
        EOF,
        NEWLINE,
        POWER,
        IDENTIFIER,
        KEYWORD,
        EQ,
        EE,
        NE,
        LT,
        GT,
        LTE,
        GTE,
        AND,
        OR,
        NOT,
        CLACCESS,
        MOD,
        QUERY,
        BITE,
        DEFAULTQUE,
        QUEBACK,
        LAMBDA,
        STEP,
        COMMA,
        LSQUARE,
        RSQUARE,
        OPEN,
        CLOSE,
        PLE,
        MIE,
        MUE,
        DIE,
        POE,
        INCR,
        DECR,
        DOT,
        USE,
        ITER,
    }
    
    public static final Map<String, TT> TOKEY = new HashMap<>(){{
        put("[", TT.LSQUARE);
        put("<-", TT.ITER);
        put("::", TT.CLACCESS);
        put("%", TT.MOD);
        put("#", TT.USE);
        put("]", TT.RSQUARE);
        put(",", TT.COMMA);
        put("+", TT.PLUS);
        put("++", TT.INCR);
        put("--", TT.DECR);
        put(">>", TT.STEP);
        put(":", TT.BITE);
        put("$", TT.QUEBACK);
        put("$_", TT.DEFAULTQUE);
        put("?", TT.QUERY);
        put("-", TT.MINUS);
        put("*", TT.MUL);
        put("/", TT.DIV);
        put("(", TT.LPAREN);
        put(")", TT.RPAREN);
        put("^", TT.POWER);
        put("=>", TT.EQ);
        put("&", TT.AND);
        put("|", TT.OR);
        put("->", TT.LAMBDA);
        put(";", TT.NEWLINE);
        put("{", TT.OPEN);
        put("}", TT.CLOSE);
        put("^=", TT.POE);
        put("*=", TT.MUE);
        put("/=", TT.DIE);
        put("+=", TT.PLE);
        put("-=", TT.MIE);
        put(".", TT.DOT);
    }};
}
