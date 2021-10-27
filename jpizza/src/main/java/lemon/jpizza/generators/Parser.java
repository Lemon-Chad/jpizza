package lemon.jpizza.generators;

import lemon.jpizza.*;
import lemon.jpizza.cases.Case;
import lemon.jpizza.cases.ElseCase;
import lemon.jpizza.errors.Error;
import lemon.jpizza.errors.Tip;
import lemon.jpizza.nodes.definitions.*;
import lemon.jpizza.nodes.expressions.*;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.operations.BinOpNode;
import lemon.jpizza.nodes.operations.UnaryOpNode;
import lemon.jpizza.nodes.values.*;
import lemon.jpizza.nodes.variables.AttrAccessNode;
import lemon.jpizza.nodes.variables.VarAccessNode;
import lemon.jpizza.results.ParseResult;

import java.util.*;

import static lemon.jpizza.Tokens.*;

public class Parser {
    Token currentToken;
    final List<Token> tokens;
    int tokIdx = -1;
    int displayTokIdx = -1;
    int tokount;

    enum NamingConvention {
        CamelCase,
        ScreamingSnakeCase,
        PascalCase,
        SnakeCase,
        MixedSnakeCase,
        None,
        Mixed
    }

    static String stringConvention(NamingConvention convention) {
        return switch (convention) {
            case CamelCase -> "camelCase";
            case ScreamingSnakeCase -> "SCREAMING_SNAKE_CASE";
            case PascalCase -> "PascalCase";
            case SnakeCase -> "snake_case";
            case MixedSnakeCase -> "Mixed_Snake_Case";
            default -> "lowercase";
        };
    }

    static NamingConvention getConvention(String name) {
        int uppercase = 0;
        int lowercase = 0;

        for (char c: name.toCharArray()) {
            if (!Character.isLetter(c)) continue;
            
            if ('A' <= c && c <= 'Z')
                uppercase++;
            else
                lowercase++;
        }

        if (name.contains("_")) {
            if (uppercase == 0)
                return NamingConvention.SnakeCase;
            else if (lowercase == 0 && uppercase > 0)
                return NamingConvention.ScreamingSnakeCase;
            else
                return NamingConvention.MixedSnakeCase;
        }
        else if (lowercase == 0 && uppercase > 1)
            return NamingConvention.ScreamingSnakeCase;
        else if ('A' <= name.charAt(0) && name.charAt(0) <= 'Z')
            return NamingConvention.PascalCase;
        else if (uppercase > 0)
            return NamingConvention.CamelCase;

        return NamingConvention.None;
    }

    static boolean conventionMatches(NamingConvention a, NamingConvention b) {
        if (b == NamingConvention.ScreamingSnakeCase || b == NamingConvention.PascalCase)
            return a == b;
        if (a == NamingConvention.None || b == NamingConvention.None)
            return true;
        return a == b;
    }

    static final List<String> declKeywords = Arrays.asList(
            "static",
            "stc",
            "prv",
            "pub"
    );
    static final List<String> classWords = Arrays.asList(
            "class",
            "recipe",
            "obj"
    );
    static final List<String> methKeywords = Arrays.asList(
            "method",
            "mthd",
            "md"
    );
    static final List<String> constWords = Arrays.asList(
            "bake",
            "const"
    );
    static final List<String> varWords = Arrays.asList(
            "bake",
            "const",
            "var"
    );

    public interface L {
        ParseResult execute();
    }

    public Parser(List<Token> Tokens) {
        tokens = Tokens;
        tokount = Tokens.size();
        advance();
    }

    public void advance() {
        tokIdx++;
        if (currentToken != null && currentToken.type != TT.INVISILINE)
            displayTokIdx++;
        updateTok();
    }

    public void updateTok() {
        if (0 <= tokIdx && tokIdx < tokount)
            currentToken = tokens.get(tokIdx);
    }

    public Token peek(int i) {
        return 0 <= tokIdx + i && tokIdx + i < tokount ? tokens.get(tokIdx + i) : null;
    }

    public void reverse(int amount) {
        tokIdx -= amount;
        updateTok();
    }
    public void reverse() {
        reverse(1);
    }

