package lemon.jpizza.Generators;

import lemon.jpizza.Cases.Case;
import lemon.jpizza.Cases.ElseCase;
import lemon.jpizza.Errors.Error;
import lemon.jpizza.Nodes.Definitions.*;
import lemon.jpizza.Nodes.Expressions.*;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Nodes.Operations.BinOpNode;
import lemon.jpizza.Nodes.Operations.UnaryOpNode;
import lemon.jpizza.Nodes.Values.*;
import lemon.jpizza.Nodes.Variables.AttrAccessNode;
import lemon.jpizza.Nodes.Variables.VarAccessNode;
import lemon.jpizza.Position;
import lemon.jpizza.Results.ParseResult;
import lemon.jpizza.Token;

import java.util.*;

import static lemon.jpizza.Tokens.*;
import lemon.jpizza.Pair;

public class Parser {
    Token currentToken;
    List<Token> tokens;
    int tokIdx = -1;
    int tokount;

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
        updateTok();
    }

    public void updateTok() {
        if (0 <= tokIdx && tokIdx < tokount)
            currentToken = tokens.get(tokIdx);
    }

    public void reverse(int amount) {
        tokIdx -= amount;
        updateTok();
    }
    public void reverse() {
        reverse(1);
    }

    public ParseResult parse() {
        ParseResult res = statements();
        if (res.error == null && !currentToken.type.equals(TT.EOF)) {
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '+', '-', '*', '^', or '/'"
            ));
        } return res;
    }

    public ParseResult statements() {
        ParseResult res = new ParseResult();
        List<Node> statements = new ArrayList<>();
        Position pos_start = currentToken.pos_start.copy();

        int newlineCount;
        while (currentToken.type.equals(TT.NEWLINE)) {
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
            while (currentToken.type.equals(TT.NEWLINE)) {
                res.registerAdvancement();
                advance();
                newlineCount++;
            }
            if (newlineCount == 0) {
                moreStatements = false;
            }
            if (!moreStatements)
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

        if (!currentToken.type.equals(TT.NEWLINE)) {
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    String.format("Missing semicolon, found %s", currentToken.value)
            ));
        } advance();
        return res.success(new ListNode(
                statements,
                pos_start,
                currentToken.pos_end.copy()
        ));
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

    public ParseResult extractVarTok() {
        ParseResult res = new ParseResult();
        res.registerAdvancement();
        advance();

        if (!currentToken.type.equals(TT.IDENTIFIER))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected identifier"
            ));

        Token var_name = currentToken;
        res.registerAdvancement();
        advance();
        return res.success(var_name);
    }

    public ParseResult expr() {
        ParseResult res = new ParseResult();
        String type = "any";
        if (currentToken.matches(TT.KEYWORD, "attr")) {
            Token var_name = (Token) res.register(extractVarTok());
            if (res.error != null) return res;

            if (!currentToken.type.equals(TT.EQ))
                return res.success(new AttrAccessNode(var_name));

            res.registerAdvancement();
            advance();

            Node expr = (Node) res.register(this.expr());
            if (res.error != null)
                return res;
            return res.success(new AttrAssignNode(var_name, expr));
        }
        else if (currentToken.matches(TT.KEYWORD, "var") || currentToken.matches(TT.KEYWORD, "bake")) {
            boolean locked = currentToken.value.equals("bake");
            Token var_name = (Token) res.register(extractVarTok());
            if (res.error != null) return res;

            Integer min = null;
            Integer max = null;

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
                if (currentToken.type != TT.RSQUARE) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start, currentToken.pos_end,
                        "Expected ']'"
                ));
                res.registerAdvancement(); advance();
            }

            if (currentToken.type.equals(TT.USE)) {
                advance(); res.registerAdvancement();
                if (!currentToken.type.equals(TT.IDENTIFIER) && !currentToken.type.equals(TT.KEYWORD)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected type"
                ));
                type = (String) currentToken.value;
                advance(); res.registerAdvancement();
            }

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
        else if (currentToken.matches(TT.KEYWORD, "cal")) {
            Token var_name = (Token) res.register(extractVarTok());
            if (res.error != null) return res;

            if (!currentToken.type.equals(TT.LAMBDA))
                return res.failure(Error.InvalidSyntax(
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
        ParseResult res = new ParseResult();
        advance(); res.registerAdvancement();
        if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected identifier"
        )); return res.success(currentToken);
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

        Node expr = (Node) res.register(this.compExpr());
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
                case "free":
                    Token varTok = (Token) res.register(expectIdentifier());
                    if (res.error != null) return res;
                    res.registerAdvancement(); advance();
                    return res.success(new DropNode(varTok));
                case "throw":
                    Node throwNode = (Node) res.register(this.throwExpr());
                    if (res.error != null) return res;
                    return res.success(throwNode);
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

                case "import":
                    advance();
                    res.registerAdvancement();
                    Token file_name_tok = currentToken;
                    if (!file_name_tok.type.equals(TT.IDENTIFIER))
                        return res.failure(Error.InvalidSyntax(
                                file_name_tok.pos_start.copy(), file_name_tok.pos_end.copy(),
                                "Expected module name"
                        ));
                    advance(); res.registerAdvancement();
                    return res.success(new ImportNode(file_name_tok));

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
                return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected ')'"
                ));
        }
        return res.failure(Error.InvalidSyntax(
                tok.pos_start.copy(), tok.pos_end != null ? tok.pos_end.copy() : tok.pos_start.copy(),
                String.format("Expected long, double, identifier, '+', '-', or '('. Found %s", tok)
        ));
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

        Node expr = (Node) res.register(this.expr());
        if (res.error != null)
            return res;

        return res.success(new ThrowNode(expr));
    }

    public ParseResult structDef() {
        ParseResult res = new ParseResult();
        if (!currentToken.matches(TT.KEYWORD, "struct")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'struct'"
        ));

        List<Token> children = new ArrayList<>();
        List<Token> types = new ArrayList<>();
        List<Node> assignment = new ArrayList<>();

        Token identifier = (Token) res.register(expectIdentifier());
        if (res.error != null) return res;
        res.registerAdvancement(); advance();

        if (currentToken.type != TT.OPEN) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '{'"
        ));
        res.registerAdvancement(); advance();

        Position start = currentToken.pos_start.copy();

        if (currentToken.type == TT.IDENTIFIER)
            do {
                if (currentToken.type == TT.COMMA) {
                    res.registerAdvancement();
                    advance();
                }

                if (currentToken.type != TT.IDENTIFIER) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected identifier"
                ));

                children.add(currentToken);
                types.add(new Token(TT.IDENTIFIER, "any", currentToken.pos_start, currentToken.pos_end));
                assignment.add(new AttrAssignNode(
                        currentToken,
                        new VarAccessNode(currentToken)
                ));

                res.registerAdvancement(); advance();
            } while (currentToken.type == TT.COMMA);
        Position end = currentToken.pos_end.copy();

        if (currentToken.type != TT.CLOSE) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '}'"
        ));
        res.registerAdvancement(); advance();

        return res.success(new ClassDefNode(
                identifier,
                children,
                children,
                types,
                new ListNode(assignment, start, end),
                new ArrayList<>(),
                end,
                new ArrayList<>(),
                0,
                null
        ));

    }

    public ParseResult enumExpr() {
        ParseResult res = new ParseResult();

        List<Token> children = new ArrayList<>();
        List< List<String> > childrenParam = new ArrayList<>();
        List< List<String> > childrenTypes = new ArrayList<>();
        Token name;

        if (!currentToken.matches(TT.KEYWORD, "enum")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'enum'"
        ));
        res.registerAdvancement(); advance();

        if (currentToken.type != TT.IDENTIFIER) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected identifier"
        ));
        name = currentToken;
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.OPEN))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '{'"
            ));
        res.registerAdvancement(); advance();

        while (currentToken.type == TT.IDENTIFIER) {
            children.add(currentToken);
            res.registerAdvancement(); advance();

            if (currentToken.type == TT.OPEN) {
                List<String> params = new ArrayList<>();
                List<String> types = new ArrayList<>();
                do {
                    Token tok = (Token) res.register(expectIdentifier());
                    if (res.error != null) return res;
                    params.add((String) tok.value);
                    res.registerAdvancement(); advance();

                    if (currentToken.type == TT.BITE) {
                        tok = (Token) res.register(expectIdentifier());
                        if (res.error != null) return res;
                        types.add((String) tok.value);
                        res.registerAdvancement(); advance();
                    } else {
                        types.add("any");
                    }

                } while (currentToken.type == TT.COMMA);
                if (currentToken.type != TT.CLOSE) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '}'"
                ));
                res.registerAdvancement(); advance();
                childrenParam.add(params);
                childrenTypes.add(types);
            } else {
                childrenParam.add(new ArrayList<>());
                childrenTypes.add(new ArrayList<>());
            }

            if (currentToken.type != TT.COMMA) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected comma"
            ));
            res.registerAdvancement(); advance();
        }

        if (!currentToken.type.equals(TT.CLOSE))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '}'"
            ));
        res.registerAdvancement(); advance();

        return res.success(new EnumNode(name, children, childrenParam, childrenTypes));
    }

    @SuppressWarnings("DuplicatedCode")
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
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '('"
            ));
        res.registerAdvancement(); advance();
        ref = (Node) res.register(expr());
        if (res.error != null) return res;
        if (!currentToken.type.equals(TT.RPAREN))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ')'"
            ));
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.OPEN))
            return res.failure(Error.InvalidSyntax(
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
                condition = (Node) res.register(expr());
                if (res.error != null) return res;
            } else condition = null;

            if (currentToken.type != TT.BITE) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ':'"
            ));
            res.registerAdvancement(); advance();

            body = (Node) res.register(statements());
            if (res.error != null) return res;

            if (def)
                elseCase = new ElseCase(body, true);
            else
                cases.add(new Case(condition, body, true));
        }

        if (!currentToken.type.equals(TT.CLOSE))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '}'"
            ));
        res.registerAdvancement(); advance();

        return res.success(new SwitchNode(ref, cases, elseCase, false));
    }

    @SuppressWarnings("DuplicatedCode")
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
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '('"
            ));
        res.registerAdvancement(); advance();
        ref = (Node) res.register(expr());
        if (res.error != null) return res;
        if (!currentToken.type.equals(TT.RPAREN))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ')'"
            ));
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.OPEN))
            return res.failure(Error.InvalidSyntax(
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
                condition = (Node) res.register(expr());
                if (res.error != null) return res;
            } else condition = null;

            if (currentToken.type != TT.LAMBDA) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '->'"
            ));
            res.registerAdvancement(); advance();

            body = (Node) res.register(expr());
            if (res.error != null) return res;

            if (def)
                elseCase = new ElseCase(body, false);
            else
                cases.add(new Case(condition, body, false));
        }

        if (!currentToken.type.equals(TT.CLOSE))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '}'"
            ));
        res.registerAdvancement(); advance();

        return res.success(new SwitchNode(ref, cases, elseCase, true));
    }

    public ParseResult call() {
        ParseResult res = new ParseResult();
        Node node = (Node) res.register(this.atom());
        if (res.error != null)
            return res;
        while (currentToken.type.equals(TT.LPAREN) || currentToken.type.equals(TT.CLACCESS)) {
            if (currentToken.type.equals(TT.LPAREN)) {
                res.registerAdvancement();
                advance();
                List<Node> arg_nodes = new ArrayList<>();
                if (!currentToken.type.equals(TT.RPAREN)) {
                    arg_nodes.add((Node) res.register(this.expr()));
                    if (res.error != null)
                        return res.failure(Error.InvalidSyntax(
                                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                                "Expected argument"
                        ));

                    while (currentToken.type.equals(TT.COMMA)) {
                        res.registerAdvancement();
                        advance();
                        arg_nodes.add((Node) res.register(this.expr()));
                        if (res.error != null)
                            return res;
                    }

                    if (!currentToken.type.equals(TT.RPAREN))
                        return res.failure(Error.InvalidSyntax(
                                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                                "Expected ',' or ')'"
                        ));
                }
                res.registerAdvancement();
                advance();
                node = new CallNode(
                        node,
                        arg_nodes
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

    public ParseResult pow() { return binOp(this::index, Arrays.asList(TT.POWER, TT.MOD), this::factor); }

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
        boolean instantSimplify = left == null || left.fluctuating;

        while (ops.contains(currentToken.type)) {
            Token op_tok = currentToken;
            res.registerAdvancement();
            advance();
            right = (Node) res.register(right_func.execute());
            instantSimplify = instantSimplify && (right == null || right.fluctuating);
            if (res.error != null)
                return res;
            if (op_tok.type == TT.LSQUARE) {
                if (currentToken.type != TT.RSQUARE) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected closing bracket (']')"
                ));
                advance();
                res.registerAdvancement();
            }
            //noinspection ConstantConditions
            left = new BinOpNode(left, op_tok, right).fluctuates(instantSimplify);
        }
        return res.success(left);
    }

    @SuppressWarnings("DuplicatedCode")
    public ParseResult gatherArgs() {
        ParseResult res = new ParseResult();

        List<Token> argNameToks = new ArrayList<>();
        List<Token> argTypeToks = new ArrayList<>();

        List<Node> defaults = new ArrayList<>();
        int defaultCount = 0;
        boolean optionals = false;

        Token anyToken = new Token(TT.IDENTIFIER, "any");
        if (currentToken.type.equals(TT.LT)) {
            advance(); res.registerAdvancement();

            if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected identifier or ommited <>"
            ));
            argNameToks.add(currentToken);
            res.registerAdvancement(); advance();
            if (currentToken.type.equals(TT.USE)) {
                res.registerAdvancement(); advance();

                if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected identifier"
                ));
                argTypeToks.add(currentToken);
                res.registerAdvancement(); advance();
            } else argTypeToks.add(anyToken);

            while (currentToken.type.equals(TT.COMMA)) {
                res.registerAdvancement(); advance();

                if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected identifier"
                ));
                argNameToks.add(currentToken);
                res.registerAdvancement(); advance();

                if (currentToken.type.equals(TT.USE)) {
                    res.registerAdvancement(); advance();

                    if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "Expected identifier"
                    ));
                    argTypeToks.add(currentToken);
                    res.registerAdvancement(); advance();
                } else argTypeToks.add(anyToken);
                if (currentToken.type.equals(TT.EQS)) {
                    res.registerAdvancement(); advance();

                    Node val = (Node) res.register(arithExpr());
                    if (res.error != null) return res;

                    defaults.add(val);
                    defaultCount++;
                    optionals = true;
                } else if (optionals) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected default value"
                ));
                else defaults.add(null);

            }


            if (!currentToken.type.equals(TT.GT)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '>'"
            )); advance(); res.registerAdvancement();
        }
        return res.success(new Pair<>(
                new Pair<>(argNameToks, argTypeToks),
                new Pair<>(defaults, defaultCount)));
    }

    public void endLine(int offset) {
        tokens.add(tokIdx + offset, new Token(TT.NEWLINE, currentToken.pos_start.copy(), currentToken.pos_start.copy()));
        tokount++;
    }

    public ParseResult block() { return block(true); }
    public ParseResult block(boolean vLine) {
        ParseResult res = new ParseResult();
        if (!currentToken.type.equals(TT.OPEN))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '{'"
            ));

        res.registerAdvancement(); advance();

        Node statements = (Node) res.register(this.statements());
        if (res.error != null)
            return res;

        if (!currentToken.type.equals(TT.CLOSE))
            return res.failure(Error.InvalidSyntax(
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
                return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '('"
                ));
            res.registerAdvancement(); advance();
        }
        Node condition = (Node) res.register(this.expr());
        if (res.error != null)
            return res;
        if (parenthesis) {
            if (!currentToken.type.equals(TT.RPAREN))
                return res.failure(Error.InvalidSyntax(
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
            if (!currentToken.type.equals(TT.NEWLINE)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start, currentToken.pos_end,
                    "Missing semicolon"
            )); res.registerAdvancement(); advance();
        }
        if (res.error != null)
            return res;
        cases.add(new Case(condition, statements, true));

        Pair<List<Case>, ElseCase> allCases = (Pair<List<Case>, ElseCase>) res.register(this.elifElse(parenthesis));
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
                if (!currentToken.type.equals(TT.NEWLINE)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start, currentToken.pos_end,
                        "Missing semicolon"
                )); res.registerAdvancement(); advance();
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
        if (!currentToken.type.equals(TT.BITE)) return res.failure(Error.InvalidSyntax(
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
            Node condition = (Node) res.register(expr());
            if (res.error != null) return res;
            Node expr_ = (Node) res.register(this.kv());
            if (res.error != null) return res;
            cases.add(new Case(condition, expr_, false));
            return null;
        };

        if (!currentToken.type.equals(TT.QUERY)) return res.failure(Error.InvalidSyntax(
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
            if (!currentToken.type.equals(TT.BITE)) return res.failure(Error.InvalidSyntax(
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
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '('"
            ));
        res.registerAdvancement();
        advance();

        if (!currentToken.type.equals(TT.IDENTIFIER))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected 'identifier'"
            ));

        Token varName = currentToken;
        res.registerAdvancement(); advance();

        boolean iterating = currentToken.type.equals(TT.ITER);
        if (!currentToken.type.equals(TT.LAMBDA) && !currentToken.type.equals(TT.ITER))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected weak assignment or iter ('->', '<-')"
            ));
        res.registerAdvancement(); advance();

        if (iterating) {
            Node iterableNode = (Node) res.register(getClosing());
            if (res.error != null) return res;
            Node body;
            switch (currentToken.type) {
                case OPEN:
                    body = (Node) res.register(this.block());
                    if (res.error != null) return res;
                    return res.success(new IterNode(varName, iterableNode, body, true));
                case EQ:
                    res.registerAdvancement(); advance();
                    body = (Node) res.register(this.statement());
                    if (res.error != null) return res;
                    return res.success(new IterNode(varName, iterableNode, body, false));
                default:
                    return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "Expected '{' or '=>'"
                    ));
            }
        }
        Node start = (Node) res.register(this.expr());
        if (res.error != null) return res;

        if (!currentToken.type.equals(TT.BITE))
            return res.failure(Error.InvalidSyntax(
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
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ')'"
            ));
        res.registerAdvancement();
        advance();

        Node body;
        switch (currentToken.type) {
            case OPEN:
                body = (Node) res.register(this.block());
                if (res.error != null) return res;
                return res.success(new ForNode(varName, start, end, step, body, true));
            case EQ:
                res.registerAdvancement(); advance();
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new ForNode(varName, start, end, step, body, false));
            default:
                return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '{' or '=>'"
                ));
        }

    }

    public ParseResult getClosing() {
        ParseResult res = new ParseResult();
        Node condition = (Node) res.register(this.expr());
        if (res.error != null) return res;
        if (!currentToken.type.equals(TT.RPAREN))
            return res.failure(Error.InvalidSyntax(
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
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '('"
            ));
        res.registerAdvancement();
        advance();

        Node condition = (Node) res.register(getClosing());
        if (res.error != null) return res;

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
            case EQ:
                res.registerAdvancement(); advance();
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
                break;
            case OPEN:
                body = (Node) res.register(block(false));
                if (res.error != null) return res;
                break;
            default:
                return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '{' or '=>'"
                ));
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
            case EQ:
                res.registerAdvancement(); advance();
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, false, false));
            case OPEN:
                body = (Node) res.register(block());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, true, false));
            default:
                return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '{' or '=>'"
                ));
        }

    }

    // Collections

    public ParseResult listExpr() {
        ParseResult res = new ParseResult();
        List<Node> elementNodes = new ArrayList<>();
        Position pos_start = currentToken.pos_start.copy();

        if (!currentToken.type.equals(TT.LSQUARE)) return res.failure(Error.InvalidSyntax(
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
            if (!currentToken.type.equals(TT.RSQUARE)) return res.failure(Error.InvalidSyntax(
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

        if (!currentToken.type.equals(TT.OPEN)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '{'"
        )); res.registerAdvancement(); advance();

        L kv = () -> {
            Node key = (Node) res.register(expr());
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
        if (!currentToken.type.equals(TT.CLOSE)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '}'"
        )); res.registerAdvancement(); advance();

        return res.success(new DictNode(dict, pos_start, currentToken.pos_end.copy()));
    }

    public ParseResult isCatcher() {
        ParseResult res = new ParseResult();
        if (currentToken.type == TT.LSQUARE) {
            res.registerAdvancement(); advance();
            if (currentToken.type != TT.RSQUARE) return res.failure(Error.InvalidSyntax(
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
            res.registerAdvancement(); advance();
        }

        var argTKs = (Pair<
                Pair< List<Token>, List<Token> >,
                Pair< List<Node>, Integer >
                >) res.register(gatherArgs());
        if (res.error != null) return res;

        boolean isCatcher = (boolean) res.register(this.isCatcher());
        if (res.error != null) return res;

        String retype = (String) res.register(staticRet());
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
                        argTKs.a.a,
                        argTKs.a.b,
                        nodeToReturn,
                        true,
                        async,
                        retype,
                        argTKs.b.a,
                        argTKs.b.b
                ).setCatcher(isCatcher));
            }
            case OPEN -> {
                nodeToReturn = (Node) res.register(this.block(varNameTok != null));
                if (res.error != null) return res;
                return res.success(new FuncDefNode(
                        varNameTok,
                        argTKs.a.a,
                        argTKs.a.b,
                        nodeToReturn,
                        false,
                        async,
                        retype,
                        argTKs.b.a,
                        argTKs.b.b
                ).setCatcher(isCatcher));
            }
            default -> {
                if (tokV.equals("yourmom"))
                    return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "yourmom is used badly B) (expected '->' or '{')"
                    ));
                return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '->' or '{'"
                ));
            }
        }

    }

    public ParseResult staticRet() {
        ParseResult res = new ParseResult();
        String retype = "any";
        if (currentToken.type.equals(TT.EQS)) {
            Token etok = (Token) res.register(expectIdentifier());
            if (res.error != null) return res;
            res.registerAdvancement(); advance();

            retype = (String) etok.value;
        } return res.success(retype);
    }

    public ParseResult classDef() {
        ParseResult res = new ParseResult();

        if (!currentToken.matches(TT.KEYWORD, "recipe")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'recipe'"
        )); advance(); res.registerAdvancement();
        if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected identifier"
        ));
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


        if (!currentToken.type.equals(TT.OPEN)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '{'"
        )); res.registerAdvancement(); advance();

        List<Token> attributeDeclarations = new ArrayList<>();
        if (currentToken.type.equals(TT.IDENTIFIER)) {
            attributeDeclarations.add(currentToken);
            advance(); res.registerAdvancement();
            while (currentToken.type.equals(TT.COMMA)) {
                res.registerAdvancement(); advance();
                if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected identifier"
                ));
                attributeDeclarations.add(currentToken);
                advance(); res.registerAdvancement();
            }
            if (!currentToken.type.equals(TT.NEWLINE)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Missing semicolon"
            )); advance(); res.registerAdvancement();
        }

        Pair<
            Pair< List<Token>, List<Token> >,
            Pair< List<Node>, Integer >
        > argTKs = new Pair<>(
                new Pair<>(new ArrayList<>(), new ArrayList<>()),
                new Pair<>(new ArrayList<>(), 0)
        );
        Node ingredientNode = new ListNode(
                new ArrayList<>(),
                classNameTok.pos_start.copy(),
                classNameTok.pos_end.copy()
        );
        List<MethDefNode> methods = new ArrayList<>();
        while (currentToken.type.equals(TT.KEYWORD) &&
                (currentToken.value.equals("method") || currentToken.value.equals("ingredients"))) {
            if (currentToken.value.equals("ingredients")) {
                advance(); res.registerAdvancement();
                argTKs = (Pair<
                        Pair< List<Token>, List<Token> >,
                        Pair< List<Node>, Integer >
                        >) res.register(gatherArgs());
                if (res.error != null) return res;

                ingredientNode = (Node) res.register(this.block(false));
                if (res.error != null) return res;
            }
            else if (currentToken.value.equals("method")) {
                res.registerAdvancement(); advance();

                boolean bin = false; boolean async = false;
                while (currentToken.type.equals(TT.KEYWORD) &&
                        (currentToken.value.equals("bin") || currentToken.value.equals("async"))) {
                    if (currentToken.value.equals("bin")) bin = true;
                    else async = true;
                    advance(); res.registerAdvancement();
                }

                if (!currentToken.type.equals(TT.IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected identifier"
                ));
                Token varNameTok = currentToken;
                res.registerAdvancement(); advance();
                var args = (Pair<
                        Pair< List<Token>, List<Token> >,
                        Pair< List<Node>, Integer >
                        >) res.register(gatherArgs());

                boolean isCatcher = (boolean) res.register(this.isCatcher());
                if (res.error != null) return res;

                String retype = (String) res.register(staticRet());
                if (res.error != null) return res;

                Node nodeToReturn;
                switch (currentToken.type) {
                    case LAMBDA:
                        res.registerAdvancement(); advance();
                        nodeToReturn = (Node) res.register(this.statement());
                        if (res.error != null) return res;
                        methods.add(new MethDefNode(
                                varNameTok,
                                args.a.a,
                                args.a.b,
                                nodeToReturn,
                                true,
                                bin,
                                async,
                                retype,
                                args.b.a,
                                args.b.b
                        ).setCatcher(isCatcher)); break;
                    case OPEN:
                         nodeToReturn = (Node) res.register(this.block(false));
                         if (res.error != null) return res;
                         methods.add(new MethDefNode(
                                 varNameTok,
                                 args.a.a,
                                 args.a.b,
                                 nodeToReturn,
                                 false,
                                 bin,
                                 async,
                                 retype,
                                 args.b.a,
                                 args.b.b
                         ).setCatcher(isCatcher)); break;
                    default:
                        return res.failure(Error.InvalidSyntax(
                                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                                "Expected '{' or '->'"
                        ));
                }

            }
        }
        if (!currentToken.type.equals(TT.CLOSE)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '}'"
        )); advance(); res.registerAdvancement();
        return res.success(new ClassDefNode(
                classNameTok,
                attributeDeclarations,
                argTKs.a.a,
                argTKs.a.b,
                ingredientNode,
                methods,
                currentToken.pos_end.copy(),
                argTKs.b.a,
                argTKs.b.b,
                ptk
        ));
    }

}
