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
        None
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

    public interface L<T> {
        ParseResult<T> execute();
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

    public ParseResult<Node> parse() {
        ParseResult<Node> res = statements(TT.EOF);
        if (res.error == null && !currentToken.type.equals(TT.EOF)) {
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '+', '-', '*', '^', or '/'"
            ));
        } return res;
    }

    private record TokenMatcher(TT type, String value) {}

    public ParseResult<Node> statements(TT end) {
        return statements(Collections.singletonList(new TokenMatcher(end, null)));
    }

    public ParseResult<Node> statements(List<TokenMatcher> tks) {
        ParseResult<Node> res = new ParseResult<>();
        List<Node> statements = new ArrayList<>();

        int newlineCount;
        while (currentToken.type.equals(TT.NEWLINE) || currentToken.type.equals(TT.INVISILINE)) {
            res.registerAdvancement();
            advance();
        }
        Node statement = res.register(this.statement());
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

            for (TokenMatcher matcher: tks) if (currentToken.type == matcher.type && (matcher.value == null || matcher.value.equals(currentToken.value))) {
                moreStatements = false;
                break;
            }
            if (!moreStatements)
                break;

            statement = res.try_register(this.statement());
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
        return res.success(new BodyNode(statements));
    }

    private String tokenFound() {
        if (currentToken.value != null)
            return String.valueOf(currentToken.value);
        return String.valueOf(currentToken.type);
    }

    public ParseResult<Node> statement() {
        ParseResult<Node> res = new ParseResult<>();
        Position pos_start = currentToken.pos_start.copy();

        if (currentToken.matches(TT.KEYWORD, "return")) {
            res.registerAdvancement();
            advance();
            Node expr = null;
            if (currentToken.type != TT.NEWLINE && currentToken.type != TT.INVISILINE) {
                expr = res.register(this.expr());
                if (res.error != null)
                    return res;
            }
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

        Node expr = res.register(this.expr());
        if (res.error != null)
            return res;
        return res.success(expr);
    }

    public ParseResult<Token> extractVarTok() { return extractVarTok(false); }

    public ParseResult<Token> extractVarTok(boolean screaming) {
        ParseResult<Token> res = new ParseResult<>();

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

    public ParseResult<Node> chainExpr() { return binOp(this::compExpr, Collections.singletonList(TT.BITE)); }

    public ParseResult<Token> buildTypeTok() {
        List<String> type = new ArrayList<>();
        ParseResult<Token> res = new ParseResult<>();
        Position start = currentToken.pos_start;
        Position end = start;

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
            end = currentToken.pos_end;
            res.registerAdvancement(); advance();
        }

        return res.success(new Token(TT.TYPE, type, start, end));
    }

    public ParseResult<Node> expr() {
        ParseResult<Node> res = new ParseResult<>();
        List<String> type = Collections.singletonList("any");
        if (currentToken.matches(TT.KEYWORD, "attr")) {
            res.registerAdvancement(); advance();
            Token var_name = res.register(extractVarTok());
            if (res.error != null) return res;

            if (currentToken.type.equals(TT.EQS)) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Should be '=>'"
            ));
            if (!currentToken.type.equals(TT.EQ))
                return res.success(new AttrAccessNode(var_name));

            res.registerAdvancement();
            advance();

            Node expr = res.register(this.expr());
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

                Node destructed = res.register(expr());
                if (res.error != null) return res;

                if (glob)
                    return res.success(new DestructNode(destructed));
                else
                    return res.success(new DestructNode(destructed, destructs));
            }
            Token var_name = res.register(extractVarTok(locked));
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
                    var_name = res.register(expectIdentifier("Variable name", NamingConvention.CamelCase));
                    if (res.error != null) return res;
                    varNames.add(new VarAssignNode(var_name, nll).setType(type));
                    res.registerAdvancement(); advance();
                } while (currentToken.type == TT.COMMA);
                return res.success(new BodyNode(varNames));
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
                Token typeTok = res.register(buildTypeTok());
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
            Node expr = res.register(this.expr());
            if (res.error != null)
                return res;
            return res.success(new VarAssignNode(var_name, expr, locked)
                    .setType(type)
                    .setRange(min, max));
        }
        else if (currentToken.matches(TT.KEYWORD, "let")) {
            Token ident = res.register(expectIdentifier("Variable name", NamingConvention.CamelCase));
            if (res.error != null) return res;
            res.registerAdvancement(); advance();
            if (currentToken.type != TT.EQ) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start, currentToken.pos_end,
                    "Expected '=>'"
            ));
            res.registerAdvancement(); advance();

            Node expr = res.register(this.expr());
            if (res.error != null) return res;

            return res.success(new LetNode(ident, expr));
        }
        else if (currentToken.matches(TT.KEYWORD, "cal")) {
            res.registerAdvancement(); advance();
            Token var_name = res.register(extractVarTok());
            if (res.error != null) return res;

            if (!currentToken.type.equals(TT.LAMBDA))
                return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected weak assignment arrow (->)"
                ));

            res.registerAdvancement();
            advance();
            Node expr = res.register(this.expr());
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
                Node value = res.register(this.expr());
                if (res.error != null)
                    return res;
                return res.success(new VarAssignNode(var_tok, new BinOpNode(
                        new VarAccessNode(var_tok),
                        switch (op_tok.type) {
                            case POE -> TT.POWER;
                            case MUE -> TT.MUL;
                            case DIE -> TT.DIV;
                            case PLE -> TT.PLUS;
                            case MIE -> TT.MINUS;
                            default -> null;
                        }, value
                ), false).setDefining(false));
            }
            if (currentToken.type.equals(TT.INCR) || currentToken.type.equals(TT.DECR)) {
                Token op_tok = currentToken;
                res.registerAdvancement();
                advance();
                return res.success(new VarAssignNode(var_tok, new UnaryOpNode(
                        op_tok.type,
                        new VarAccessNode(var_tok)
                ), false).setDefining(false));
            } reverse();
        }
        Node node = res.register(binOp(this::getExpr, Collections.singletonList(TT.DOT)));

        if (res.error != null)
            return res;

        return res.success(node);
    }

    public ParseResult<Node> factor() {
        ParseResult<Node> res = new ParseResult<>();
        Token tok = currentToken;

        if (Arrays.asList(TT.PLUS, TT.MINUS, TT.INCR, TT.DECR).contains(tok.type)) {
            res.registerAdvancement();
            advance();
            Node factor = res.register(this.factor());
            if (res.error != null)
                return res;
            return res.success(new UnaryOpNode(tok.type, factor));
        } return pow();
    }

    public ParseResult<Token> expectIdentifier() {
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

    public ParseResult<Token> expectIdentifier(String name, NamingConvention convention) {
        ParseResult<Token> res = new ParseResult<>();
        advance(); res.registerAdvancement();
        if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                String.format("Expected %s", name.toLowerCase())
        )); 
        
        matchConvention(currentToken, name, convention);
        
        return res.success(currentToken);
    }

    public ParseResult<Node> useExpr() {
        ParseResult<Node> res = new ParseResult<>();
        Token useToken = res.register(expectIdentifier());
        if (res.error != null) return res;
        advance(); res.registerAdvancement();
        List<Token> args = new ArrayList<>();
        while (currentToken.type.equals(TT.IDENTIFIER)) {
            args.add(currentToken);
            res.registerAdvancement(); advance();
        }
        return res.success(new UseNode(useToken, args));
    }

    public ParseResult<Node> btshftExpr() {
        return binOp(this::btwsExpr, Arrays.asList(TT.LEFTSHIFT, TT.RIGHTSHIFT, TT.SIGNRIGHTSHIFT), this::expr);
    }

    public ParseResult<Node> btwsExpr() {
        return binOp(this::complExpr, Arrays.asList(TT.BITAND, TT.BITOR, TT.BITXOR), this::expr);
    }

    public ParseResult<Node> complExpr() {
        ParseResult<Node> res = new ParseResult<>();
        if (currentToken.type == TT.BITCOMPL || currentToken.type == TT.QUEBACK) {
            Token opToken = currentToken;
            res.registerAdvancement();
            advance();

            Node expr = res.register(this.expr());
            if (res.error != null) return res;

            return res.success(new UnaryOpNode(opToken.type, expr));
        } return byteExpr();
    }

    public ParseResult<Node> byteExpr() {
        ParseResult<Node> res = new ParseResult<>();
        boolean toBytes = currentToken.type == TT.TOBYTE;
        if (toBytes) {
            res.registerAdvancement();
            advance();
        }

        Node expr = res.register(this.chainExpr());
        if (res.error != null) return res;

        if (toBytes)
            return res.success(new BytesNode(expr));
        return res.success(expr);
    }

    public ParseResult<Node> atom() {
        ParseResult<Node> res = new ParseResult<>();
        Token tok = currentToken;

        if (tok.type.equals(TT.KEYWORD))
            switch ((String) tok.value) {
                case "assert" -> {
                    res.registerAdvancement();
                    advance();
                    Node condition = res.register(expr());
                    if (res.error != null) return res;
                    return res.success(new AssertNode(condition));
                }
                case "free" -> {
                    Token varTok = res.register(expectIdentifier());
                    if (res.error != null) return res;
                    res.registerAdvancement();
                    advance();
                    return res.success(new DropNode(varTok));
                }
                case "throw" -> {
                    Node throwNode = res.register(this.throwExpr());
                    if (res.error != null) return res;
                    return res.success(throwNode);
                }
                case "class", "obj", "recipe" -> {
                    Node classDef = res.register(this.classDef());
                    if (res.error != null)
                        return res;
                    return res.success(classDef);
                }
                case "if" -> {
                    Node ifExpr = res.register(this.ifExpr());
                    if (res.error != null)
                        return res;
                    return res.success(ifExpr);
                }
                case "enum" -> {
                    Node enumExpr = res.register(this.enumExpr());
                    if (res.error != null)
                        return res;
                    return res.success(enumExpr);
                }
                case "switch" -> {
                    Node switchExpr = res.register(this.switchExpr());
                    if (res.error != null)
                        return res;
                    return res.success(switchExpr);
                }
                case "match" -> {
                    Node matchExpr = res.register(this.matchExpr());
                    if (res.error != null)
                        return res;
                    return res.success(matchExpr);
                }
                case "null" -> {
                    res.registerAdvancement();
                    advance();
                    return res.success(new NullNode(tok));
                }
                case "for" -> {
                    Node forExpr = res.register(this.forExpr());
                    if (res.error != null)
                        return res;
                    return res.success(forExpr);
                }
                case "function", "yourmom", "fn" -> {
                    Node funcDef = res.register(this.funcDef());
                    if (res.error != null)
                        return res;
                    return res.success(funcDef);
                }
                case "struct" -> {
                    Node structDef = res.register(this.structDef());
                    if (res.error != null)
                        return res;
                    return res.success(structDef);
                }
                case "loop", "while" -> {
                    Node whileExpr = res.register(this.whileExpr());
                    if (res.error != null)
                        return res;
                    return res.success(whileExpr);
                }
                case "do" -> {
                    Node doExpr = res.register(this.doExpr());
                    if (res.error != null)
                        return res;
                    return res.success(doExpr);
                }
                case "scope" -> {
                    res.registerAdvancement();
                    advance();
                    String name = null;
                    if (currentToken.type == TT.LSQUARE) {
                        Token n = res.register(expectIdentifier("Scope", NamingConvention.SnakeCase));
                        if (res.error != null) return res;

                        name = n.value.toString();

                        res.registerAdvancement();
                        advance();
                        if (currentToken.type != TT.RSQUARE) return res.failure(Error.ExpectedCharError(
                                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                                "Expected ']'"
                        ));
                        res.registerAdvancement();
                        advance();
                    }
                    Node statements = res.register(block());
                    if (res.error != null) return res;
                    return res.success(new ScopeNode(name, statements));
                }
                case "import" -> {
                    advance();
                    res.registerAdvancement();
                    Token file_name_tok = currentToken;
                    if (!file_name_tok.type.equals(TT.IDENTIFIER))
                        return res.failure(Error.InvalidSyntax(
                                file_name_tok.pos_start.copy(), file_name_tok.pos_end.copy(),
                                "Expected module name"
                        ));
                    matchConvention(file_name_tok, "Module name", NamingConvention.SnakeCase);
                    advance();
                    res.registerAdvancement();
                    if (currentToken.matches(TT.KEYWORD, "as")) {
                        Token ident = res.register(expectIdentifier("Module name", NamingConvention.SnakeCase));
                        if (res.error != null)
                            return res;
                        res.registerAdvancement();
                        advance();
                        return res.success(new ImportNode(file_name_tok, ident));
                    }
                    return res.success(new ImportNode(file_name_tok));
                }
                case "extend" -> {
                    advance();
                    res.registerAdvancement();
                    Token fileNameTok = currentToken;
                    if (!fileNameTok.type.equals(TT.IDENTIFIER))
                        return res.failure(Error.InvalidSyntax(
                                fileNameTok.pos_start.copy(), fileNameTok.pos_end.copy(),
                                "Expected module name"
                        ));
                    matchConvention(fileNameTok, "Module name", NamingConvention.SnakeCase);
                    advance();
                    res.registerAdvancement();
                    return res.success(new ExtendNode(fileNameTok));
                }
                case "attr" -> {
                    advance();
                    res.registerAdvancement();
                    Token var_name_tok = currentToken;
                    if (!currentToken.type.equals(TT.IDENTIFIER))
                        return res.failure(Error.InvalidSyntax(
                                var_name_tok.pos_start.copy(), var_name_tok.pos_end.copy(),
                                "Expected identifier"
                        ));
                    advance();
                    res.registerAdvancement();
                    return res.success(new AttrAccessNode(var_name_tok));
                }
                default -> {
                }
            }
        else if (currentToken.type == TT.DIV) {
            // Decorator
            // /decorator/ fn abc { xyz; }
            advance();
            res.registerAdvancement();
            Node decorator = res.register(factor());
            if (res.error != null) return res;
            if (currentToken.type != TT.DIV) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected closing slash"
            ));
            res.registerAdvancement();
            advance();
            Node fn = res.register(statement());
            if (res.error != null) return res;
            Token name;
            if (fn.jptype == Constants.JPType.FuncDef)
                name = ((FuncDefNode) fn).var_name_tok;
            else if (fn.jptype == Constants.JPType.ClassDef)
                name = ((ClassDefNode) fn).class_name_tok;
            else if (fn.jptype == Constants.JPType.Decorator)
                name = ((DecoratorNode) fn).name;
            else return res.failure(Error.InvalidSyntax(
                    fn.pos_start.copy(), fn.pos_end.copy(),
                    "Object is not decorable"
            ));
            return res.success(new DecoratorNode(decorator, fn, name));
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
                        TT.MUL,
                        identifier
                ));
            }
            return res.success(new NumberNode(tok));
        }
        else if (tok.type.equals(TT.USE)) {
            Node useExpr = res.register(this.useExpr());
            if (res.error != null) return res;
            return res.success(useExpr);
        }
        else if (tok.type.equals(TT.STRING)) {
            if (((Pair<String, Boolean>) tok.value).b) {
                Node val = res.register(formatStringExpr());
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
                Node value = res.register(expr());
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
            Node queryExpr = res.register(this.queryExpr());
            if (res.error != null)
                return res;
            return res.success(queryExpr);
        }
        else if (tok.type.equals(TT.LSQUARE)) {
            Node listExpr = res.register(this.listExpr());
            if (res.error != null)
                return res;
            return res.success(listExpr);
        }
        else if (tok.type.equals(TT.OPEN)) {
            Node dictExpr = res.register(this.dictExpr());
            if (res.error != null)
                return res;
            return res.success(dictExpr);
        }
        else if (tok.type.equals(TT.LPAREN)) {
            res.registerAdvancement(); advance();
            Node expr = res.register(this.expr());
            if (res.error != null)
                return res;
            if (currentToken.type.equals(TT.RPAREN)) {
                res.registerAdvancement(); advance();
                return res.success(expr);
            }
            else
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

    public ParseResult<Node> refExpr() {
        Token prefixToken;
        if (currentToken.type == TT.MUL || currentToken.type == TT.AND) {
            ParseResult<Node> res = new ParseResult<>();

            prefixToken = currentToken;
            res.registerAdvancement(); advance();

            Node expr = res.register(refExpr());
            if (res.error != null) return res;
            
            if (prefixToken.type == TT.MUL) return res.success(new DerefNode(expr));
            else return res.success(new RefNode(expr));
        }
    
        return index();
    }

    public ParseResult<Node> formatStringExpr() {
        ParseResult<Node> res = new ParseResult<>();
        Token tok = currentToken;
        StringBuilder sb = new StringBuilder();
        Pair<String, Boolean> val = (Pair<String, Boolean>) tok.value;

        TT addToken = TT.PLUS;
        Node node = new StringNode(new Token(TT.STRING, new Pair<>("", false), tok.pos_start, tok.pos_end));
        for (int i = 0; i < val.a.length(); i++) {
            char current = val.a.charAt(i);
            char next = i + 1 < val.a.length() ? val.a.charAt(i + 1) : ' ';
            if (current == '!' && next == '$') {
                sb.append("$");
                i++;
            }
            else if (current == '$' && next == '{') {
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
                for (Token tk : ts.a) {
                    tk.pos_start.idx += tok.pos_start.idx + i;
                    tk.pos_end.idx += tok.pos_start.idx + i;
                }
                Node r = res.register(new Parser(ts.a).statement());
                if (res.error != null) return res;
                node = new BinOpNode(node, addToken, r);
            }
            else {
                sb.append(current);
            }
        }

        res.registerAdvancement(); advance();
        return res.success(new BinOpNode(node, addToken,
                new StringNode(new Token(TT.STRING, new Pair<>(sb.toString(), false), tok.pos_start, tok.pos_end))));
    }

    public ParseResult<Node> throwExpr() {
        ParseResult<Node> res = new ParseResult<>();
        if (!currentToken.matches(TT.KEYWORD, "throw")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'throw'"
        )); res.registerAdvancement(); advance();

        Node first = res.register(this.expr());
        if (res.error != null)
            return res;

        if (currentToken.type == TT.COMMA) {
            res.registerAdvancement(); advance();

            Node second = res.register(this.expr());
            if (res.error != null)
                return res;

            return res.success(new ThrowNode(first, second));
        }

        return res.success(new ThrowNode(new StringNode(new Token(TT.STRING, new Pair<>("Thrown", false),
                first.pos_start, first.pos_end)), first));
    }

    public ParseResult<Node> structDef() {
        ParseResult<Node> res = new ParseResult<>();
        if (!currentToken.matches(TT.KEYWORD, "struct")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'struct'"
        ));

        List<AttrDeclareNode> childrenDecls = new ArrayList<>();
        List<Token> children = new ArrayList<>();
        List<Token> types = new ArrayList<>();
        List<Node> assignment = new ArrayList<>();

        Token identifier = res.register(expectIdentifier("Struct", NamingConvention.PascalCase));
        if (res.error != null) return res;
        res.registerAdvancement(); advance();

        if (currentToken.type != TT.OPEN) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '{'"
        ));

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
                new BodyNode(assignment),
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

    public record EnumChild(Token token, List<String> params,
                            List<List<String>> types,
                            List<String> generics) {
    }

    public ParseResult<Node> enumExpr() {
        ParseResult<Node> res = new ParseResult<>();

        List<EnumChild> children = new ArrayList<>();
        Token name;

        if (!currentToken.matches(TT.KEYWORD, "enum")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'enum'"
        ));
        res.registerAdvancement(); advance();

        boolean pub = currentToken.matches(TT.KEYWORD, "pub");
        if (pub) {
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
                    Token ident = res.register(expectIdentifier("Generic type", NamingConvention.PascalCase));
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
                    Token tok = res.register(expectIdentifier("Parameter", NamingConvention.CamelCase));
                    if (res.error != null) return res;
                    params.add((String) tok.value);
                    res.registerAdvancement(); advance();

                    if (currentToken.type == TT.BITE) {
                        res.registerAdvancement(); advance();
                        tok = res.register(buildTypeTok());
                        if (res.error != null) return res;
                        types.add((List<String>) tok.value);
                    }
                    else {
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

    public ParseResult<Node> switchExpr() {
        ParseResult<Node> res = new ParseResult<>();

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
        ref = res.register(expr());
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
                condition = res.register(compExpr());
                if (res.error != null) return res;
            }
            else condition = null;

            if (currentToken.type != TT.BITE) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ':'"
            ));
            res.registerAdvancement(); advance();

            body = res.register(statements(Arrays.asList(
                    new TokenMatcher(TT.CLOSE, null),
                    new TokenMatcher(TT.KEYWORD, "case"),
                    new TokenMatcher(TT.KEYWORD, "default")
            )));
            if (res.error != null) return res;

            if (def)
                elseCase = new ElseCase(body, false);
            else
                cases.add(new Case(condition, body, false));
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

    public ParseResult<Void> expectSemicolon() {
        if (currentToken.type != TT.INVISILINE && currentToken.type != TT.NEWLINE) return new ParseResult<Void>().failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected ';'"
        ));
        return new ParseResult<>();
    }

    public ParseResult<Node> patternExpr(Node expr) {
        ParseResult<Node> res = new ParseResult<>();
        HashMap<Token, Node> patterns = new HashMap<>();

        if (currentToken.type != TT.LPAREN) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '('"
        ));

        if (peek(1).type == TT.IDENTIFIER) do {
            Token ident = res.register(expectIdentifier());
            if (res.error != null) return res;
            res.registerAdvancement(); advance();

            if (currentToken.type != TT.BITE) {
                patterns.put(ident, new VarAccessNode(ident));
                continue;
            }
            res.registerAdvancement(); advance();

            Node pattern = res.register(this.expr());
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

    public ParseResult<Node> matchExpr() {
        ParseResult<Node> res = new ParseResult<>();

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
        ref = res.register(expr());
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
                    condition = res.register(atom());
                    if (res.error != null) return res;
                    if (currentToken.type == TT.LPAREN) {
                        condition = res.register(patternExpr(condition));
                        if (res.error != null) return res;
                    }
                }
                else {
                    res.registerAdvancement();
                    advance();
                    if (!def) {
                        condition = res.register(expr());
                        if (res.error != null) return res;
                    }
                else condition = null;
                }
                if (condition != null)
                    conditions.add(condition);
            } while (currentToken.type == TT.COMMA);

            if (currentToken.type != TT.LAMBDA) return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '->'"
            ));
            res.registerAdvancement(); advance();

            body = res.register(expr());
            if (res.error != null) return res;

            if (def)
                elseCase = new ElseCase(body, true);
            else for (Node condition: conditions)
                cases.add(new Case(condition, body, true));
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

    public ParseResult<Node> call() {
        ParseResult<Node> res = new ParseResult<>();
        Node node = res.register(this.atom());
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
                                arg_nodes.add(new SpreadNode(res.register(this.expr())));
                            }
                            else {
                                arg_nodes.add(res.register(this.expr()));
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
                            vk = res.register(expectIdentifier());
                            if (res.error != null) return res;
                            res.registerAdvancement(); advance();

                            if (currentToken.type != TT.BITE) return res.failure(Error.ExpectedCharError(
                                    currentToken.pos_start, currentToken.pos_end,
                                    "Expected ':'"
                            )); res.registerAdvancement(); advance();

                            val = res.register(this.expr());
                            if (res.error != null) return res;

                            kwargs.put(vk.value.toString(), val);
                        } while (currentToken.type == TT.COMMA);
                    }

                    if (!currentToken.type.equals(TT.RPAREN))
                        return res.failure(Error.ExpectedCharError(
                                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                                "Expected ',' or ')'"
                        ));
                }
                else {
                    res.registerAdvancement(); advance();
                }
                res.registerAdvancement();
                advance();
                if (currentToken.type == TT.LT && Constants.TYPETOKS.contains(peek(1).type)) {
                    int startIndex = tokIdx;
                    res.registerAdvancement();
                    advance();

                    ParseResult<Token> r = buildTypeTok();
                    if (r.error != null) return res.failure(r.error);
                    generics.add(res.register(r));

                    while (currentToken.type == TT.COMMA) {
                        res.registerAdvancement();
                        advance();
                        
                        r = buildTypeTok();
                        if (r.error != null) return res.failure(r.error);
                        generics.add(res.register(r));
                    }
                    if (currentToken.type != TT.GT) {
                        generics = new ArrayList<>();
                        tokIdx = startIndex;
                        updateTok();
                    }
                    else {
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
                Token tok = res.register(expectIdentifier());
                if (res.error != null) return res;
                advance(); res.registerAdvancement();
                node = new ClaccessNode(node, tok);
            }
        }
        return res.success(node);
    }

    public ParseResult<Node> index() { return binOp(this::call, Collections.singletonList(TT.LSQUARE), this::expr); }

    public ParseResult<Node> pow() { return binOp(this::refOp, Arrays.asList(TT.POWER, TT.MOD), this::factor); }

    public ParseResult<Node> refOp() { return binOp(this::refSugars, Collections.singletonList(TT.EQ)); }

    static final List<TT> binRefOps = Arrays.asList(TT.PLE, TT.MIE, TT.MUE, TT.DIE, TT.POE);
    static final List<TT> unRefOps  = Arrays.asList(TT.INCR, TT.DECR);

    public ParseResult<Node> refSugars() {
        ParseResult<Node> res = new ParseResult<>();

        Node expr = res.register(this.refExpr());
        if (res.error != null) return res;

        while (binRefOps.contains(currentToken.type) || unRefOps.contains(currentToken.type)) {
            if (unRefOps.contains(currentToken.type)) {
                expr = new BinOpNode(
                    expr,
                    TT.EQ,
                    new UnaryOpNode(
                        currentToken.type,
                        expr
                    )
                );

                res.registerAdvancement();
                advance();
            }
            else if (binRefOps.contains(currentToken.type)) {
                TT opTok = switch (currentToken.type) {
                    case POE -> TT.POWER;
                    case MUE -> TT.MUL;
                    case DIE -> TT.DIV;
                    case PLE -> TT.PLUS;
                    case MIE -> TT.MINUS;
                    default -> null;
                };
                res.registerAdvancement();
                advance();

                Node right = res.register(this.expr());
                if (res.error != null) return res;

                expr = new BinOpNode(
                    expr,
                    TT.EQ,
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

    public ParseResult<Node> term() { return binOp(this::factor, Arrays.asList(TT.MUL, TT.DIV)); }

    public ParseResult<Node> arithExpr() { return binOp(this::term, Arrays.asList(TT.PLUS, TT.MINUS)); }

    public ParseResult<Node> compExpr() {
        ParseResult<Node> res = new ParseResult<>();
        if (currentToken.type.equals(TT.NOT)) {
            Token op_tok = currentToken;
            res.registerAdvancement();
            advance();

            Node node = res.register(compExpr());
            if (res.error != null)
                return res;
            return res.success(new UnaryOpNode(op_tok.type, node));
        }
        Node node = res.register(binOp(this::arithExpr, Arrays.asList(TT.EE, TT.NE, TT.LT, TT.GT, TT.LTE, TT.GTE)));

        if (res.error != null)
            return res;
        return res.success(node);
    }

    public ParseResult<Node> getExpr() {
        ParseResult<Node> res = new ParseResult<>();
        Node node = res.register(binOp(this::btshftExpr, Arrays.asList(TT.AND, TT.OR)));
        if (res.error != null)
            return res;
        return res.success(node);
    }

    public ParseResult<Node> binOp(L<Node> left_func, List<TT> ops) {
        return binOp(left_func, ops, null);
    }

    public ParseResult<Node> binOp(L<Node> left_func, List<TT> ops, L<Node> right_func) {
        ParseResult<Node> res = new ParseResult<>();
        if (right_func == null)
            right_func = left_func;
        Node right; Node left;
        left = res.register(left_func.execute());
        if (res.error != null)
            return res;

        while (ops.contains(currentToken.type)) {
            TT op_tok = currentToken.type;
            res.registerAdvancement();
            advance();
            right = res.register(right_func.execute());
            if (res.error != null)
                return res;
            if (op_tok == TT.LSQUARE) {
                if (currentToken.type != TT.RSQUARE) return res.failure(Error.ExpectedCharError(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected closing bracket (']')"
                ));
                advance();
                res.registerAdvancement();
            }
            if (op_tok == TT.DOT && right.jptype == Constants.JPType.Call) {
                CallNode call = (CallNode) right;
                call.argNodes.add(0, left);
                left = call;
            }
            else left = new BinOpNode(left, op_tok, right);
        }
        return res.success(left);
    }

    record ArgData(List<Token> argNameToks, List<Token> argTypeToks,
                   List<Node> defaults, int defaultCount,
                   List<Token> generics, String argname, String kwargname) {
    }

    public ParseResult<ArgData> gatherArgs() {
        ParseResult<ArgData> res = new ParseResult<>();

        List<Token> argNameToks = new ArrayList<>();
        List<Token> argTypeToks = new ArrayList<>();

        String argname = null;
        String kwargname = null;

        List<Node> defaults = new ArrayList<>();
        int defaultCount = 0;
        boolean optionals = false;

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

                        Token typetok = res.register(buildTypeTok());
                        if (res.error != null) return res;
                        argTypeToks.add(typetok);
                    }
                    else argTypeToks.add(new Token(TT.TYPE,
                            Collections.singletonList("any"), currentToken.pos_start, currentToken.pos_end));
                    if (currentToken.type.equals(TT.EQS)) {
                        res.registerAdvancement();
                        advance();

                        Node val = res.register(arithExpr());
                        if (res.error != null) return res;

                        defaults.add(val);
                        defaultCount++;
                        optionals = true;
                    }
                    else if (optionals) return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "Expected default value"
                    ));

                } while (currentToken.type.equals(TT.COMMA));
            }
            else {
                res.registerAdvancement();
                advance();
            }

            if (currentToken.type == TT.BACK) {
                Token kw = res.register(expectIdentifier("Parameter", NamingConvention.CamelCase));
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

    public ParseResult<Node> block() { return block(true); }
    public ParseResult<Node> block(boolean vLine) {
        ParseResult<Node> res = new ParseResult<>();
        if (!currentToken.type.equals(TT.OPEN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '{'"
            ));

        res.registerAdvancement(); advance();

        Node statements = res.register(this.statements(TT.CLOSE));
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

    public ParseResult<Node> ifExpr() {
        ParseResult<Node> res = new ParseResult<>();
        var allCases = res.register(this.ifExprCases("if", true));
        if (res.error != null)
            return res;
        List<Case> cases = allCases.a;
        ElseCase elseCase = allCases.b;
        endLine(0); updateTok();
        return res.success(new QueryNode(cases, elseCase));
    }

    public ParseResult<Pair<List<Case>, ElseCase>> ifExprCases(String caseKeyword, boolean parenthesis) {
        ParseResult<Pair<List<Case>, ElseCase>> res = new ParseResult<>();
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
        Node condition = res.register(this.expr());
        if (res.error != null)
            return res;

        if (condition.jptype == Constants.JPType.Boolean && ((BooleanNode) condition).val) {
            Shell.logger.tip(new Tip(condition.pos_start, condition.pos_end,
                    "Redundant conditional", """
if (true)
    println("This runs no matter what");""")
                    .asString());
        }
        else if (condition.jptype == Constants.JPType.Boolean) {
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
            statements = res.register(this.block(false));
        else {
            statements = res.register(this.statement());
            res.register(expectSemicolon());
            if (res.error != null) return res;
            res.registerAdvancement(); advance();
        }
        if (res.error != null)
            return res;
        cases.add(new Case(condition, statements, false));

        Pair<List<Case>, ElseCase> allCases = res.register(this.elifElse(parenthesis));
        if (res.error != null)
            return res;
        List<Case> newCases = allCases.a;
        ElseCase elseCase = allCases.b;
        cases.addAll(newCases);

        return res.success(new Pair<>(cases, elseCase));
    }

    public ParseResult<Pair<List<Case>, ElseCase>> elifElse(boolean parenthesis) {
        ParseResult<Pair<List<Case>, ElseCase>> res = new ParseResult<>();
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
            elseCase = res.register(this.elseExpr());
            if (res.error != null)
                return res;
        } return res.success(
                new Pair<>(cases, elseCase)
        );

    }

    public ParseResult<Pair<List<Case>, ElseCase>> elifExpr(boolean parenthesis) {
        return ifExprCases("elif", parenthesis);
    }

    public ParseResult<ElseCase> elseExpr() {
        ParseResult<ElseCase> res = new ParseResult<>();
        ElseCase elseCase = null;

        if (currentToken.matches(TT.KEYWORD, "else")) {
            res.registerAdvancement(); advance();

            Node statements;
            if (currentToken.type.equals(TT.OPEN))
                statements = res.register(this.block());
            else {
                statements = res.register(this.statement());
                res.register(expectSemicolon());
                if (res.error != null) return res;
                res.registerAdvancement(); advance();
            }
            if (res.error != null)
                return res;
            elseCase = new ElseCase(statements, false);
        }

        return res.success(elseCase);
    }

    // Query

    public ParseResult<Node> kv() {
        ParseResult<Node> res = new ParseResult<>();
        if (!currentToken.type.equals(TT.BITE)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected ':'"
        ));
        res.registerAdvancement(); advance();
        Node expr = res.register(expr());
        if (res.error != null) return res;
        return res.success(expr);
    }

    public ParseResult<Node> queryExpr() {
        ParseResult<Node> res = new ParseResult<>();
        List<Case> cases = new ArrayList<>();
        ElseCase elseCase = null;

        L<Node> getStatement = () -> {
            res.registerAdvancement(); advance();
            Node condition = res.register(compExpr());
            if (res.error != null) return res;
            Node expr_ = res.register(this.kv());
            if (res.error != null) return res;
            cases.add(new Case(condition, expr_, true));
            return null;
        };

        if (!currentToken.type.equals(TT.QUERY)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '?'"
        ));

        ParseResult<Node> r;
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

            Node expr = res.register(this.statement());
            if (res.error != null) return res;
            elseCase = new ElseCase(expr, true);
        }
        return res.success(new QueryNode(cases, elseCase));
    }

    // Loops

    public ParseResult<Node> forExpr() {
        ParseResult<Node> res = new ParseResult<>();

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
            Node iterableNode = res.register(getClosing());
            if (res.error != null) return res;
            Node body;
            switch (currentToken.type) {
                case OPEN -> {
                    body = res.register(this.block());
                    if (res.error != null) return res;
                    return res.success(new IterNode(varName, iterableNode, body, true));
                }
                case EQ -> {
                    res.registerAdvancement();
                    advance();
                    body = res.register(this.statement());
                    if (res.error != null) return res;
                    return res.success(new IterNode(varName, iterableNode, body, false));
                }
                default -> {
                    body = res.register(this.statement());
                    if (res.error != null) return res;
                    return res.success(new IterNode(varName, iterableNode, body, true));
                }
            }
        }
        Node start = res.register(this.compExpr());
        if (res.error != null) return res;

        if (!currentToken.type.equals(TT.BITE))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ':'"
            ));
        res.registerAdvancement(); advance();

        Node end = res.register(this.expr());
        if (res.error != null) return res;

        Node step;
        if (currentToken.type.equals(TT.STEP)) {
            res.registerAdvancement(); advance();
            step = res.register(this.expr());
        }
        else step = null;

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
                body = res.register(this.block());
                if (res.error != null) return res;
                return res.success(new ForNode(varName, start, end, step, body, true));
            }
            case EQ -> {
                res.registerAdvancement();
                advance();
                body = res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new ForNode(varName, start, end, step, body, false));
            }
            default -> {
                body = res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new ForNode(varName, start, end, step, body, true));
            }
        }

    }

    public ParseResult<Node> getClosing() {
        ParseResult<Node> res = new ParseResult<>();
        Node condition = res.register(this.expr());
        if (res.error != null) return res;
        if (!currentToken.type.equals(TT.RPAREN))
            return res.failure(Error.ExpectedCharError(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ')'"
            ));
        res.registerAdvancement(); advance();
        return res.success(condition);
    }

    public ParseResult<Node> getWhileCondition() {
        ParseResult<Node> res = new ParseResult<>();

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

        Node condition = res.register(getClosing());
        if (res.error != null) return res;
        if (condition.jptype == Constants.JPType.Boolean) {
            if (((BooleanNode) condition).val) {
                Shell.logger.tip(new Tip(condition.pos_start, condition.pos_end,
                        "Can be changed to a generic loop", """
loop {
    println("To infinity and beyond!");
}""")
                        .asString());
            }
            else {
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

    public ParseResult<Node> doExpr() {
        ParseResult<Node> res = new ParseResult<>();

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
                body = res.register(this.statement());
                if (res.error != null) return res;
            }
            case OPEN -> {
                body = res.register(block(false));
                if (res.error != null) return res;
            }
            default -> {
                body = res.register(this.statement());
                if (res.error != null) return res;
            }
        }

        Node condition = res.register(getWhileCondition());
        if (res.error != null) return res;

        return res.success(new WhileNode(condition, body, bracket, true));
    }

    public ParseResult<Node> whileExpr() {
        ParseResult<Node> res = new ParseResult<>();

        Node condition;
        if (currentToken.matches(TT.KEYWORD, "loop")) {
            Token loopTok = currentToken;
            res.registerAdvancement();
            advance();
            condition = new BooleanNode(new Token(TT.BOOL, true, loopTok.pos_start, loopTok.pos_end));
        }
        else {
            condition = res.register(getWhileCondition());

            if (res.error != null) return res;
        }
        Node body;
        switch (currentToken.type) {
            case EQ -> {
                res.registerAdvancement();
                advance();
                body = res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, false, false));
            }
            case OPEN -> {
                body = res.register(block());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, true, false));
            }
            default -> {
                body = res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, true, false));
            }
        }

    }

    // Collections

    public ParseResult<Node> listExpr() {
        ParseResult<Node> res = new ParseResult<>();
        List<Node> elementNodes = new ArrayList<>();
        Position pos_start = currentToken.pos_start.copy();

        if (!currentToken.type.equals(TT.LSQUARE)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '['"
        ));
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.RSQUARE)) {
            elementNodes.add(res.register(this.expr()));
            if (res.error != null) return res;

            while (currentToken.type.equals(TT.COMMA)) {
                res.registerAdvancement();
                advance();
                elementNodes.add(res.register(this.expr()));
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

    public ParseResult<Node> dictExpr() {
        ParseResult<Node> res = new ParseResult<>();
        Map<Node, Node> dict = new HashMap<>();
        Position pos_start = currentToken.pos_start.copy();

        if (!currentToken.type.equals(TT.OPEN)) return res.failure(Error.ExpectedCharError(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '{'"
        )); res.registerAdvancement(); advance();

        L<Node> kv = () -> {
            Node key = res.register(compExpr());
            if (res.error != null) return res;

            Node value = res.register(this.kv());
            if (res.error != null) return res;
            dict.put(key, value);
            return null;
        };

        ParseResult<Node> x;
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

    public ParseResult<Boolean> isCatcher() {
        ParseResult<Boolean> res = new ParseResult<>();
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

    public ParseResult<Node> funcDef() {
        ParseResult<Node> res = new ParseResult<>();

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

        ArgData argTKs = res.register(gatherArgs());
        if (res.error != null) return res;

        boolean isCatcher = res.register(this.isCatcher());
        if (res.error != null) return res;

        List<String> retype = res.register(staticRet());
        if (res.error != null) return res;

        Node nodeToReturn;
        switch (currentToken.type) {
            case LAMBDA -> {
                res.registerAdvancement();
                advance();
                nodeToReturn = res.register(this.statement());
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
                nodeToReturn = res.register(this.block(varNameTok != null));
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

    public ParseResult<List<String>> staticRet() {
        ParseResult<List<String>> res = new ParseResult<>();
        List<String> retype = Collections.singletonList("any");
        if (currentToken.type.equals(TT.EQS)) {
            res.registerAdvancement(); advance();
            Token etok = res.register(buildTypeTok());
            if (res.error != null) return res;

            retype = (List<String>) etok.value;
        } return res.success(retype);
    }

    public ParseResult<Node> classDef() {
        ParseResult<Node> res = new ParseResult<>();

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

        TriFunction<Token, Boolean, Boolean, ParseResult<Node>> getComplexAttr = (valTok, isstatic, isprivate) -> {
            ParseResult<Node> result = new ParseResult<>();

            List<String> type = Collections.singletonList("any");
            if (currentToken.type == TT.BITE) {
                result.registerAdvancement(); advance();
                Token t = result.register(buildTypeTok());
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
                expr = result.register(this.expr());
                if (result.error != null) return result;
            }

            attributeDeclarations.add(new AttrDeclareNode(valTok, type, isstatic, isprivate, expr));

            res.register(expectSemicolon());
            if (res.error != null) return res;
            advance(); result.registerAdvancement();

            return result;
        };

        Node ingredientNode = new BodyNode(
                new ArrayList<>(),
                classNameTok.pos_start.copy(),
                classNameTok.pos_end.copy()
        );
        List<MethDefNode> methods = new ArrayList<>();
        while (currentToken.type.equals(TT.KEYWORD) || currentToken.type.equals(TT.IDENTIFIER)) {
            if (currentToken.value.equals("ingredients")) {
                advance(); res.registerAdvancement();
                argTKs = res.register(gatherArgs());
                if (res.error != null) return res;

                ingredientNode = res.register(this.block(false));
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
                ArgData args = res.register(gatherArgs());

                boolean isCatcher = res.register(this.isCatcher());
                if (res.error != null) return res;

                List<String> retype = res.register(staticRet());
                if (res.error != null) return res;

                Node nodeToReturn;
                switch (currentToken.type) {
                    case LAMBDA:
                        res.registerAdvancement(); advance();
                        nodeToReturn = res.register(this.statement());
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
                                args.argname,
                                args.kwargname
                        ).setCatcher(isCatcher)); break;
                    case OPEN:
                         nodeToReturn = res.register(this.block(false));
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
                                 args.argname,
                                 args.kwargname
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
                }
                else {
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
