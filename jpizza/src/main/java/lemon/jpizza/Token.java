package lemon.jpizza;

import java.io.Serializable;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class Token implements Serializable {
    public final TokenType type;
    public final Object value;
    public Position pos_start;
    public Position pos_end;

    public Token(TokenType type, Object value, @NotNull Position pos_start, @NotNull Position pos_end) {
        this.type = type;
        this.value = value;

        if (pos_start != null) {
            this.pos_start = pos_start.copy();
            this.pos_end = pos_end != null ? pos_end.copy() : pos_start.copy().advance();
        }
    }

    public Token(TokenType type, @NotNull Position start_pos) {
        this.type = type;
        this.value = null;

        this.pos_start = start_pos.copy();
        this.pos_end = start_pos.copy().advance();
    }

    public Token(TokenType type, @NotNull Position start_pos, @NotNull Position end_pos) {
        this.type = type;
        this.value = null;

        this.pos_start = start_pos.copy();
        this.pos_end = end_pos.copy();
    }

    public boolean matches(TokenType type, Object value) {
        return this.type.equals(type) && (this.value == null || this.value.equals(value));
    }

    public String toString() {
        return value != null ? String.format(
                "%s:%s",
                type, value
        ) : String.valueOf(type);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Token)) return false;

        Token other = (Token) o;
        if (value == null) return other.type == type && other.value == null;
        return other.type == type && value.equals(other.value);
    }

    public String asString() {
        return switch (type) {
            case Type -> String.join("", (List<String>) value);
            case InvisibleNewline -> "";
            case LeftTildeArrow -> "<~";
            case TildeTilde -> "~~";
            case RightTildeArrow -> "~>";
            case Equal -> "=";
            case TildeAmpersand -> "~&";
            case TildePipe -> "~|";
            case TildeCaret -> "~^";
            case Tilde -> "~";
            case At -> "@";
            case Int, Float, Boolean -> String.valueOf(value);
            case String -> ((Pair<String, Boolean>) value).a;
            case Plus -> "+";
            case Minus -> "-";
            case Star -> "*";
            case Slash -> "/";
            case LeftParen -> "(";
            case RightParen -> ")";
            case EndOfFile -> "EOF";
            case Newline -> "\n";
            case Caret -> "^";
            case Identifier, Keyword -> (String) value;
            case FatArrow -> "=>";
            case EqualEqual -> "==";
            case BangEqual -> "!=";
            case LeftAngle -> "<";
            case RightAngle -> ">";
            case LessEquals -> "<=";
            case GreaterEquals -> ">=";
            case Ampersand -> "&";
            case Pipe -> "|";
            case Bang -> "!";
            case ColonColon -> "::";
            case Percent -> "%";
            case QuestionMark -> "?";
            case Colon -> ":";
            case DollarUnderscore -> "$_";
            case DollarSign -> "$";
            case SkinnyArrow -> "->";
            case AngleAngle -> ">>";
            case Comma -> ",";
            case LeftBracket -> "[";
            case RightBracket -> "]";
            case LeftBrace -> "{";
            case RightBrace -> "}";
            case PlusEquals -> "+=";
            case MinusEquals -> "-=";
            case StarEquals -> "*=";
            case SlashEquals -> "/=";
            case CaretEquals -> "^=";
            case PlusPlus -> "++";
            case MinusMinus -> "--";
            case Dot -> ".";
            case Hash -> "#";
            case LeftArrow -> "<-";
            case Backslash -> "\\";
            case DotDot -> "..";
        };
    }
}
