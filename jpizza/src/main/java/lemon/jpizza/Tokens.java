package lemon.jpizza;

import java.util.HashMap;
import java.util.Map;

public class Tokens {
    public static final Map<String, TokenType> TOKEY = new HashMap<String, TokenType>(){{
        put("[", TokenType.LeftBracket);
        put("\\", TokenType.Backslash);
        put("..", TokenType.DotDot);
        put("~~", TokenType.TildeTilde);
        put("~>", TokenType.RightTildeArrow);
        put("<~", TokenType.LeftTildeArrow);
        put("@", TokenType.At);
        put("~&", TokenType.TildeAmpersand);
        put("~|", TokenType.TildePipe);
        put("~^", TokenType.TildeCaret);
        put("~", TokenType.Tilde);
        put("<-", TokenType.LeftArrow);
        put("::", TokenType.ColonColon);
        put("%", TokenType.Percent);
        put("#", TokenType.Hash);
        put("]", TokenType.RightBracket);
        put(",", TokenType.Comma);
        put("+", TokenType.Plus);
        put("++", TokenType.PlusPlus);
        put("--", TokenType.MinusMinus);
        put(">>", TokenType.AngleAngle);
        put(":", TokenType.Colon);
        put("$", TokenType.DollarSign);
        put("$_", TokenType.DollarUnderscore);
        put("?", TokenType.QuestionMark);
        put("-", TokenType.Minus);
        put("*", TokenType.Star);
        put("/", TokenType.Slash);
        put("(", TokenType.LeftParen);
        put(")", TokenType.RightParen);
        put("^", TokenType.Caret);
        put("=>", TokenType.FatArrow);
        put("&", TokenType.Ampersand);
        put("|", TokenType.Pipe);
        put("->", TokenType.SkinnyArrow);
        put(";", TokenType.Newline);
        put("{", TokenType.LeftBrace);
        put("}", TokenType.RightBrace);
        put("^=", TokenType.CaretEquals);
        put("*=", TokenType.StarEquals);
        put("/=", TokenType.SlashEquals);
        put("+=", TokenType.PlusEquals);
        put("-=", TokenType.MinusEquals);
        put(".", TokenType.Dot);
    }};
}
