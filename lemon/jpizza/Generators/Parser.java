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
        } if (currentToken.matches(TT.KEYWORD, "continue")) {
            res.registerAdvancement();
            advance();
            return res.success(new ContinueNode(pos_start, currentToken.pos_end.copy()));
        } if (currentToken.matches(TT.KEYWORD, "break")) {
            res.registerAdvancement();
            advance();
            return res.success(new BreakNode(pos_start, currentToken.pos_end.copy()));
        }
        if (currentToken.matches(TT.KEYWORD, "pass")) {
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
            return res.success(new VarAssignNode(var_name, expr, locked).setType(type));
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
        return res.success(new UseNode(useToken));
    }

    public ParseResult atom() {
        ParseResult res = new ParseResult();
        Token tok = currentToken;

        if (tok.type.equals(TT.KEYWORD))
            switch ((String) tok.value) {
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

                case "while":
                    Node whileExpr = (Node) res.register(this.whileExpr());
                    if (res.error != null)
                        return res;
                    return res.success(whileExpr);

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

    public ParseResult enumExpr() {
        ParseResult res = new ParseResult();

        List<Token> children = new ArrayList<>();
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

        return res.success(new EnumNode(name, children));
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
        Node node = (Node) res.register(binOp(this::compExpr, Arrays.asList(TT.AND, TT.OR)));
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

        while (ops.contains(currentToken.type)) {
            Token op_tok = currentToken;
            res.registerAdvancement();
            advance();
            right = (Node) res.register(right_func.execute());
            if (res.error != null)
                return res;
            if (op_tok.type == TT.LSQUARE) {
                if (currentToken.type != TT.RSQUARE) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected closing bracket (']')"
                ));
                advance(); res.registerAdvancement();
            }
            left = new BinOpNode(left, op_tok, right);
        }
        return res.success(left);
    }

    @SuppressWarnings("DuplicatedCode")
    public ParseResult gatherArgs() {
        ParseResult res = new ParseResult();
        List<Token> argNameToks = new ArrayList<>();
        List<Token> argTypeToks = new ArrayList<>();
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

            }


            if (!currentToken.type.equals(TT.GT)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '>'"
            )); advance(); res.registerAdvancement();
        } return res.success(new Pair<>(argNameToks, argTypeToks));
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
        Pair<List<Case>, ElseCase> allCases = (Pair<List<Case>, ElseCase>) res.register(this.ifExprCases("if", true));
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

        Node statements = (Node) res.register(this.block(false));
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
            Pair<List<Case>, ElseCase> allCases = (Pair<List<Case>, ElseCase>) res.register(this.elifExpr(parenthesis));
            if (res.error != null)
                return res;
            cases = allCases.a;
            elseCase = allCases.b;
        } else {
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

            Node statements = (Node) res.register(this.block(false));
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

    public ParseResult whileExpr() {
        ParseResult res = new ParseResult();

        if (!currentToken.matches(TT.KEYWORD, "while")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'while'"
        ));
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT.LPAREN))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '('"
            ));
        res.registerAdvancement(); advance();

        Node condition = (Node) res.register(getClosing());
        if (res.error != null) return res;
        Node body;
        switch (currentToken.type) {
            case EQ:
                res.registerAdvancement(); advance();
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, false));
            case OPEN:
                body = (Node) res.register(block());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, true));
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

        var argTKs = (Pair< List<Token>, List<Token> >) res.register(gatherArgs());
        if (res.error != null) return res;

        Node nodeToReturn;
        switch (currentToken.type) {
            case LAMBDA -> {
                res.registerAdvancement();
                advance();
                nodeToReturn = (Node) res.register(this.expr());
                if (res.error != null) return res;
                return res.success(new FuncDefNode(
                        varNameTok,
                        argTKs.a,
                        argTKs.b,
                        nodeToReturn,
                        true,
                        async
                ));
            }
            case OPEN -> {
                nodeToReturn = (Node) res.register(this.block());
                if (res.error != null) return res;
                return res.success(new FuncDefNode(
                        varNameTok,
                        argTKs.a,
                        argTKs.b,
                        nodeToReturn,
                        false,
                        async
                ));
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

        Pair< List<Token>, List<Token> > argTKs = new Pair<>(new ArrayList<>(), new ArrayList<>());
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
                argTKs = (Pair< List<Token>, List<Token> >) res.register(gatherArgs());
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
                var args = (Pair< List<Token>, List<Token> >) res.register(gatherArgs());

                Node nodeToReturn;
                switch (currentToken.type) {
                    case LAMBDA:
                        res.registerAdvancement(); advance();
                        nodeToReturn = (Node) res.register(this.expr());
                        if (res.error != null) return res;
                        methods.add(new MethDefNode(
                                varNameTok,
                                args.a,
                                args.b,
                                nodeToReturn,
                                true,
                                bin,
                                async
                        )); break;
                    case OPEN:
                         nodeToReturn = (Node) res.register(this.block(false));
                         if (res.error != null) return res;
                         methods.add(new MethDefNode(
                                 varNameTok,
                                 argTKs.a,
                                 argTKs.b,
                                 nodeToReturn,
                                 false,
                                 bin,
                                 async
                         )); break;
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
                argTKs.a,
                argTKs.b,
                ingredientNode,
                methods,
                currentToken.pos_end.copy()
        ));
    }

}
