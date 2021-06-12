package lemon.jpizza;

import java.util.HashMap;
import java.util.Map;

public class Tokens {
    public static final String TT_INT = "INT";
    public static final String TT_FLOAT = "FLOAT";
    public static final String TT_STRING = "STRING";
    public static final String TT_BOOL = "BOOL";
    public static final String TT_PLUS = "PLUS";
    public static final String TT_MINUS = "MINUS";
    public static final String TT_MUL = "MUL";
    public static final String TT_DIV = "DIV";
    public static final String TT_LPAREN = "LPAREN";
    public static final String TT_RPAREN = "RPAREN";
    public static final String TT_EOF = "EoF";
    public static final String TT_NEWLINE = "NEWLINE";
    public static final String TT_POWER = "POWER";
    public static final String TT_IDENTIFIER = "IDENTIFIER";
    public static final String TT_KEYWORD = "KEYWORD";
    public static final String TT_EQ = "EQ";
    public static final String TT_EE = "EE";
    public static final String TT_NE = "NE";
    public static final String TT_LT = "LT";
    public static final String TT_GT = "GT";
    public static final String TT_LTE = "LTE";
    public static final String TT_GTE = "GTE";
    public static final String TT_AND = "AND";
    public static final String TT_OR = "OR";
    public static final String TT_NOT = "NOT";
    public static final String TT_CLACCESS = "CLACCESS";
    public static final String TT_MOD = "MOD";
    public static final String TT_QUERY = "QUERY";
    public static final String TT_BITE = "BITE";
    public static final String TT_DEFAULTQUE = "DEFAULTQUE";
    public static final String TT_QUEBACK = "QUEBACK";
    public static final String TT_LAMBDA = "LAMBDA";
    public static final String TT_STEP = "STEP";
    public static final String TT_COMMA = "COMMA";
    public static final String TT_OPENSIGN = "OPENSIGN";
    public static final String TT_CLOSESIGN = "CLOSESIGN";
    public static final String TT_LSQUARE = "LSQUARE";
    public static final String TT_RSQUARE = "RSQUARE";
    public static final String TT_OPEN = "OPEN";
    public static final String TT_CLOSE = "CLOSE";
    public static final String TT_PLE = "PLE";
    public static final String TT_MIE = "MIE";
    public static final String TT_MUE = "MUE";
    public static final String TT_DIE = "DIE";
    public static final String TT_POE = "POE";
    public static final String TT_INCR = "INCR";
    public static final String TT_DECR = "DECR";
    public static final String TT_DICT = "DICT";
    public static final String TT_DOT = "DOT";

    public static final Map<String, String> TOKEY = new HashMap<>(){{
        put("[", TT_LSQUARE);
        put("::", TT_CLACCESS);
        put("%", TT_MOD);
        put("]", TT_RSQUARE);
        put(",", TT_COMMA);
        put("+", TT_PLUS);
        put("++", TT_INCR);
        put("--", TT_DECR);
        put(">>", TT_STEP);
        put(":", TT_BITE);
        put("$", TT_QUEBACK);
        put("$_", TT_DEFAULTQUE);
        put("?", TT_QUERY);
        put("-", TT_MINUS);
        put("*", TT_MUL);
        put("/", TT_DIV);
        put("(", TT_LPAREN);
        put(")", TT_RPAREN);
        put("^", TT_POWER);
        put("=>", TT_EQ);
        put("&", TT_AND);
        put("|", TT_OR);
        put("->", TT_LAMBDA);
        put(";", TT_NEWLINE);
        put("{", TT_OPEN);
        put("}", TT_CLOSE);
        put("^=", TT_POE);
        put("*=", TT_MUE);
        put("/=", TT_DIE);
        put("+=", TT_PLE);
        put("-=", TT_MIE);
        put(".", TT_DOT);
    }};
}