    public ParseResult parse() {
        ParseResult res = statements(TT.EOF);
        if (res.error == null && !currentToken.type.equals(TT.EOF)) {
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '+', '-', '*', '^', or '/'"
            ));
        } return res;
    }

    public ParseResult statements(TT end) { 
        return statements(Collections.singletonList(new Token(end))); 
    }

    public ParseResult statements(List<Token> tks) {
        ParseResult res = new ParseResult();
        List<Node> statements = new ArrayList<>();
        Position pos_start = currentToken.pos_start.copy();

        int newlineCount;
        while (currentToken.type.equals(TT.NEWLINE) || currentToken.type.equals(TT.INVISILINE)) {
            res.registerAdvancement();
            advance();
        }
        Node statement = (Node) res.register(this.statement());
        if (res.error != null)
            return res;
        statements.add(statement);

        boolean moreStatements = true;

        while (true) {
            newlineCount = 0;
            while (currentToken.type.equals(TT.NEWLINE) || currentToken.type.equals(TT.INVISILINE)) {
                res.registerAdvancement();
                advance();
                newlineCount++;
            }
            if (newlineCount == 0) {
                moreStatements = false;
            }
            
            if (!moreStatements || tks.contains(currentToken))
                break;
            statement = (Node) res.try_register(this.statement());
            if (statement == null) {
                reverse(res.toReverseCount);
                moreStatements = false;
                continue;
            }
            statements.add(statement);
        }
        reverse();

        if (!currentToken.type.equals(TT.NEWLINE) && !currentToken.type.equals(TT.INVISILINE)) {
            Node prevStatement = statements.get(statements.size() - 1);
            statements.set(statements.size() - 1, new ReturnNode(
                    prevStatement,
                    prevStatement.pos_start,
                    prevStatement.pos_end,
                    true
            ));
        } advance();
        return res.success(new ListNode(
                statements,
                pos_start,
                currentToken.pos_end.copy()
        ));
    }

    private String tokenFound() {
        if (currentToken.value != null)
            return String.valueOf(currentToken.value);
        return String.valueOf(currentToken.type);
    }

    public ParseResult statement() {
        ParseResult res = new ParseResult();
        Position pos_start = currentToken.pos_start.copy();

        if (currentToken.matches(TT.KEYWORD, "return")) {
            res.registerAdvancement();
            advance();
            Node expr = (Node) res.try_register(this.expr());
            if (expr == null)
                reverse(res.toReverseCount);
            return res.success(new ReturnNode(expr, pos_start, currentToken.pos_end.copy()));
        }
        else if (currentToken.matches(TT.KEYWORD, "continue")) {
            res.registerAdvancement();
            advance();
            return res.success(new ContinueNode(pos_start, currentToken.pos_end.copy()));
        }
        else if (currentToken.matches(TT.KEYWORD, "break")) {
            res.registerAdvancement();
            advance();
            return res.success(new BreakNode(pos_start, currentToken.pos_end.copy()));
        }
        else if (currentToken.matches(TT.KEYWORD, "pass")) {
            res.registerAdvancement();
            advance();
            return res.success(new PassNode(pos_start, currentToken.pos_end.copy()));
        }

        Node expr = (Node) res.register(this.expr());
        if (res.error != null)
            return res;
        return res.success(expr);
    }

    public ParseResult extractVarTok() { return extractVarTok(false); }

    public ParseResult extractVarTok(boolean screaming) {
        ParseResult res = new ParseResult();

        if (!currentToken.type.equals(TT.IDENTIFIER))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected identifier"
            ));
        matchConvention(currentToken, "Variable name", screaming ? NamingConvention.ScreamingSnakeCase
                                                                       : NamingConvention.CamelCase);

        Token var_name = currentToken;
        res.registerAdvancement();
        advance();
        return res.success(var_name);
    }

    public ParseResult chainExpr() { return binOp(this::compExpr, Collections.singletonList(TT.BITE)); }

    public ParseResult buildTypeTok() {
        List<String> type = new ArrayList<>();
        ParseResult res = new ParseResult();

        if (!Constants.TYPETOKS.contains(currentToken.type)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start, currentToken.pos_end,
                "Expected type"
        ));

        while (Constants.TYPETOKS.contains(currentToken.type)) {
            type.add(switch (currentToken.type) {
                case LSQUARE -> "[";
                case RSQUARE -> "]";
                case LPAREN -> "(";
                case RPAREN -> ")";
                case OPEN -> "{";
                case CLOSE -> "}";
                case LT -> "<";
                case GT -> ">";
                default -> currentToken.value.toString();
            });
            res.registerAdvancement(); advance();
        }

        return res.success(new Token(TT.TYPE, type));
    }

    public ParseResult expr() {
        ParseResult res = new ParseResult();
        List<String> type = Collections.singletonList("any");
        if (currentToken.matches(TT.KEYWORD, "attr")) {
            res.registerAdvancement(); advance();
            Token var_name = (Token) res.register(extractVarTok());
            if (res.error != null) return res;

            if (currentToken.type.equals(TT.EQS)) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Should be '=>'"
            ));
            if (!currentToken.type.equals(TT.EQ))
                return res.success(new AttrAccessNode(var_name));

            res.registerAdvancement();
            advance();

            Node expr = (Node) res.register(this.expr());
            if (res.error != null)
                return res;
            return res.success(new AttrAssignNode(var_name, expr));
        }
        else if (currentToken.type == TT.KEYWORD && varWords.contains(currentToken.value.toString())) {
            boolean locked = constWords.contains(currentToken.value.toString());

            res.registerAdvancement(); advance();
            if (currentToken.type == TT.OPEN) {
                // Destructure
                boolean glob = false;
                List<Token> destructs = new ArrayList<>();

                res.registerAdvancement(); advance();

                if (currentToken.type == TT.MUL) {
                    glob = true;
                    res.registerAdvancement(); advance();
                }
                else do {
                    if (currentToken.type != TT.IDENTIFIER) return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "Expected identifier"
                    ));

                    matchConvention(currentToken, "Variable name", NamingConvention.CamelCase);

                    destructs.add(currentToken);
                    res.registerAdvancement();
                    advance();
                } while (currentToken.type != TT.CLOSE);

                if (currentToken.type != TT.CLOSE) return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '}'"
                ));
                res.registerAdvancement(); advance();

                if (currentToken.type != TT.EQ) return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '=>'"
                ));
                res.registerAdvancement(); advance();

                Node destructed = (Node) res.register(expr());
                if (res.error != null) return res;

                if (glob)
                    return res.success(new DestructNode(destructed));
                else
                    return res.success(new DestructNode(destructed, destructs));
            }
            Token var_name = (Token) res.register(extractVarTok(locked));
            if (res.error != null) return res;

            Integer min = null;
            Integer max = null;

            if (currentToken.type == TT.COMMA) {
                Node nll = new NullNode(new Token(
                        TT.IDENTIFIER,
                        "null",
                        currentToken.pos_start.copy(),
                        currentToken.pos_end.copy()
                ));
                List<Node> varNames = new ArrayList<>(Collections.singletonList(new VarAssignNode(var_name, nll).setType(type)));
                do {
                    var_name = (Token) res.register(expectIdentifier("Variable name", NamingConvention.CamelCase));
                    if (res.error != null) return res;
                    varNames.add(new VarAssignNode(var_name, nll).setType(type));
                    res.registerAdvancement(); advance();
                } while (currentToken.type == TT.COMMA);
                return res.success(new ListNode(varNames, varNames.get(0).pos_start,
                        varNames.get(varNames.size() - 1).pos_end));
            }

            if (currentToken.type == TT.LSQUARE) {
                res.registerAdvancement(); advance();
                boolean neg = false;
                if (currentToken.type == TT.MINUS) {
                    neg = true;
                    res.registerAdvancement(); advance();
                }
                if (currentToken.type != TT.INT) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start, currentToken.pos_end,
                        "Expected integer"
                ));
                min = 0;
                max = ((Double) currentToken.value).intValue() * (neg ? -1 : 1);
                res.registerAdvancement(); advance();
                if (currentToken.type == TT.OR) {
                    res.registerAdvancement(); advance();
                    neg = false;
                    if (currentToken.type == TT.MINUS) {
                        neg = true;
                        res.registerAdvancement(); advance();
                    }
                    if (currentToken.type != TT.INT) return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start, currentToken.pos_end,
                            "Expected integer"
                    ));
                    min = max;
                    max = ((Double) currentToken.value).intValue() * (neg ? -1 : 1);
                    res.registerAdvancement(); advance();
                }
                if (currentToken.type != TT.RSQUARE) return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start, currentToken.pos_end,
                        "Expected ']'"
                ));
                res.registerAdvancement(); advance();
            }

            if (currentToken.type.equals(TT.USE)) {
                advance(); res.registerAdvancement();
                Token typeTok = (Token) res.register(buildTypeTok());
                if (res.error != null) return res;
                type = (List<String>) typeTok.value;
            }

            if (currentToken.type.equals(TT.EQS)) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Should be '=>'"
            ));
            if (!currentToken.type.equals(TT.EQ))
                return res.success(new VarAssignNode(
                        var_name, new NullNode(new Token(
                                        TT.IDENTIFIER,
                                        "null",
                                        currentToken.pos_start.copy(),
                                        currentToken.pos_end.copy()
                                ))
                ).setType(type));

            res.registerAdvancement();
            advance();
            Node expr = (Node) res.register(this.expr());
            if (res.error != null)
                return res;
            return res.success(new VarAssignNode(var_name, expr, locked)
                    .setType(type)
                    .setRange(min, max));
        }
        else if (currentToken.matches(TT.KEYWORD, "let")) {
            Token ident = (Token) res.register(expectIdentifier("Variable name", NamingConvention.CamelCase));
            if (res.error != null) return res;
            res.registerAdvancement(); advance();
            if (currentToken.type != TT.EQ) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start, currentToken.pos_end,
                    "Expected '=>'"
            ));
            res.registerAdvancement(); advance();

            Node expr = (Node) res.register(this.expr());
            if (res.error != null) return res;

            return res.success(new LetNode(ident, expr));
        }
        else if (currentToken.matches(TT.KEYWORD, "cal")) {
            res.registerAdvancement(); advance();
            Token var_name = (Token) res.register(extractVarTok());
            if (res.error != null) return res;

            if (!currentToken.type.equals(TT.LAMBDA))
                return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected weak assignment arrow (->)"
                ));

            res.registerAdvancement();
            advance();
            Node expr = (Node) res.register(this.expr());
            if (res.error != null)
                return res;
            return res.success(new DynAssignNode(var_name, expr));
        }
        else if (currentToken.type.equals(TT.IDENTIFIER)) {
            Token var_tok = currentToken;
            advance();
            res.registerAdvancement();
            if (Arrays.asList(TT.POE, TT.PLE, TT.MUE, TT.DIE, TT.MIE).contains(currentToken.type)) {
                Token op_tok = currentToken;
                advance();
                res.registerAdvancement();
                Node value = (Node) res.register(this.expr());
                if (res.error != null)
                    return res;
                return res.success(new VarAssignNode(var_tok, new BinOpNode(
                        new VarAccessNode(var_tok),
                        new Token(switch (op_tok.type) {
                                    case POE -> TT.POWER;
                                    case MUE -> TT.MUL;
                                    case DIE -> TT.DIV;
                                    case PLE -> TT.PLUS;
                                    case MIE -> TT.MINUS;
                                    default -> null;
                                }, op_tok.pos_start.copy(), op_tok.pos_end
                        ), value
                ), false).setDefining(false));
            }
            if (currentToken.type.equals(TT.INCR) || currentToken.type.equals(TT.DECR)) {
                Token op_tok = currentToken;
                res.registerAdvancement();
                advance();
                return res.success(new VarAssignNode(var_tok, new UnaryOpNode(
                        op_tok,
                        new VarAccessNode(var_tok)
                ), false).setDefining(false));
            } reverse();
        }
        Node node = (Node) res.register(binOp(this::getExpr, Collections.singletonList(TT.DOT)));

        if (res.error != null)
            return res;

        return res.success(node);
    }

    public ParseResult factor() {
        ParseResult res = new ParseResult();
        Token tok = currentToken;

        if (Arrays.asList(TT.PLUS, TT.MINUS, TT.INCR, TT.DECR).contains(tok.type)) {
            res.registerAdvancement();
            advance();
            Node factor = (Node) res.register(this.factor());
            if (res.error != null)
                return res;
            return res.success(new UnaryOpNode(tok, factor));
        } return pow();
    }

    public ParseResult expectIdentifier() { 
        return expectIdentifier("Identifier", NamingConvention.None); 
    }

    static void matchConvention(Token tok, String name, NamingConvention convention) {
        NamingConvention tokenCase = getConvention(tok.value.toString());
        if (!conventionMatches(tokenCase, convention))
            if (tokenCase != convention) {
                Shell.logger.tip(new Tip(
                    tok.pos_start, tok.pos_end,
                    String.format("%s should have naming convention %s, not %s.",
                                  name, stringConvention(convention), stringConvention(tokenCase)),
                    """
camelCaseLooksLikeThis
snake_case_looks_like_this
SCREAMING_SNAKE_CASE_LOOKS_LIKE_THIS
PascalCaseLooksLikeThis
Mixed_Snake_Case_Looks_Like_This"""
                ).asString());
            }
    }

    public ParseResult expectIdentifier(String name, NamingConvention convention) {
        ParseResult res = new ParseResult();
        advance(); res.registerAdvancement();
        if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                String.format("Expected %s", name.toLowerCase())
        )); 
        
        matchConvention(currentToken, name, convention);
        
        return res.success(currentToken);
    }

    public ParseResult useExpr() {
        ParseResult res = new ParseResult();
        Token useToken = (Token) res.register(expectIdentifier());
        if (res.error != null) return res;
        advance(); res.registerAdvancement();
        List<Token> args = new ArrayList<>();
        while (currentToken.type.equals(TT.IDENTIFIER)) {
            args.add(currentToken);
            res.registerAdvancement(); advance();
        }
        return res.success(new UseNode(useToken, args));
    }

    public ParseResult btshftExpr() {
        return binOp(this::btwsExpr, Arrays.asList(TT.LEFTSHIFT, TT.RIGHTSHIFT, TT.SIGNRIGHTSHIFT), this::expr);
    }

    public ParseResult btwsExpr() {
        return binOp(this::complExpr, Arrays.asList(TT.BITAND, TT.BITOR, TT.BITXOR), this::expr);
    }

    public ParseResult complExpr() {
        ParseResult res = new ParseResult();
        if (currentToken.type == TT.BITCOMPL || currentToken.type == TT.QUEBACK) {
            Token opToken = currentToken;
            res.registerAdvancement();
            advance();

            Node expr = (Node) res.register(this.expr());
            if (res.error != null) return res;

            return res.success(new UnaryOpNode(opToken, expr));
        } return byteExpr();
    }

    public ParseResult byteExpr() {
        ParseResult res = new ParseResult();
        boolean toBytes = currentToken.type == TT.TOBYTE;
        if (toBytes) {
            res.registerAdvancement();
            advance();
        }

        Node expr = (Node) res.register(this.chainExpr());
        if (res.error != null) return res;

        if (toBytes)
            return res.success(new BytesNode(expr));
        return res.success(expr);
    }

    public ParseResult atom() {
        ParseResult res = new ParseResult();
        Token tok = currentToken;

        if (tok.type.equals(TT.KEYWORD))
            switch ((String) tok.value) {
                case "assert":
                    res.registerAdvancement(); advance();
                    Node condition = (Node) res.register(expr());
                    if (res.error != null) return res;
                    return res.success(new AssertNode(condition));

                case "free":
                    Token varTok = (Token) res.register(expectIdentifier());
                    if (res.error != null) return res;
                    res.registerAdvancement(); advance();
                    return res.success(new DropNode(varTok));

                case "throw":
                    Node throwNode = (Node) res.register(this.throwExpr());
                    if (res.error != null) return res;
                    return res.success(throwNode);

                case "class":
                case "obj":
                case "recipe":
                    Node classDef = (Node) res.register(this.classDef());
                    if (res.error != null)
                        return res;
                    return res.success(classDef);

                case "if":
                    Node ifExpr = (Node) res.register(this.ifExpr());
                    if (res.error != null)
                        return res;
                    return res.success(ifExpr);

                case "enum":
                    Node enumExpr = (Node) res.register(this.enumExpr());
                    if (res.error != null)
                         return res;
                    return res.success(enumExpr);

                case "switch":
                    Node switchExpr = (Node) res.register(this.switchExpr());
                    if (res.error != null)
                        return res;
                    return res.success(switchExpr);

                case "match":
                    Node matchExpr = (Node) res.register(this.matchExpr());
                    if (res.error != null)
                        return res;
                    return res.success(matchExpr);

                case "null":
                    res.registerAdvancement(); advance();
                    return res.success(new NullNode(tok));

                case "for":
                    Node forExpr = (Node) res.register(this.forExpr());
                    if (res.error != null)
                        return res;
                    return res.success(forExpr);

                case "function":
                case "yourmom":
                case "fn":
                    Node funcDef = (Node) res.register(this.funcDef());
                    if (res.error != null)
                        return res;
                    return res.success(funcDef);

                case "struct":
                    Node structDef = (Node) res.register(this.structDef());
                    if (res.error != null)
                        return res;
                    return res.success(structDef);

                case "loop":
                case "while":
                    Node whileExpr = (Node) res.register(this.whileExpr());
                    if (res.error != null)
                        return res;
                    return res.success(whileExpr);

                case "do":
                    Node doExpr = (Node) res.register(this.doExpr());
                    if (res.error != null)
                        return res;
                    return res.success(doExpr);

                case "scope":
                    res.registerAdvancement(); advance();
                    String name = null;
                    if (currentToken.type == TT.LSQUARE) {
                        Token n = (Token) res.register(expectIdentifier("Scope", NamingConvention.SnakeCase));
                        if (res.error != null) return res;

                        name = n.value.toString();

                        res.registerAdvancement(); advance();
                        if (currentToken.type != TT.RSQUARE) return res.failure(Error.ExpectedCharError(
                                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                                "Expected ']'"
                        ));
                        res.registerAdvancement(); advance();
                    }
                    Node statements = (Node) res.register(block());
                    if (res.error != null) return res;
                    return res.success(new ScopeNode(name, statements));

                case "import":
                    advance();
                    res.registerAdvancement();
                    Token file_name_tok = currentToken;
                    if (!file_name_tok.type.equals(TT.IDENTIFIER))
                        return res.failure(Error.InvalidSyntax(
                                file_name_tok.pos_start.copy(), file_name_tok.pos_end.copy(),
                                "Expected module name"
                        ));
                    
                    matchConvention(file_name_tok, "Module name", NamingConvention.SnakeCase);

                    advance(); res.registerAdvancement();
                    if (currentToken.matches(TT.KEYWORD, "as")) {
                        Token ident = (Token) res.register(expectIdentifier("Module name", NamingConvention.SnakeCase));
                        if (res.error != null)
                            return res;
                        res.registerAdvancement(); advance();
                        return res.success(new ImportNode(file_name_tok, ident));
                    }
                    return res.success(new ImportNode(file_name_tok));

                case "extend":
                    advance();
                    res.registerAdvancement();
                    Token fileNameTok = currentToken;
                    if (!fileNameTok.type.equals(TT.IDENTIFIER))
                        return res.failure(Error.InvalidSyntax(
                                fileNameTok.pos_start.copy(), fileNameTok.pos_end.copy(),
                                "Expected module name"
                        ));

                    matchConvention(fileNameTok, "Module name", NamingConvention.SnakeCase);

                    advance(); res.registerAdvancement();
                    return res.success(new ExtendNode(fileNameTok));

                case "attr":
                    advance(); res.registerAdvancement();
                    Token var_name_tok = currentToken;
                    if (!currentToken.type.equals(TT.IDENTIFIER))
                        return res.failure(Error.InvalidSyntax(
                                var_name_tok.pos_start.copy(), var_name_tok.pos_end.copy(),
                                "Expected identifier"
                        ));
                    advance(); res.registerAdvancement();
                    return res.success(new AttrAccessNode(var_name_tok));

                default:
                    break;
            }
        else if (Arrays.asList(TT.INT, TT.FLOAT).contains(tok.type)) {
            res.registerAdvancement(); advance();
            if (currentToken.type == TT.IDENTIFIER) {
                if (currentToken.value.toString().startsWith("x") && tok.value.equals(0.0) && tok.type.equals(TT.INT)) {
                    try {
                        Token hexTk = currentToken;
                        res.registerAdvancement(); advance();
                        int hexForm = Integer.parseInt(hexTk.value.toString().substring(1), 16);
                        return res.success(new NumberNode(hexForm, hexTk.pos_start, hexTk.pos_end));
                    } catch (NumberFormatException ignored) {}
                }
                Node identifier = new VarAccessNode(currentToken);
                res.registerAdvancement(); advance();
                return res.success(new BinOpNode(
                        new NumberNode(tok),
                        new Token(TT.MUL, tok.pos_start, identifier.pos_end),
                        identifier
                ));
            }
            return res.success(new NumberNode(tok));
        }
        else if (tok.type.equals(TT.USE)) {
            Node useExpr = (Node) res.register(this.useExpr());
            if (res.error != null) return res;
            return res.success(useExpr);
        }
        else if (tok.type.equals(TT.STRING)) {
            if (((Pair<String, Boolean>) tok.value).b) {
                Node val = (Node) res.register(formatStringExpr());
                if (res.error != null) return res;
                return res.success(val);
            }
            res.registerAdvancement(); advance();
            return res.success(new StringNode(tok));
        }
        else if (tok.type.equals(TT.IDENTIFIER)) {
            res.registerAdvancement(); advance();
            if (currentToken.type.equals(TT.EQS)) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Should be '=>'"
            ));
            if (currentToken.type.equals(TT.EQ)) {
                res.registerAdvancement(); advance();
                Node value = (Node) res.register(expr());
                if (res.error != null) return res;
                return res.success(new VarAssignNode(tok, value, false, 1));
            }
            return res.success(new VarAccessNode(tok));
        }
        else if (tok.type.equals(TT.BOOL)) {
            res.registerAdvancement(); advance();
            return res.success(new BooleanNode(tok));
        }
        else if (tok.type.equals(TT.QUERY)) {
            Node queryExpr = (Node) res.register(this.queryExpr());
            if (res.error != null)
                return res;
            return res.success(queryExpr);
        }
        else if (tok.type.equals(TT.LSQUARE)) {
            Node listExpr = (Node) res.register(this.listExpr());
            if (res.error != null)
                return res;
            return res.success(listExpr);
        }
        else if (tok.type.equals(TT.OPEN)) {
            Node dictExpr = (Node) res.register(this.dictExpr());
            if (res.error != null)
                return res;
            return res.success(dictExpr);
        }
        else if (tok.type.equals(TT.LPAREN)) {
            res.registerAdvancement(); advance();
            Node expr = (Node) res.register(this.expr());
            if (res.error != null)
                return res;
            if (currentToken.type.equals(TT.RPAREN)) {
                res.registerAdvancement(); advance();
                return res.success(expr);
            } else
                return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected ')'"
                ));
        }
        return res.failure(Error.InvalidSyntax(
                tok.pos_start.copy(), tok.pos_end != null ? tok.pos_end.copy() : tok.pos_start.copy(),
                String.format("Expected long, double, identifier, '+', '-', or '('. Found %s", tokenFound())
        ));
    }

    public ParseResult refExpr() {
        Token prefixToken;
        if (currentToken.type == TT.MUL || currentToken.type == TT.AND) {
            ParseResult res = new ParseResult();

            prefixToken = currentToken;
            res.registerAdvancement(); advance();

            Node expr = (Node) res.register(refExpr());
            if (res.error != null) return res;
            
            if (prefixToken.type == TT.MUL) return res.success(new DerefNode(expr));
            else return res.success(new RefNode(expr));
        }
    
        return index();
    }

    public ParseResult formatStringExpr() {
        ParseResult res = new ParseResult();
        Token tok = currentToken;
        StringBuilder sb = new StringBuilder();
        Pair<String, Boolean> val = (Pair<String, Boolean>) tok.value;

        Token addToken = new Token(TT.PLUS, tok.pos_start, tok.pos_end);
        Node node = new StringNode(new Token(TT.STRING, new Pair<>("", false), tok.pos_start, tok.pos_end));
        for (int i = 0; i < val.a.length(); i++) {
            char current = val.a.charAt(i);
            char next = i + 1 < val.a.length() ? val.a.charAt(i + 1) : ' ';
            if (current == '!' && next == '$') {
                sb.append("$");
                i++;
            } else if (current == '$' && next == '{') {
                node = new BinOpNode(node, addToken,
                        new StringNode(new Token(TT.STRING, new Pair<>(sb.toString(), false),
                                tok.pos_start, tok.pos_end)));
                sb = new StringBuilder();
                StringBuilder expr = new StringBuilder();
                i += 2;
                while (i < val.a.length() && val.a.charAt(i) != '}') {
                    current = val.a.charAt(i);
                    expr.append(current);
                    i++;
                }

                if (i >= val.a.length()) return res.failure(Error.InvalidSyntax(
                        tok.pos_start, tok.pos_end,
                        "Unmatched bracket"
                ));

                Pair<List<Token>, Error> ts = new Lexer("<fstring>", expr.toString()).make_tokens();
                if (ts.b != null) return res.failure(ts.b);
                Node r = (Node) res.register(new Parser(ts.a).statement());
                if (res.error != null) return res;
                node = new BinOpNode(node, addToken, r);
            } else {
                sb.append(current);
            }
        }

        res.registerAdvancement(); advance();
        return res.success(new BinOpNode(node, addToken,
                new StringNode(new Token(TT.STRING, new Pair<>(sb.toString(), false), tok.pos_start, tok.pos_end))));
    }

    public ParseResult throwExpr() {
        ParseResult res = new ParseResult();
        if (!currentToken.matches(TT.KEYWORD, "throw")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'throw'"
        )); res.registerAdvancement(); advance();

        Node first = (Node) res.register(this.expr());
        if (res.error != null)
            return res;

        if (currentToken.type == TT.COMMA) {
            res.registerAdvancement(); advance();

            Node second = (Node) res.register(this.expr());
            if (res.error != null)
                return res;

            return res.success(new ThrowNode(first, second));
        }

        return res.success(new ThrowNode(new StringNode(new Token(TT.STRING, new Pair<>("Thrown", false),
                first.pos_start, first.pos_end)), first));
    }

    public ParseResult structDef() {
        ParseResult res = new ParseResult();
        if (!currentToken.matches(TT.KEYWORD, "struct")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'struct'"
        ));

        List<AttrDeclareNode> childrenDecls = new ArrayList<>();
        List<Token> children = new ArrayList<>();
        List<Token> types = new ArrayList<>();
        List<Node> assignment = new ArrayList<>();

        Token identifier = (Token) res.register(expectIdentifier("Struct", NamingConvention.PascalCase));
        if (res.error != null) return res;
        res.registerAdvancement(); advance();

        if (currentToken.type != TT.OPEN) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '{'"
        ));

        Position start = currentToken.pos_start.copy();

        do {
            res.register(expectIdentifier("Attribute name", NamingConvention.CamelCase));
            if (res.error != null) return res;

            children.add(currentToken);
            childrenDecls.add(new AttrDeclareNode(currentToken));
            types.add(new Token(TT.TYPE, Collections.singletonList("any"),
                    currentToken.pos_start, currentToken.pos_end));
            assignment.add(new AttrAssignNode(
                    currentToken,
                    new VarAccessNode(currentToken)
            ));

            res.registerAdvancement(); advance();
        } while (currentToken.type == TT.COMMA);

        Position end = currentToken.pos_end.copy();

        if (currentToken.type != TT.CLOSE) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '}'"
        ));
        endLine(1);
        res.registerAdvancement(); advance();

        return res.success(new ClassDefNode(
                identifier,
                childrenDecls,
                children,
                types,
                new ListNode(assignment, start, end),
                new ArrayList<>(),
                end,
                new ArrayList<>(),
                0,
                null,
                new ArrayList<>(),
                null,
                null
        ));

    }

    public static class EnumChild {
        public final List<String> params;
        public final List<List<String>> types;
        public final List<String> generics;
        public final Token token;
        public EnumChild(Token token, List<String> params, List<List<String>> types, List<String> generics) {
            this.params = params;
            this.types = types;
            this.generics = generics;
            this.token = token;
        }
    }

    public ParseResult enumExpr() {
        ParseResult res = new ParseResult();

        List<EnumChild> children = new ArrayList<>();
        Token name;

        if (!currentToken.matches(TT.KEYWORD, "enum")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'enum'"
        ));
        res.registerAdvancement(); advance();

        boolean pub;
        if (pub = currentToken.matches(TT.KEYWORD, "pub")) {
            res.registerAdvancement(); advance();
        }

        if (currentToken.type != TT.IDENTIFIER) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected identifier"
        ));

        matchConvention(currentToken, "Enum", NamingConvention.PascalCase);

        name = currentToken;
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.OPEN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '{'"
            ));
        res.registerAdvancement(); advance();

        while (currentToken.type == TT.IDENTIFIER) {
            Token token = currentToken;
            matchConvention(token, "Enum child", NamingConvention.PascalCase);
            res.registerAdvancement(); advance();

            List<String> generics = new ArrayList<>();
            if (currentToken.type == TT.LPAREN) {
                do {
                    Token ident = (Token) res.register(expectIdentifier("Generic type", NamingConvention.PascalCase));
                    if (res.error != null) return res;
                    res.registerAdvancement(); advance();
                    generics.add(ident.value.toString());
                } while (currentToken.type == TT.COMMA);

                if (currentToken.type != TT.RPAREN) return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected ')'"
                ));
                res.registerAdvancement(); advance();
            }

            List<String> params = new ArrayList<>();
            List<List<String>> types = new ArrayList<>();
            if (currentToken.type == TT.OPEN) {
                do {
                    Token tok = (Token) res.register(expectIdentifier("Parameter", NamingConvention.CamelCase));
                    if (res.error != null) return res;
                    params.add((String) tok.value);
                    res.registerAdvancement(); advance();

                    if (currentToken.type == TT.BITE) {
                        res.registerAdvancement(); advance();
                        tok = (Token) res.register(buildTypeTok());
                        if (res.error != null) return res;
                        types.add((List<String>) tok.value);
                    } else {
                        types.add(Collections.singletonList("any"));
                    }

                } while (currentToken.type == TT.COMMA);
                if (currentToken.type != TT.CLOSE) return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '}'"
                ));
                res.registerAdvancement(); advance();
            }

            if (currentToken.type != TT.COMMA) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected comma"
            ));
            res.registerAdvancement(); advance();
            children.add(new EnumChild(token, params, types, generics));
        }

        if (!currentToken.type.equals(TT.CLOSE))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '}'"
            ));
        endLine(1);
        res.registerAdvancement(); advance();

        return res.success(new EnumNode(name, children, pub));
    }

    public ParseResult switchExpr() {
        ParseResult res = new ParseResult();

        ElseCase elseCase = null;
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TT.KEYWORD, "switch")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected switch"
            ));
        res.registerAdvancement(); advance();

        Node ref;
        if (!currentToken.type.equals(TT.LPAREN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '('"
            ));
        res.registerAdvancement(); advance();
        ref = (Node) res.register(expr());
        if (res.error != null) return res;
        if (!currentToken.type.equals(TT.RPAREN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ')'"
            ));
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.OPEN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '{'"
            ));
        res.registerAdvancement(); advance();

        boolean def;
        Node condition, body;
        while (currentToken.matches(TT.KEYWORD, "case") || currentToken.matches(TT.KEYWORD, "default")) {
            def = currentToken.matches(TT.KEYWORD, "default");
            res.registerAdvancement(); advance();

            if (!def) {
                condition = (Node) res.register(compExpr());
                if (res.error != null) return res;
            } else condition = null;

            if (currentToken.type != TT.BITE) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ':'"
            ));
            res.registerAdvancement(); advance();

            body = (Node) res.register(statements(Arrays.asList(
                new Token(TT.CLOSE),
                new Token(TT.KEYWORD, "case"),
                new Token(TT.KEYWORD, "default")
            )));
            if (res.error != null) return res;

            if (def)
                elseCase = new ElseCase(body, true);
            else
                cases.add(new Case(condition, body, true));
        }

        if (!currentToken.type.equals(TT.CLOSE))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '}'"
            ));
        endLine(1);
        res.registerAdvancement(); advance();

        Node swtch = new SwitchNode(ref, cases, elseCase, false);

        if (cases.size() < 3) Shell.logger.tip(new Tip(
            swtch.pos_start, swtch.pos_end,
            "Switch can be replaced with if-elif-else structure", """
if (x == 1) {
    println("X is 1!");
} elif (x == 2) {
    println("X is two.");
} else {
    println("X is dumb! >:(");
}"""
        ).asString());

        return res.success(swtch);
    }

    public ParseResult expectSemicolon() {
        if (currentToken.type != TT.INVISILINE && currentToken.type != TT.NEWLINE) return new ParseResult().failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected ';'"
        ));
        return new ParseResult();
    }

    public ParseResult patternExpr(Node expr) {
        ParseResult res = new ParseResult();
        HashMap<Token, Node> patterns = new HashMap<>();

        if (currentToken.type != TT.LPAREN) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '('"
        ));

        if (peek(1).type == TT.IDENTIFIER) do {
            Token ident = (Token) res.register(expectIdentifier());
            if (res.error != null) return res;
            res.registerAdvancement(); advance();

            if (currentToken.type != TT.BITE) {
                patterns.put(ident, new VarAccessNode(ident));
                continue;
            }
            res.registerAdvancement(); advance();

            Node pattern = (Node) res.register(this.expr());
            if (res.error != null) return res;

            patterns.put(ident, pattern);
        } while (currentToken.type == TT.COMMA);
        else {
            res.registerAdvancement(); advance();
        }

        if (currentToken.type != TT.RPAREN) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected ')'"
        ));
        res.registerAdvancement(); advance();

        return res.success(new PatternNode(expr, patterns));
    }

    public ParseResult matchExpr() {
        ParseResult res = new ParseResult();

        ElseCase elseCase = null;
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TT.KEYWORD, "match")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected match"
            ));
        res.registerAdvancement(); advance();

        Node ref;
        if (!currentToken.type.equals(TT.LPAREN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '('"
            ));
        res.registerAdvancement(); advance();
        ref = (Node) res.register(expr());
        if (res.error != null) return res;
        if (!currentToken.type.equals(TT.RPAREN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ')'"
            ));
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.OPEN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '{'"
            ));
        res.registerAdvancement(); advance();

        Node body;
        boolean pat, def;
        while (currentToken.type != TT.CLOSE) {
            pat = !currentToken.matches(TT.KEYWORD, "case") && !currentToken.matches(TT.KEYWORD, "default");
            def = currentToken.matches(TT.KEYWORD, "default");

            List<Node> conditions = new ArrayList<>();
            reverse();
            do {
                res.registerAdvancement(); advance();
                Node condition;
                if (pat) {
                    condition = (Node) res.register(atom());
                    if (res.error != null) return res;
                    if (currentToken.type == TT.LPAREN) {
                        condition = (Node) res.register(patternExpr(condition));
                        if (res.error != null) return res;
                    }
                } else {
                    res.registerAdvancement();
                    advance();
                    if (!def) {
                        condition = (Node) res.register(expr());
                        if (res.error != null) return res;
                    } else condition = null;
                }
                if (condition != null)
                    conditions.add(condition);
            } while (currentToken.type == TT.COMMA);

            if (currentToken.type != TT.LAMBDA) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '->'"
            ));
            res.registerAdvancement(); advance();

            body = (Node) res.register(expr());
            if (res.error != null) return res;

            if (def)
                elseCase = new ElseCase(body, false);
            else for (Node condition: conditions)
                cases.add(new Case(condition, body, false));
            res.register(expectSemicolon());
            if (res.error != null) return res;
            res.registerAdvancement(); advance();
        }

        res.registerAdvancement(); advance();

        Node swtch = new SwitchNode(ref, cases, elseCase, true);

        if (elseCase == null) {
            Shell.logger.tip(new Tip(swtch.pos_start, swtch.pos_end,
                                      "Match statement should have a default branch", """
match (a) {
    b -> c
    default -> d
    <> This runs in case none of the others match
    <> and helps prevents stray null values.
};""").asString());
        }

        return res.success(swtch);
    }

    public ParseResult call() {
        ParseResult res = new ParseResult();
        Node node = (Node) res.register(this.atom());
        if (res.error != null)
            return res;
        while (currentToken.type.equals(TT.LPAREN) || currentToken.type.equals(TT.CLACCESS)) {
            if (currentToken.type.equals(TT.LPAREN)) {
                List<Token> generics = new ArrayList<>();
                List<Node> arg_nodes = new ArrayList<>();
                Map<String, Node> kwargs = new HashMap<>();
                if (peek(1) != null && !peek(1).type.equals(TT.RPAREN)) {
                    if (peek(1).type != TT.BACK) {
                        do {
                            res.registerAdvancement();
                            advance();

                            if (currentToken.type == TT.SPREAD) {
                                res.registerAdvancement(); advance();
                                arg_nodes.add(new SpreadNode((Node) res.register(this.expr())));
                            } else {
                                arg_nodes.add((Node) res.register(this.expr()));
                            }

                            if (res.error != null)
                                return res;
                        } while (currentToken.type.equals(TT.COMMA));
                    }
                    else {
                        res.registerAdvancement();
                        advance();
                    }

                    if (currentToken.type == TT.BACK) {
                        Token vk; Node val;
                        do {
                            vk = (Token) res.register(expectIdentifier());
                            if (res.error != null) return res;
                            res.registerAdvancement(); advance();

                            if (currentToken.type != TT.BITE) return res.failure(Error.ExpectedCharError(
                                    currentToken.pos_start, currentToken.pos_end,
                                    "Expected ':'"
                            )); res.registerAdvancement(); advance();

                            val = (Node) res.register(this.expr());
                            if (res.error != null) return res;

                            kwargs.put(vk.value.toString(), val);
                        } while (currentToken.type == TT.COMMA);
                    }

                    if (!currentToken.type.equals(TT.RPAREN))
                        return res.failure(Error.ExpectedCharError(
                                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                                "Expected ',' or ')'"
                        ));
                } else {
                    res.registerAdvancement(); advance();
                }
                res.registerAdvancement();
                advance();
                if (currentToken.type == TT.LT && Constants.TYPETOKS.contains(peek(1).type)) {
                    int startIndex = tokIdx;
                    res.registerAdvancement();
                    advance();

                    ParseResult r = buildTypeTok();
                    if (r.error != null) return r;
                    generics.add((Token) res.register(r));

                    while (currentToken.type == TT.COMMA) {
                        res.registerAdvancement();
                        advance();
                        
                        r = buildTypeTok();
                        if (r.error != null) return r;
                        generics.add((Token) res.register(r));
                    }
                    if (currentToken.type != TT.GT) {
                        generics = new ArrayList<>();
                        tokIdx = startIndex;
                        updateTok();
                    } else {
                        res.registerAdvancement();
                        advance();
                    }
                }
                node = new CallNode(
                        node,
                        arg_nodes,
                        generics,
                        kwargs
                );
            }
            else {
                Token tok = (Token) res.register(expectIdentifier());
                if (res.error != null) return res;
                advance(); res.registerAdvancement();
                node = new ClaccessNode(node, tok);
            }
        }
        return res.success(node);
    }

    public ParseResult index() { return binOp(this::call, Collections.singletonList(TT.LSQUARE), this::expr); }

    public ParseResult pow() { return binOp(this::refOp, Arrays.asList(TT.POWER, TT.MOD), this::factor); }

    public ParseResult refOp() { return binOp(this::refSugars, Collections.singletonList(TT.EQ)); }

    static final List<TT> binRefOps = Arrays.asList(TT.PLE, TT.MIE, TT.MUE, TT.DIE, TT.POE);
    static final List<TT> unRefOps  = Arrays.asList(TT.INCR, TT.DECR);

    public ParseResult refSugars() {
        ParseResult res = new ParseResult();

        Node expr = (Node) res.register(this.refExpr());
        if (res.error != null) return res;

        while (binRefOps.contains(currentToken.type) || unRefOps.contains(currentToken.type)) {
            if (unRefOps.contains(currentToken.type)) {
                expr = new BinOpNode(
                    expr,
                    new Token(TT.EQ),
                    new UnaryOpNode(
                        currentToken,
                        expr
                    )
                );

                res.registerAdvancement();
                advance();
            } else if (binRefOps.contains(currentToken.type)) {
                Token opTok = new Token(switch (currentToken.type) {
                    case POE -> TT.POWER;
                    case MUE -> TT.MUL;
                    case DIE -> TT.DIV;
                    case PLE -> TT.PLUS;
                    case MIE -> TT.MINUS;
                    default -> null;
                });
                res.registerAdvancement();
                advance();

                Node right = (Node) res.register(this.expr());
                if (res.error != null) return res;

                expr = new BinOpNode(
                    expr,
                    new Token(TT.EQ),
                    new BinOpNode(
                        expr,
                        opTok,
                        right
                    )
                );
            }
        }
        
        return res.success(expr);
    }

    public ParseResult term() { return binOp(this::factor, Arrays.asList(TT.MUL, TT.DIV)); }

    public ParseResult arithExpr() { return binOp(this::term, Arrays.asList(TT.PLUS, TT.MINUS)); }

    public ParseResult compExpr() {
        ParseResult res = new ParseResult();
        if (currentToken.type.equals(TT.NOT)) {
            Token op_tok = currentToken;
            res.registerAdvancement();
            advance();

            Node node = (Node) res.register(compExpr());
            if (res.error != null)
                return res;
            return res.success(new UnaryOpNode(op_tok, node));
        }
        Node node = (Node) res.register(binOp(this::arithExpr, Arrays.asList(TT.EE, TT.NE, TT.LT, TT.GT, TT.LTE, TT.GTE)));

        if (res.error != null)
            return res;
        return res.success(node);
    }

    public ParseResult getExpr() {
        ParseResult res = new ParseResult();
        Node node = (Node) res.register(binOp(this::btshftExpr, Arrays.asList(TT.AND, TT.OR)));
        if (res.error != null)
            return res;
        return res.success(node);
    }

    public ParseResult binOp(L left_func, List<TT> ops) {
        return binOp(left_func, ops, null);
    }

    public ParseResult binOp(L left_func, List<TT> ops, L right_func) {
        ParseResult res = new ParseResult();
        if (right_func == null)
            right_func = left_func;
        Node right; Node left;
        left = (Node) res.register(left_func.execute());
        if (res.error != null)
            return res;

        while (ops.contains(currentToken.type)) {
            Token op_tok = currentToken;
            res.registerAdvancement();
            advance();
            right = (Node) res.register(right_func.execute());
            if (res.error != null)
                return res;
            if (op_tok.type == TT.LSQUARE) {
                if (currentToken.type != TT.RSQUARE) return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected closing bracket (']')"
                ));
                advance();
                res.registerAdvancement();
            }
            if (op_tok.type == TT.DOT && right.jptype == Constants.JPType.Call) {
                CallNode call = (CallNode) right;
                call.argNodes.add(0, left);
                left = call;
            } else left = new BinOpNode(left, op_tok, right);
        }
        return res.success(left);
    }

    static class ArgData {
        public final List<Token> argNameToks;
        public final List<Token> argTypeToks;
        public final List<Token> generics;
        public final List<Node> defaults;
        public final int defaultCount;
        public final String argname;
        public final String kwargname;
        public ArgData(List<Token> argNameToks, List<Token> argTypeToks, List<Node> defaults, int defaultCount,
                       List<Token> generics, String argname, String kwargname) {
            this.argNameToks = argNameToks;
            this.argTypeToks = argTypeToks;
            this.defaults = defaults;
            this.defaultCount = defaultCount;
            this.generics = generics;
            this.argname = argname;
            this.kwargname = kwargname;
        }
    }

    public ParseResult gatherArgs() {
        ParseResult res = new ParseResult();

        List<Token> argNameToks = new ArrayList<>();
        List<Token> argTypeToks = new ArrayList<>();

        String argname = null;
        String kwargname = null;

        List<Node> defaults = new ArrayList<>();
        int defaultCount = 0;
        boolean optionals = false;

        Token anyToken = new Token(TT.TYPE, Collections.singletonList("any"));
        if (currentToken.type.equals(TT.LT)) {
            if (peek(1) != null && peek(1).type != TT.BACK) {
                boolean args;
                do {
                    res.registerAdvancement();
                    advance();

                    args = currentToken.type == TT.SPREAD;
                    if (args) {
                        res.registerAdvancement();
                        advance();
                    }

                    if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "Expected identifier"
                    ));
                    matchConvention(currentToken, "Parameter", NamingConvention.CamelCase);

                    if (args) {
                        argname = currentToken.value.toString();
                        res.registerAdvancement();
                        advance();
                        break;
                    }

                    argNameToks.add(currentToken);
                    res.registerAdvancement();
                    advance();

                    if (currentToken.type.equals(TT.USE)) {
                        res.registerAdvancement();
                        advance();

                        Token typetok = (Token) res.register(buildTypeTok());
                        if (res.error != null) return res;
                        argTypeToks.add(typetok);
                    } else argTypeToks.add(anyToken);
                    if (currentToken.type.equals(TT.EQS)) {
                        res.registerAdvancement();
                        advance();

                        Node val = (Node) res.register(arithExpr());
                        if (res.error != null) return res;

                        defaults.add(val);
                        defaultCount++;
                        optionals = true;
                    } else if (optionals) return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "Expected default value"
                    ));

                } while (currentToken.type.equals(TT.COMMA));
            } else {
                res.registerAdvancement();
                advance();
            }

            if (currentToken.type == TT.BACK) {
                Token kw = (Token) res.register(expectIdentifier("Parameter", NamingConvention.CamelCase));
                if (res.error != null) return res;
                res.registerAdvancement(); advance();
                kwargname = kw.value.toString();
            }

            if (!currentToken.type.equals(TT.GT)) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '>'"
            )); advance(); res.registerAdvancement();
        }

        List<Token> generics = new ArrayList<>();
        if (currentToken.type.equals(TT.LPAREN)) {
            res.registerAdvancement(); advance();
            do {
                if (currentToken.type == TT.COMMA) {
                    res.registerAdvancement(); advance();
                }
                if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected type"
                ));

                matchConvention(currentToken, "Generic type", NamingConvention.PascalCase);

                generics.add(currentToken);
                res.registerAdvancement(); advance();
            } while (currentToken.type == TT.COMMA);
            if (!currentToken.type.equals(TT.RPAREN)) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ')'"
            ));
            res.registerAdvancement();
            advance();
        }

        return res.success(new ArgData(
                argNameToks,
                argTypeToks,
                defaults,
                defaultCount,
                generics,
                argname,
                kwargname
        ));
    }

    public void endLine(int offset) {
        tokens.add(
                tokIdx + offset,
                new Token(
                        TT.INVISILINE,
                        currentToken.pos_start.copy(),
                        currentToken.pos_start.copy()
                )
        );
        tokount++;
    }

    public ParseResult block() { return block(true); }
    public ParseResult block(boolean vLine) {
        ParseResult res = new ParseResult();
        if (!currentToken.type.equals(TT.OPEN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '{'"
            ));

        res.registerAdvancement(); advance();

        Node statements = (Node) res.register(this.statements(TT.CLOSE));
        if (res.error != null)
            return res;

        if (!currentToken.type.equals(TT.CLOSE))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '}'"
            ));
        if (vLine) endLine(1);
        res.registerAdvancement(); advance();

        return res.success(statements);
    }

    // EXPRESSIONS

    // If Parts

    public ParseResult ifExpr() {
        ParseResult res = new ParseResult();
        var allCases = (Pair<List<Case>, ElseCase>) res.register(this.ifExprCases("if", true));
        if (res.error != null)
            return res;
        List<Case> cases = allCases.a;
        ElseCase elseCase = allCases.b;
        endLine(0); updateTok();
        return res.success(new QueryNode(cases, elseCase));
    }

    public ParseResult ifExprCases(String caseKeyword, boolean parenthesis) {
        ParseResult res = new ParseResult();
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TT.KEYWORD, caseKeyword))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    String.format("Expected %s", caseKeyword)
            ));

        res.registerAdvancement();
        advance();

        if (parenthesis) {
            if (!currentToken.type.equals(TT.LPAREN))
                return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '('"
                ));
            res.registerAdvancement(); advance();
        }
        Node condition = (Node) res.register(this.expr());
        if (res.error != null)
            return res;

        if (condition.jptype == Constants.JPType.Boolean && ((BooleanNode) condition).val) {
            Shell.logger.tip(new Tip(condition.pos_start, condition.pos_end,
                    "Redundant conditional", """
if (true)
    println("This runs no matter what");""")
                    .asString());
        } else if (condition.jptype == Constants.JPType.Boolean) {
            Shell.logger.tip(new Tip(condition.pos_start, condition.pos_end,
                    "Conditional will never run", """
if (false)
    println("Useless!!!");""")
                    .asString());
        }

        if (parenthesis) {
            if (!currentToken.type.equals(TT.RPAREN))
                return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected ')'"
                ));
            res.registerAdvancement();
            advance();
        }

        Node statements;
        if (currentToken.type.equals(TT.OPEN))
            statements = (Node) res.register(this.block(false));
        else {
            statements = (Node) res.register(this.statement());
            res.register(expectSemicolon());
            if (res.error != null) return res;
            res.registerAdvancement(); advance();
        }
        if (res.error != null)
            return res;
        cases.add(new Case(condition, statements, true));

        Pair<List<Case>, ElseCase> allCases = (Pair<List<Case>, ElseCase>) res.register(this.elifElse(parenthesis));
        if (res.error != null)
            return res;
        List<Case> newCases = allCases.a;
        ElseCase elseCase = allCases.b;
        cases.addAll(newCases);

        return res.success(new Pair<>(cases, elseCase));
    }

    public ParseResult elifElse(boolean parenthesis) {
        ParseResult res = new ParseResult();
        List<Case> cases = new ArrayList<>();
        ElseCase elseCase;

        if (currentToken.matches(TT.KEYWORD, "elif")) {
            var allCases = (Pair<List<Case>, ElseCase>) res.register(this.elifExpr(parenthesis));
            if (res.error != null)
                return res;
            cases = allCases.a;
            elseCase = allCases.b;
        }
        else {
            elseCase = (ElseCase) res.register(this.elseExpr());
            if (res.error != null)
                return res;
        } return res.success(
                new Pair<>(cases, elseCase)
        );

    }

    public ParseResult elifExpr(boolean parenthesis) {
        return ifExprCases("elif", parenthesis);
    }

    public ParseResult elseExpr() {
        ParseResult res = new ParseResult();
        ElseCase elseCase = null;

        if (currentToken.matches(TT.KEYWORD, "else")) {
            res.registerAdvancement(); advance();

            Node statements;
            if (currentToken.type.equals(TT.OPEN))
                statements = (Node) res.register(this.block());
            else {
                statements = (Node) res.register(this.statement());
                res.register(expectSemicolon());
                if (res.error != null) return res;
                res.registerAdvancement(); advance();
            }
            if (res.error != null)
                return res;
            elseCase = new ElseCase(statements, true);
        }

        return res.success(elseCase);
    }

    // Query

    public ParseResult kv() {
        ParseResult res = new ParseResult();
        if (!currentToken.type.equals(TT.BITE)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected ':'"
        ));
        res.registerAdvancement(); advance();
        Node expr = (Node) res.register(expr());
        if (res.error != null) return res;
        return res.success(expr);
    }

    public ParseResult queryExpr() {
        ParseResult res = new ParseResult();
        List<Case> cases = new ArrayList<>();
        ElseCase elseCase = null;

        L getStatement = () -> {
            res.registerAdvancement(); advance();
            Node condition = (Node) res.register(compExpr());
            if (res.error != null) return res;
            Node expr_ = (Node) res.register(this.kv());
            if (res.error != null) return res;
            cases.add(new Case(condition, expr_, false));
            return null;
        };

        if (!currentToken.type.equals(TT.QUERY)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '?'"
        ));

        ParseResult r;
        r = getStatement.execute();
        if (r != null) return r;

        while (currentToken.type.equals(TT.QUEBACK)) {
            r = getStatement.execute();
            if (r != null) return r;
        }

        if (currentToken.type.equals(TT.DEFAULTQUE)) {
            res.registerAdvancement(); advance();
            if (!currentToken.type.equals(TT.BITE)) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ':'"
            ));
            res.registerAdvancement(); advance();

            Node expr = (Node) res.register(this.statement());
            if (res.error != null) return res;
            elseCase = new ElseCase(expr, false);
        }
        return res.success(new QueryNode(cases, elseCase));
    }

    // Loops

    public ParseResult forExpr() {
        ParseResult res = new ParseResult();

        if (!currentToken.matches(TT.KEYWORD, "for"))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected 'for'"
            ));

        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.LPAREN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '('"
            ));
        res.registerAdvancement();
        advance();

        if (!currentToken.type.equals(TT.IDENTIFIER))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected identifier"
            ));
        
        matchConvention(currentToken, "Variable name", NamingConvention.CamelCase);

        Token varName = currentToken;
        res.registerAdvancement(); advance();

        boolean iterating = currentToken.type.equals(TT.ITER);
        if (!currentToken.type.equals(TT.LAMBDA) && !currentToken.type.equals(TT.ITER))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected weak assignment or iter ('->', '<-')"
            ));
        res.registerAdvancement(); advance();

        if (iterating) {
            Node iterableNode = (Node) res.register(getClosing());
            if (res.error != null) return res;
            Node body;
            switch (currentToken.type) {
                case OPEN -> {
                    body = (Node) res.register(this.block());
                    if (res.error != null) return res;
                    return res.success(new IterNode(varName, iterableNode, body, true));
                }
                case EQ -> {
                    res.registerAdvancement();
                    advance();
                    body = (Node) res.register(this.statement());
                    if (res.error != null) return res;
                    return res.success(new IterNode(varName, iterableNode, body, false));
                }
                default -> {
                    body = (Node) res.register(this.statement());
                    if (res.error != null) return res;
                    return res.success(new IterNode(varName, iterableNode, body, true));
                }
            }
        }
        Node start = (Node) res.register(this.compExpr());
        if (res.error != null) return res;

        if (!currentToken.type.equals(TT.BITE))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ':'"
            ));
        res.registerAdvancement(); advance();

        Node end = (Node) res.register(this.expr());
        if (res.error != null) return res;

        Node step;
        if (currentToken.type.equals(TT.STEP)) {
            res.registerAdvancement(); advance();
            step = (Node) res.register(this.expr());
        } else step = null;

        if (!currentToken.type.equals(TT.RPAREN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ')'"
            ));
        res.registerAdvancement();
        advance();

        Node body;
        switch (currentToken.type) {
            case OPEN -> {
                body = (Node) res.register(this.block());
                if (res.error != null) return res;
                return res.success(new ForNode(varName, start, end, step, body, true));
            }
            case EQ -> {
                res.registerAdvancement();
                advance();
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new ForNode(varName, start, end, step, body, false));
            }
            default -> {
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new ForNode(varName, start, end, step, body, true));
            }
        }

    }

    public ParseResult getClosing() {
        ParseResult res = new ParseResult();
        Node condition = (Node) res.register(this.expr());
        if (res.error != null) return res;
        if (!currentToken.type.equals(TT.RPAREN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ')'"
            ));
        res.registerAdvancement(); advance();
        return res.success(condition);
    }

    public ParseResult getWhileCondition() {
        ParseResult res = new ParseResult();

        if (!currentToken.matches(TT.KEYWORD, "while")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'while'"
        ));
        res.registerAdvancement();
        advance();

        if (!currentToken.type.equals(TT.LPAREN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '('"
            ));
        res.registerAdvancement();
        advance();

        Node condition = (Node) res.register(getClosing());
        if (res.error != null) return res;
        if (condition.jptype == Constants.JPType.Boolean) {
            if (((BooleanNode) condition).val) {
                Shell.logger.tip(new Tip(condition.pos_start, condition.pos_end,
                        "Can be changed to a generic loop", """
loop {
    println("To infinity and beyond!");
}""")
                        .asString());
            } else {
                Shell.logger.tip(new Tip(condition.pos_start, condition.pos_end,
                        "Loop will never run", """
while (false) {
    println("Remove me!");
}""")
                        .asString());
            }
        }

        return res.success(condition);
    }

    public ParseResult doExpr() {
        ParseResult res = new ParseResult();

        if (!currentToken.matches(TT.KEYWORD, "do")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'do'"
        ));
        res.registerAdvancement();
        advance();

        Node body;
        boolean bracket = currentToken.type == TT.OPEN;
        switch (currentToken.type) {
            case EQ -> {
                res.registerAdvancement();
                advance();
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
            }
            case OPEN -> {
                body = (Node) res.register(block(false));
                if (res.error != null) return res;
            }
            default -> {
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
            }
        }

        Node condition = (Node) res.register(getWhileCondition());
        if (res.error != null) return res;

        return res.success(new WhileNode(condition, body, bracket, true));
    }

    public ParseResult whileExpr() {
        ParseResult res = new ParseResult();

        Node condition;
        if (currentToken.matches(TT.KEYWORD, "loop")) {
            Token loopTok = currentToken;
            res.registerAdvancement();
            advance();
            condition = new BooleanNode(new Token(TT.BOOL, true, loopTok.pos_start, loopTok.pos_end));
        } else {
            condition = (Node) res.register(getWhileCondition());

            if (res.error != null) return res;
        }
        Node body;
        switch (currentToken.type) {
            case EQ -> {
                res.registerAdvancement();
                advance();
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, false, false));
            }
            case OPEN -> {
                body = (Node) res.register(block());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, true, false));
            }
            default -> {
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, true, false));
            }
        }

    }

    // Collections

    public ParseResult listExpr() {
        ParseResult res = new ParseResult();
        List<Node> elementNodes = new ArrayList<>();
        Position pos_start = currentToken.pos_start.copy();

        if (!currentToken.type.equals(TT.LSQUARE)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '['"
        ));
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.RSQUARE)) {
            elementNodes.add((Node) res.register(this.expr()));
            if (res.error != null) return res;

            while (currentToken.type.equals(TT.COMMA)) {
                res.registerAdvancement();
                advance();
                elementNodes.add((Node) res.register(this.expr()));
                if (res.error != null) return res;
            }
            if (!currentToken.type.equals(TT.RSQUARE)) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ']'"
            ));
        }
        res.registerAdvancement();
        advance();
        return res.success(new ListNode(
                elementNodes,
                pos_start,
                currentToken.pos_end.copy()
        ));
    }

    public ParseResult dictExpr() {
        ParseResult res = new ParseResult();
        Map<Node, Node> dict = new HashMap<>();
        Position pos_start = currentToken.pos_start.copy();

        if (!currentToken.type.equals(TT.OPEN)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '{'"
        )); res.registerAdvancement(); advance();

        L kv = () -> {
            Node key = (Node) res.register(compExpr());
            if (res.error != null) return res;

            Node value = (Node) res.register(this.kv());
            if (res.error != null) return res;
            dict.put(key, value);
            return null;
        };

        ParseResult x;
        if (!currentToken.type.equals(TT.CLOSE)) {
            x = kv.execute();
            if (x != null) return x;
        }

        while (currentToken.type.equals(TT.COMMA)) {
            advance(); res.registerAdvancement();
            x = kv.execute();
            if (x != null) return x;
        }
        if (!currentToken.type.equals(TT.CLOSE)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '}'"
        )); res.registerAdvancement(); advance();

        return res.success(new DictNode(dict, pos_start, currentToken.pos_end.copy()));
    }

    public ParseResult isCatcher() {
        ParseResult res = new ParseResult();
        if (currentToken.type == TT.LSQUARE) {
            res.registerAdvancement(); advance();
            if (currentToken.type != TT.RSQUARE) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start, currentToken.pos_end,
                    "Expected ']'"
            ));
            res.registerAdvancement(); advance();
            return res.success(true);
        } return res.success(false);
    }

    // Executables

    public ParseResult funcDef() {
        ParseResult res = new ParseResult();

        String tokV = (String) currentToken.value;
        if (!currentToken.type.equals(TT.KEYWORD) && Arrays.asList("fn", "function", "yourmom").contains(tokV))
            return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'function'"
        )); advance(); res.registerAdvancement();

        boolean async = false;
        if (currentToken.matches(TT.KEYWORD, "async")) {
            async = true;
            advance(); res.registerAdvancement();
        }

        Token varNameTok = null;
        if (currentToken.type.equals(TT.IDENTIFIER)) {
            if (tokV.equals("yourmom"))
                return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "yourmom is invalid B) (must be a lambda)"
                ));
            varNameTok = currentToken;
            matchConvention(varNameTok, "Function name", NamingConvention.CamelCase);
            res.registerAdvancement(); advance();
        }

        ArgData argTKs = (ArgData) res.register(gatherArgs());
        if (res.error != null) return res;

        boolean isCatcher = (boolean) res.register(this.isCatcher());
        if (res.error != null) return res;

        List<String> retype = (List<String>) res.register(staticRet());
        if (res.error != null) return res;

        Node nodeToReturn;
        switch (currentToken.type) {
            case LAMBDA -> {
                res.registerAdvancement();
                advance();
                nodeToReturn = (Node) res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new FuncDefNode(
                        varNameTok,
                        argTKs.argNameToks,
                        argTKs.argTypeToks,
                        nodeToReturn,
                        true,
                        async,
                        retype,
                        argTKs.defaults,
                        argTKs.defaultCount,
                        argTKs.generics,
                        argTKs.argname,
                        argTKs.kwargname
                ).setCatcher(isCatcher));
            }
            case OPEN -> {
                nodeToReturn = (Node) res.register(this.block(varNameTok != null));
                if (res.error != null) return res;

                Node funcNode = new FuncDefNode(
                        varNameTok,
                        argTKs.argNameToks,
                        argTKs.argTypeToks,
                        nodeToReturn,
                        false,
                        async,
                        retype,
                        argTKs.defaults,
                        argTKs.defaultCount,
                        argTKs.generics,
                        argTKs.argname,
                        argTKs.kwargname
                ).setCatcher(isCatcher);

                if (nodeToReturn.jptype == Constants.JPType.List && ((ListNode) nodeToReturn).elements.size() == 1) {
                    Shell.logger.tip(new Tip(funcNode.pos_start, funcNode.pos_end,
                                        "Can be refactored to use arrow syntax", "fn addOne<x> -> x + 1;")
                                        .asString());
                }

                return res.success(funcNode);
            }
            default -> {
                if (tokV.equals("yourmom"))
                    return res.failure(Error.ExpectedCharError(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "yourmom is used badly B) (expected '->' or '{')"
                    ));
                return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '->' or '{'"
                ));
            }
        }

    }

    public ParseResult staticRet() {
        ParseResult res = new ParseResult();
        List<String> retype = Collections.singletonList("any");
        if (currentToken.type.equals(TT.EQS)) {
            res.registerAdvancement(); advance();
            Token etok = (Token) res.register(buildTypeTok());
            if (res.error != null) return res;

            retype = (List<String>) etok.value;
        } return res.success(retype);
    }

    public ParseResult classDef() {
        ParseResult res = new ParseResult();

        if (currentToken.type != TT.KEYWORD || !classWords.contains(currentToken.value.toString()))
            return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'recipe', 'class', or 'obj'"
        )); advance(); res.registerAdvancement();
        if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected identifier"
        ));

        matchConvention(currentToken, "Class", NamingConvention.PascalCase);

        Token classNameTok = currentToken;
        res.registerAdvancement(); advance();

        Token ptk = null;
        if (currentToken.type == TT.LAMBDA) {
            advance(); res.registerAdvancement();
            if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected identifier"
            ));
            ptk = currentToken;
            res.registerAdvancement(); advance();
        }


        if (!currentToken.type.equals(TT.OPEN)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '{'"
        )); res.registerAdvancement(); advance();

        List<AttrDeclareNode> attributeDeclarations = new ArrayList<>();

        ArgData argTKs = new ArgData(
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                0,
                new ArrayList<>(),
                null,
                null
        );

        TriFunction<Token, Boolean, Boolean, ParseResult> getComplexAttr = (valTok, isstatic, isprivate) -> {
            ParseResult result = new ParseResult();

            List<String> type = Collections.singletonList("any");
            if (currentToken.type == TT.BITE) {
                result.registerAdvancement(); advance();
                Token t = (Token) result.register(buildTypeTok());
                if (result.error != null) return result;
                type = (List<String>) t.value;
            }

            Node expr = null;
            if (currentToken.type.equals(TT.EQS)) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Should be '=>'"
            ));
            if (currentToken.type == TT.EQ) {
                result.registerAdvancement(); advance();
                expr = (Node) result.register(this.expr());
                if (result.error != null) return result;
            }

            attributeDeclarations.add(new AttrDeclareNode(valTok, type, isstatic, isprivate, expr));

            res.register(expectSemicolon());
            if (res.error != null) return res;
            advance(); result.registerAdvancement();

            return result;
        };

        Node ingredientNode = new ListNode(
                new ArrayList<>(),
                classNameTok.pos_start.copy(),
                classNameTok.pos_end.copy()
        );
        List<MethDefNode> methods = new ArrayList<>();
        while (currentToken.type.equals(TT.KEYWORD) || currentToken.type.equals(TT.IDENTIFIER)) {
            if (currentToken.value.equals("ingredients")) {
                advance(); res.registerAdvancement();
                argTKs = (ArgData) res.register(gatherArgs());
                if (res.error != null) return res;

                ingredientNode = (Node) res.register(this.block(false));
                if (res.error != null) return res;
            }
            else if (methKeywords.contains(currentToken.value.toString())) {
                res.registerAdvancement(); advance();

                boolean async, bin, stat, priv;
                async = bin = stat = priv = false;
                while (currentToken.type.equals(TT.KEYWORD)) {
                    switch (currentToken.value.toString()) {
                        case "bin" -> bin = true;
                        case "async" -> async = true;
                        case "static", "stc" -> stat = true;
                        case "prv" -> priv = true;
                        case "pub" -> priv = false;
                    }
                    advance(); res.registerAdvancement();
                }

                if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected identifier"
                ));
                Token varNameTok = currentToken;

                matchConvention(varNameTok, "Method name", NamingConvention.CamelCase);

                res.registerAdvancement(); advance();
                ArgData args = (ArgData) res.register(gatherArgs());

                boolean isCatcher = (boolean) res.register(this.isCatcher());
                if (res.error != null) return res;

                List<String> retype = (List<String>) res.register(staticRet());
                if (res.error != null) return res;

                Node nodeToReturn;
                switch (currentToken.type) {
                    case LAMBDA:
                        res.registerAdvancement(); advance();
                        nodeToReturn = (Node) res.register(this.statement());
                        if (res.error != null) return res;
                        res.register(expectSemicolon());
                        if (res.error != null) return res;
                        res.registerAdvancement(); advance();
                        methods.add(new MethDefNode(
                                varNameTok,
                                args.argNameToks,
                                args.argTypeToks,
                                nodeToReturn,
                                true,
                                bin,
                                async,
                                retype,
                                args.defaults,
                                args.defaultCount,
                                args.generics,
                                stat,
                                priv,
                                argTKs.argname,
                                argTKs.kwargname
                        ).setCatcher(isCatcher)); break;
                    case OPEN:
                         nodeToReturn = (Node) res.register(this.block(false));
                         if (res.error != null) return res;
                         methods.add(new MethDefNode(
                                 varNameTok,
                                 args.argNameToks,
                                 args.argTypeToks,
                                 nodeToReturn,
                                 false,
                                 bin,
                                 async,
                                 retype,
                                 args.defaults,
                                 args.defaultCount,
                                 args.generics,
                                 stat,
                                 priv,
                                 argTKs.argname,
                                 argTKs.kwargname
                         ).setCatcher(isCatcher)); break;
                    default:
                        return res.failure(Error.ExpectedCharError(
                                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                                "Expected '{' or '->'"
                        ));
                }

            }
            else if (currentToken.type.equals(TT.IDENTIFIER)) {
                Token valTok = currentToken;

                matchConvention(valTok, "Attribute name", NamingConvention.CamelCase);

                res.registerAdvancement(); advance();
                if (currentToken.type.equals(TT.EQS)) return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Should be '=>'"
                ));
                if (currentToken.type == TT.EQ || currentToken.type == TT.BITE) {
                    res.register(getComplexAttr.apply(valTok, false, false));
                    if (res.error != null) return res;
                } else {
                    attributeDeclarations.add(new AttrDeclareNode(valTok));
                    while (currentToken.type.equals(TT.COMMA)) {
                        res.registerAdvancement();
                        advance();
                        if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                                "Expected identifier"
                        ));

                        matchConvention(currentToken, "Attribute name", NamingConvention.CamelCase);

                        attributeDeclarations.add(new AttrDeclareNode(currentToken));
                        advance();
                        res.registerAdvancement();
                    }

                    res.register(expectSemicolon());
                    if (res.error != null) return res;
                    advance(); res.registerAdvancement();
                }
            }
            else if (declKeywords.contains(currentToken.value.toString())) {
                boolean isprivate, isstatic;
                isprivate = isstatic = false;

                while (declKeywords.contains(currentToken.value.toString())) {
                    switch (currentToken.value.toString()) {
                        case "prv" -> isprivate = true;
                        case "static", "stc" -> isstatic = true;
                        case "pub" -> isprivate = false;
                    }
                    res.registerAdvancement(); advance();
                }

                if (currentToken.type != TT.IDENTIFIER) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start, currentToken.pos_end,
                        "Expected identifier"
                ));

                Token valTok = currentToken;

                matchConvention(valTok, "Attribute name", NamingConvention.CamelCase);

                res.registerAdvancement(); advance();
                res.register(getComplexAttr.apply(valTok, isstatic, isprivate));
                if (res.error != null) return res;
            }
            else return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start, currentToken.pos_end,
                        "Unexpected keyword"
                ));
        }
        if (!currentToken.type.equals(TT.CLOSE)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '}'"
        ));
        endLine(1);
        advance(); res.registerAdvancement();

        Node classDef = new ClassDefNode(
                classNameTok,
                attributeDeclarations,
                argTKs.argNameToks,
                argTKs.argTypeToks,
                ingredientNode,
                methods,
                currentToken.pos_end.copy(),
                argTKs.defaults,
                argTKs.defaultCount,
                ptk,
                argTKs.generics,
                argTKs.argname,
                argTKs.kwargname
        );

        if (argTKs.argNameToks.size() == attributeDeclarations.size() && methods.size() == 0) {
            Shell.logger.tip(new Tip(classDef.pos_start, classDef.pos_end,
                    "Can be refactored as a struct", """
struct Vector3 {
    x,
    y,
    z
};""")
                    .asString());
        }

        return res.success(classDef);
    }

}
