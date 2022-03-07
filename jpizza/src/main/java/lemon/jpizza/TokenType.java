package lemon.jpizza;

public enum TokenType {
    Type,
    InvisibleNewline,
    LeftTildeArrow,
    TildeTilde,
    RightTildeArrow,
    Equal,
    TildeAmpersand,
    TildePipe,
    TildeCaret,
    Tilde,
    At,
    Int,
    Float,
    String,
    Boolean,
    Plus,
    Minus,
    Star,
    Slash,
    LeftParen,
    RightParen,
    EndOfFile,
    Newline,
    Caret,
    Identifier,
    Keyword,
    FatArrow,
    EqualEqual,
    BangEqual,
    LeftAngle,
    RightAngle,
    LessEquals,
    GreaterEquals,
    Ampersand,
    Pipe,
    Bang,
    ColonColon,
    Percent,
    QuestionMark,
    Colon,
    DollarUnderscore,
    DollarSign,
    SkinnyArrow,
    AngleAngle,
    Comma,
    LeftBracket,
    RightBracket,
    LeftBrace,
    RightBrace,
    PlusEquals,
    MinusEquals,
    StarEquals,
    SlashEquals,
    CaretEquals,
    PlusPlus,
    MinusMinus,
    Dot,
    Hash,
    LeftArrow,
    Backslash,
    DotDot;

    public String toString() {
        switch (this) {
            case Type:
                return "type";
            case InvisibleNewline:
                return "invisible newline";
            case LeftTildeArrow:
                return "<~";
            case TildeTilde:
                return "~~";
            case RightTildeArrow:
                return "~>";
            case Equal:
                return "=";
            case TildeAmpersand:
                return "~&";
            case TildePipe:
                return "~|";
            case TildeCaret:
                return "~^";
            case Tilde:
                return "~";
            case At:
                return "@";
            case Int:
                return "int";
            case Float:
                return "float";
            case String:
                return "string";
            case Boolean:
                return "boolean";
            case Plus:
                return "+";
            case Minus:
                return "-";
            case Star:
                return "*";
            case Slash:
                return "/";
            case LeftParen:
                return "(";
            case RightParen:
                return ")";
            case EndOfFile:
                return "end of file";
            case Newline:
                return "newline";
            case Caret:
                return "^";
            case Identifier:
                return "identifier";
            case Keyword:
                return "keyword";
            case FatArrow:
                return "=>";
            case EqualEqual:
                return "==";
            case BangEqual:
                return "!=";
            case LeftAngle:
                return "<";
            case RightAngle:
                return ">";
            case LessEquals:
                return "<=";
            case GreaterEquals:
                return ">=";
            case Ampersand:
                return "&";
            case Pipe:
                return "|";
            case Bang:
                return "!";
            case ColonColon:
                return "::";
            case Percent:
                return "%";
            case QuestionMark:
                return "?";
            case Colon:
                return ":";
            case DollarUnderscore:
                return "$_";
            case DollarSign:
                return "$";
            case SkinnyArrow:
                return "->";
            case AngleAngle:
                return ">>";
            case Comma:
                return ",";
            case LeftBracket:
                return "[";
            case RightBracket:
                return "]";
            case LeftBrace:
                return "{";
            case RightBrace:
                return "}";
            case PlusEquals:
                return "+=";
            case MinusEquals:
                return "-=";
            case StarEquals:
                return "*=";
            case SlashEquals:
                return "/=";
            case CaretEquals:
                return "^=";
            case PlusPlus:
                return "++";
            case MinusMinus:
                return "--";
            case Dot:
                return ".";
            case Hash:
                return "#";
            case LeftArrow:
                return "<-";
            case Backslash:
                return "\\";
            case DotDot:
                return "..";
        }
        return "";
    }
}