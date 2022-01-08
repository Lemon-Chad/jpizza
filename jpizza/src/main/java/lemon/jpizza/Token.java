package lemon.jpizza;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Token {
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
        switch (type) {
            case Type: return String.join("", (List<String>) value);
            case InvisibleNewline: return "";
            case LeftTildeArrow: return "<~";
            case TildeTilde: return "~~";
            case RightTildeArrow: return "~>";
            case Equal: return "=";
            case TildeAmpersand: return "~&";
            case TildePipe: return "~|";
            case TildeCaret: return "~^";
            case Tilde: return "~";
            case At: return "@";
            case Int:
            case Float:
            case Boolean: return String.valueOf(value);
            case String: return ((Pair<String, Boolean>) value).a;
            case Plus: return "+";
            case Minus: return "-";
            case Star: return "*";
            case Slash: return "/";
            case LeftParen: return "(";
            case RightParen: return ")";
            case EndOfFile: return "EOF";
            case Newline: return "\n";
            case Caret: return "^";
            case Identifier:
            case Keyword: return (String) value;
            case FatArrow: return "=>";
            case EqualEqual: return "==";
            case BangEqual: return "!=";
            case LeftAngle: return "<";
            case RightAngle: return ">";
            case LessEquals: return "<=";
            case GreaterEquals: return ">=";
            case Ampersand: return "&";
            case Pipe: return "|";
            case Bang: return "!";
            case ColonColon: return "::";
            case Percent: return "%";
            case QuestionMark: return "?";
            case Colon: return ":";
            case DollarUnderscore: return "$_";
            case DollarSign: return "$";
            case SkinnyArrow: return "->";
            case AngleAngle: return ">>";
            case Comma: return ",";
            case LeftBracket: return "[";
            case RightBracket: return "]";
            case LeftBrace: return "{";
            case RightBrace: return "}";
            case PlusEquals: return "+=";
            case MinusEquals: return "-=";
            case StarEquals: return "*=";
            case SlashEquals: return "/=";
            case CaretEquals: return "^=";
            case PlusPlus: return "++";
            case MinusMinus: return "--";
            case Dot: return ".";
            case Hash: return "#";
            case LeftArrow: return "<-";
            case Backslash: return "\\";
            case DotDot: return "..";
        }
        return "";
    }
}
