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
import lemon.jpizza.Double;

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
        if (res.error == null && !currentToken.type.equals(TT_EOF)) {
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
        while (currentToken.type.equals(TT_NEWLINE)) {
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
            while (currentToken.type.equals(TT_NEWLINE)) {
                res.registerAdvancement();
                advance();
                newlineCount++;
            }
            if (newlineCount == 0)
                moreStatements = false;
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

        if (!currentToken.type.equals(TT_NEWLINE)) {
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

        if (currentToken.matches(TT_KEYWORD, "return")) {
            res.registerAdvancement();
            advance();
            Node expr = (Node) res.try_register(this.expr());
            if (expr == null)
                reverse(res.toReverseCount);
            return res.success(new ReturnNode(expr, pos_start, currentToken.pos_end.copy()));
        } if (currentToken.matches(TT_KEYWORD, "continue")) {
            res.registerAdvancement();
            advance();
            return res.success(new ContinueNode(pos_start, currentToken.pos_end.copy()));
        } if (currentToken.matches(TT_KEYWORD, "break")) {
            res.registerAdvancement();
            advance();
            return res.success(new BreakNode(pos_start, currentToken.pos_end.copy()));
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

        if (!currentToken.type.equals(TT_IDENTIFIER))
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
        if (currentToken.matches(TT_KEYWORD, "attr")) {
            Token var_name = (Token) res.register(extractVarTok());
            if (res.error != null) return res;

            if (!currentToken.type.equals(TT_EQ))
                return res.success(new AttrAccessNode(var_name));

            res.registerAdvancement();
            advance();

            Node expr = (Node) res.register(this.expr());
            if (res.error != null)
                return res;
            return res.success(new AttrAssignNode(var_name, expr));
        }
        if (currentToken.matches(TT_KEYWORD, "var") || currentToken.matches(TT_KEYWORD, "bake")) {
            boolean locked = currentToken.value.equals("bake");
            Token var_name = (Token) res.register(extractVarTok());
            if (res.error != null) return res;

            if (!currentToken.type.equals(TT_EQ))
                return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '=>'"
                ));

            res.registerAdvancement();
            advance();
            Node expr = (Node) res.register(this.expr());
            if (res.error != null)
                return res;
            return res.success(new VarAssignNode(var_name, expr, locked));
        }
        if (currentToken.type.equals(TT_IDENTIFIER)) {
            Token var_tok = currentToken;
            advance();
            res.registerAdvancement();
            if (Arrays.asList(TT_POE, TT_PLE, TT_MUE, TT_DIE, TT_MIE).contains(currentToken.type)) {
                Token op_tok = currentToken;
                advance();
                res.registerAdvancement();
                Node value = (Node) res.register(this.expr());
                if (res.error != null)
                    return res;
                return res.success(new VarAssignNode(var_tok, new BinOpNode(
                        new VarAccessNode(var_tok),
                        new Token(switch (op_tok.type) {
                                    case TT_POE -> TT_POWER;
                                    case TT_MUE -> TT_MUL;
                                    case TT_DIE -> TT_DIV;
                                    case TT_PLE -> TT_PLUS;
                                    case TT_MIE -> TT_MINUS;
                                    default -> null;
                                }, op_tok.pos_start.copy(), op_tok.pos_end
                        ), value
                ), false));
            }
            if (currentToken.type.equals(TT_INCR) || currentToken.type.equals(TT_DECR)) {
                Token op_tok = currentToken;
                res.registerAdvancement();
                advance();
                return res.success(new VarAssignNode(var_tok, new UnaryOpNode(
                        op_tok,
                        new VarAccessNode(var_tok)
                ), false));
            } reverse();
        }
        Node node = (Node) res.register(binOp(this::getExpr, Collections.singletonList(TT_DOT)));

        if (res.error != null)
            return res;
            /*return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected keyword, int, bool, float, identifier, '+', '-', '*', '/', '^', '!', '?', 'for'," +
                            " 'while' or '('"
            ));*/

        return res.success(node);
    }

    public ParseResult factor() {
        ParseResult res = new ParseResult();
        Token tok = currentToken;

        if (Arrays.asList(TT_PLUS, TT_MINUS, TT_INCR, TT_DECR).contains(tok.type)) {
            res.registerAdvancement();
            advance();
            Node factor = (Node) res.register(this.factor());
            if (res.error != null)
                return res;
            return res.success(new UnaryOpNode(tok, factor));
        } return pow();
    }

    public ParseResult atom() {
        ParseResult res = new ParseResult();
        Token tok = currentToken;

        if (tok.type.equals(TT_KEYWORD)) {
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

                case "null":
                    res.registerAdvancement(); advance();
                    return res.success(new NullNode(tok));

                case "for":
                    Node forExpr = (Node) res.register(this.forExpr());
                    if (res.error != null)
                        return res;
                    return res.success(forExpr);

                case "function":
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
                    if (!file_name_tok.type.equals(TT_IDENTIFIER))
                        return res.failure(Error.InvalidSyntax(
                                file_name_tok.pos_start.copy(), file_name_tok.pos_end.copy(),
                                "Expected module name"
                        ));
                    advance(); res.registerAdvancement();
                    return res.success(new ImportNode(file_name_tok));

                case "attr":
                    advance(); res.registerAdvancement();
                    Token var_name_tok = currentToken;
                    if (!currentToken.type.equals(TT_IDENTIFIER))
                        return res.failure(Error.InvalidSyntax(
                                var_name_tok.pos_start.copy(), var_name_tok.pos_end.copy(),
                                "Expected identifier"
                        ));
                    advance(); res.registerAdvancement();
                    return res.success(new AttrAccessNode(var_name_tok));

                default:
                    break;
            }

        }
        else if (Arrays.asList(TT_INT, TT_FLOAT).contains(tok.type)) {
            res.registerAdvancement(); advance();
            return res.success(new NumberNode(tok));
        }
        else if (tok.type.equals(TT_STRING)) {
            res.registerAdvancement(); advance();
            return res.success(new StringNode(tok));
        }
        else if (tok.type.equals(TT_IDENTIFIER)) {
            res.registerAdvancement(); advance();
            if (currentToken.type.equals(TT_CLACCESS)) {
                res.registerAdvancement(); advance();
                Token origin = currentToken;
                if (!origin.type.equals(TT_IDENTIFIER))
                    return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "Expected identifier"
                    ));
                advance(); res.registerAdvancement();
                return res.success(new ClaccessNode(
                        new VarAccessNode(tok),
                        origin
                ));
            } return res.success(new VarAccessNode(tok));
        }
        else if (tok.type.equals(TT_BOOL)) {
            res.registerAdvancement(); advance();
            return res.success(new BooleanNode(tok));
        }
        else if (tok.type.equals(TT_QUERY)) {
            Node queryExpr = (Node) res.register(this.queryExpr());
            if (res.error != null)
                return res;
            return res.success(queryExpr);
        }
        else if (tok.type.equals(TT_LSQUARE)) {
            Node listExpr = (Node) res.register(this.listExpr());
            if (res.error != null)
                return res;
            return res.success(listExpr);
        }
        else if (tok.type.equals(TT_OPEN)) {
            Node dictExpr = (Node) res.register(this.dictExpr());
            if (res.error != null)
                return res;
            return res.success(dictExpr);
        }
        else if (tok.type.equals(TT_LPAREN)) {
            res.registerAdvancement(); advance();
            Node expr = (Node) res.register(this.expr());
            if (res.error != null)
                return res;
            if (currentToken.type.equals(TT_RPAREN)) {
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
                "Expected int, float, identifier, '+', '-', or '('"
        ));
    }

    public ParseResult call() {
        ParseResult res = new ParseResult();
        Node atom = (Node) res.register(this.accessExpr());
        if (res.error != null)
            return res;
        if (currentToken.type.equals(TT_LPAREN)) {
            res.registerAdvancement();
            advance();
            List<Node> arg_nodes = new ArrayList<>();
            if (!currentToken.type.equals(TT_RPAREN)) {
                arg_nodes.add((Node) res.register(this.expr()));
                if (res.error != null)
                    return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "Expected argument"
                    ));

                while (currentToken.type.equals(TT_COMMA)) {
                    res.registerAdvancement();
                    advance();
                    arg_nodes.add((Node) res.register(this.expr()));
                    if (res.error != null)
                        return res;
                }

                if (!currentToken.type.equals(TT_RPAREN))
                    return res.failure(Error.InvalidSyntax(
                            currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                            "Expected ',' or ')'"
                    ));
            }
            res.registerAdvancement();
            advance();
            return res.success(new CallNode(
                    atom,
                    arg_nodes
            ));
        }
        return res.success(atom);
    }

    public ParseResult pow() { return binOp(this::call, Arrays.asList(TT_POWER, TT_MOD), this::factor); }

    public ParseResult term() { return binOp(this::factor, Arrays.asList(TT_MUL, TT_DIV)); }

    public ParseResult arithExpr() { return binOp(this::term, Arrays.asList(TT_PLUS, TT_MINUS)); }

    public ParseResult accessExpr() {
        ParseResult res = new ParseResult();
        Node node = (Node) res.register(atom());

        if (res.error != null) return res;
        if (currentToken.type.equals(TT_CLACCESS)) {
            advance(); res.registerAdvancement();
            if (!currentToken.type.equals(TT_IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected identifier"
            )); Token tok = currentToken;
            advance(); res.registerAdvancement();
            return res.success(new ClaccessNode(node, tok));
        } return res.success(node);

    }

    public ParseResult compExpr() {
        ParseResult res = new ParseResult();
        if (currentToken.type.equals(TT_NOT)) {
            Token op_tok = currentToken;
            res.registerAdvancement();
            advance();

            Node node = (Node) res.register(compExpr());
            if (res.error != null)
                return res;
            return res.success(new UnaryOpNode(op_tok, node));
        }
        Node node = (Node) res.register(binOp(this::arithExpr, Arrays.asList(TT_EE, TT_NE, TT_LT, TT_GT, TT_LTE, TT_GTE)));

        if (res.error != null)
            return res;
        return res.success(node);
    }

    public ParseResult getExpr() {
        ParseResult res = new ParseResult();
        Node node = (Node) res.register(binOp(this::compExpr, Arrays.asList(TT_AND, TT_OR)));
        if (res.error != null)
            return res;
        return res.success(node);
    }

    public ParseResult binOp(L left_func, List<String> ops) {
        return binOp(left_func, ops, null);
    }

    public ParseResult binOp(L left_func, List<String> ops, L right_func) {
        ParseResult res = new ParseResult();
        if (right_func == null)
            right_func = left_func;
        Node right; Node left;
        try {
            left = (Node) res.register(left_func.execute());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        while (ops.contains(currentToken.type)) {
            Token op_tok = currentToken;
            res.registerAdvancement();
            advance();
            try {
                right = (Node) res.register(right_func.execute());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            if (res.error != null)
                return res;
            left = new BinOpNode(left, op_tok, right);
        }
        return res.success(left);
    }

    public ParseResult gatherArgs() {
        ParseResult res = new ParseResult();
        List<Token> argNameToks = new ArrayList<>();
        if (currentToken.type.equals(TT_LT)) {
            advance(); res.registerAdvancement();

            if (!currentToken.type.equals(TT_IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected identifier or ommited <>"
            ));
            argNameToks.add(currentToken);
            res.registerAdvancement(); advance();

            while (currentToken.type.equals(TT_COMMA)) {
                res.registerAdvancement(); advance();

                if (!currentToken.type.equals(TT_IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected identifier"
                ));
                argNameToks.add(currentToken);
                res.registerAdvancement(); advance();
            }


            if (!currentToken.type.equals(TT_GT)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '>'"
            )); advance(); res.registerAdvancement();
        } return res.success(argNameToks);
    }

    public ParseResult block() {
        ParseResult res = new ParseResult();
        if (!currentToken.type.equals(TT_OPEN))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '{'"
            ));

        res.registerAdvancement(); advance();

        Node statements = (Node) res.register(this.statements());
        if (res.error != null)
            return res;

        if (!currentToken.type.equals(TT_CLOSE))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected '}'"
            ));

        res.registerAdvancement(); advance();

        return res.success(statements);
    }

    // EXPRESSIONS

    // If Parts

    public ParseResult ifExpr() {
        ParseResult res = new ParseResult();
        Double allCases = (Double) res.register(this.ifExprCases("if"));
        if (res.error != null)
            return res;
        @SuppressWarnings("unchecked") List<Case> cases = (List<Case>) allCases.get(0);
        ElseCase elseCase = (ElseCase) allCases.get(1);
        return res.success(new QueryNode(cases, elseCase));
    }

    public ParseResult ifExprCases(String caseKeyword) {
        ParseResult res = new ParseResult();
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TT_KEYWORD, caseKeyword))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    String.format("Expected %s", caseKeyword)
            ));

        res.registerAdvancement(); advance();

        Node condition = (Node) res.register(this.expr());
        if (res.error != null)
            return res;

        Node statements = (Node) res.register(this.block());
        if (res.error != null)
            return res;
        cases.add(new Case(condition, statements, true));

        Double allCases = (Double) res.register(this.elifElse());
        @SuppressWarnings("unchecked") List<Case> newCases = (List<Case>) allCases.get(0);
        ElseCase elseCase = (ElseCase) allCases.get(1);
        cases.addAll(newCases);

        return res.success(new Double(cases, elseCase));
    }

    public ParseResult elifElse() {
        ParseResult res = new ParseResult();
        List<Case> cases = new ArrayList<>();
        ElseCase elseCase;

        if (currentToken.matches(TT_KEYWORD, "elif")) {
            Double allCases = (Double) res.register(this.elifExpr());
            if (res.error != null)
                return res;
            //noinspection unchecked
            cases = (List<Case>) allCases.get(0);
            elseCase = (ElseCase) allCases.get(1);
        } else {
            elseCase = (ElseCase) res.register(this.elseExpr());
            if (res.error != null)
                return res;
        } return res.success(
                new Double(cases, elseCase)
        );

    }

    public ParseResult elifExpr() {
        return ifExprCases("elif");
    }

    public ParseResult elseExpr() {
        ParseResult res = new ParseResult();
        ElseCase elseCase = null;

        if (currentToken.matches(TT_KEYWORD, "else")) {
            res.registerAdvancement(); advance();

            Node statements = (Node) res.register(this.block());
            if (res.error != null)
                return res;
            elseCase = new ElseCase(statements, true);
        }

        return res.success(elseCase);
    }

    // Query

    public ParseResult kv() {
        ParseResult res = new ParseResult();
        if (!currentToken.type.equals(TT_BITE)) return res.failure(Error.InvalidSyntax(
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

        if (!currentToken.type.equals(TT_QUERY)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '?'"
        ));

        ParseResult r;
        r = getStatement.execute();
        if (r != null) return r;

        while (currentToken.type.equals(TT_QUEBACK)) {
            r = getStatement.execute();
            if (r != null) return r;
        }

        if (currentToken.type.equals(TT_DEFAULTQUE)) {
            res.registerAdvancement(); advance();
            if (!currentToken.type.equals(TT_BITE)) return res.failure(Error.InvalidSyntax(
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

        if (!currentToken.matches(TT_KEYWORD, "for"))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected 'for'"
            ));

        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT_IDENTIFIER))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected 'identifier'"
            ));

        Token varName = currentToken;
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT_LAMBDA))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected weak assignment ('->')"
            ));
        res.registerAdvancement(); advance();

        Node start = (Node) res.register(this.expr());
        if (res.error != null) return res;

        if (!currentToken.type.equals(TT_BITE))
            return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Expected ':'"
            ));
        res.registerAdvancement(); advance();

        Node end = (Node) res.register(this.expr());
        if (res.error != null) return res;

        Node step;
        if (currentToken.type.equals(TT_STEP)) {
            res.registerAdvancement(); advance();
            step = (Node) res.register(this.expr());
        } else step = null;

        Node body;
        switch (currentToken.type) {
            case TT_OPEN:
                body = (Node) res.register(this.block());
                if (res.error != null) return res;
                return res.success(new ForNode(varName, start, end, step, body, true));
            case TT_EQ:
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

    public ParseResult whileExpr() {
        ParseResult res = new ParseResult();

        if (!currentToken.matches(TT_KEYWORD, "while")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'while'"
        ));
        res.registerAdvancement(); advance();

        Node condition = (Node) res.register(this.expr());
        if (res.error != null) return res;

        Node body;
        switch (currentToken.type) {
            case TT_EQ:
                res.registerAdvancement(); advance();
                body = (Node) res.register(this.statement());
                if (res.error != null) return res;
                return res.success(new WhileNode(condition, body, false));
            case TT_OPEN:
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

        if (!currentToken.type.equals(TT_LSQUARE)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '['"
        ));
        res.registerAdvancement(); advance();

        if (!currentToken.type.equals(TT_RSQUARE)) {
            elementNodes.add((Node) res.register(this.expr()));
            if (res.error != null) return res;

            while (currentToken.type.equals(TT_COMMA)) {
                res.registerAdvancement();
                advance();
                elementNodes.add((Node) res.register(this.expr()));
                if (res.error != null) return res;
            }
            if (!currentToken.type.equals(TT_RSQUARE)) return res.failure(Error.InvalidSyntax(
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
        Map<Object, Object> dict = new HashMap<>();
        Position pos_start = currentToken.pos_start.copy();

        if (!currentToken.type.equals(TT_OPEN)) return res.failure(Error.InvalidSyntax(
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
        if (!currentToken.type.equals(TT_CLOSE)) {
            x = kv.execute();
            if (x != null) return x;
        }

        while (currentToken.type.equals(TT_COMMA)) {
            advance(); res.registerAdvancement();
            x = kv.execute();
            if (x != null) return x;
        }
        if (!currentToken.type.equals(TT_CLOSE)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '}'"
        )); res.registerAdvancement(); advance();

        return res.success(new DictNode(dict, pos_start, currentToken.pos_end.copy()));
    }

    // Executables

    public ParseResult funcDef() {
        ParseResult res = new ParseResult();

        if (!currentToken.matches(TT_KEYWORD, "function")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'function'"
        )); advance(); res.registerAdvancement();

        boolean async = false;
        if (currentToken.matches(TT_KEYWORD, "async")) {
            async = true;
            advance(); res.registerAdvancement();
        }

        Token varNameTok = null;
        if (currentToken.type.equals(TT_IDENTIFIER)) {
            varNameTok = currentToken;
            res.registerAdvancement(); advance();
        }

        @SuppressWarnings("unchecked") List<Token> argNameToks = (List<Token>) res.register(gatherArgs());
        if (res.error != null) return res;

        Node nodeToReturn;
        switch (currentToken.type) {
            case TT_LAMBDA:
                res.registerAdvancement(); advance();
                nodeToReturn = (Node) res.register(this.expr());
                if (res.error != null) return res;
                return res.success(new FuncDefNode(
                        varNameTok,
                        argNameToks,
                        nodeToReturn,
                        true,
                        async
                ));
            case TT_OPEN:
                nodeToReturn = (Node) res.register(this.block());
                if (res.error != null) return res;
                return res.success(new FuncDefNode(
                        varNameTok,
                        argNameToks,
                        nodeToReturn,
                        false,
                        async
                ));
            default:
                return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected '->' or '{'"
                ));
        }

    }

    @SuppressWarnings("unchecked")
    public ParseResult classDef() {
        ParseResult res = new ParseResult();

        if (!currentToken.matches(TT_KEYWORD, "recipe")) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected 'recipe'"
        )); advance(); res.registerAdvancement();
        if (!currentToken.type.equals(TT_IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected identifier"
        ));
        Token classNameTok = currentToken;
        res.registerAdvancement(); advance();


        if (!currentToken.type.equals(TT_OPEN)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '{'"
        )); res.registerAdvancement(); advance();

        List<Token> attributeDeclarations = new ArrayList<>();
        if (currentToken.type.equals(TT_IDENTIFIER)) {
            attributeDeclarations.add(currentToken);
            advance(); res.registerAdvancement();
            while (currentToken.type.equals(TT_COMMA)) {
                res.registerAdvancement(); advance();
                if (!currentToken.type.equals(TT_IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected identifier"
                ));
                attributeDeclarations.add(currentToken);
                advance(); res.registerAdvancement();
            }
            if (!currentToken.type.equals(TT_NEWLINE)) return res.failure(Error.InvalidSyntax(
                    currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                    "Missing semicolon"
            )); advance(); res.registerAdvancement();
        }

        List<Token> argNameToks = new ArrayList<>();
        Node ingredientNode = new ListNode(
                new ArrayList<>(),
                classNameTok.pos_start.copy(),
                classNameTok.pos_end.copy()
        );
        List<MethDefNode> methods = new ArrayList<>();
        while (currentToken.type.equals(TT_KEYWORD) &&
                (currentToken.value.equals("method") || currentToken.value.equals("ingredients"))) {
            if (currentToken.value.equals("ingredients")) {
                advance(); res.registerAdvancement();
                argNameToks = (List<Token>) res.register(gatherArgs()); if (res.error != null) return res;
                ingredientNode = (Node) res.register(this.block()); if (res.error != null) return res;
            }
            else if (currentToken.value.equals("method")) {
                res.registerAdvancement(); advance();

                boolean bin = false; boolean async = false;
                while (currentToken.type.equals(TT_KEYWORD) &&
                        (currentToken.value.equals("bin") || currentToken.value.equals("async"))) {
                    if (currentToken.value.equals("bin")) bin = true;
                    else async = true;
                    advance(); res.registerAdvancement();
                }

                if (!currentToken.type.equals(TT_IDENTIFIER)) return res.failure(Error.InvalidSyntax(
                        currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                        "Expected identifier"
                ));
                Token varNameTok = currentToken;
                res.registerAdvancement(); advance();
                List<Token> args = (List<Token>) res.register(gatherArgs());

                Node nodeToReturn;
                switch (currentToken.type) {
                    case TT_LAMBDA:
                        res.registerAdvancement(); advance();
                        nodeToReturn = (Node) res.register(this.expr());
                        if (res.error != null) return res;
                        methods.add(new MethDefNode(
                                varNameTok,
                                args,
                                nodeToReturn,
                                true,
                                bin,
                                async
                        )); break;
                    case TT_OPEN:
                         nodeToReturn = (Node) res.register(this.block());
                         if (res.error != null) return res;
                         methods.add(new MethDefNode(
                                 varNameTok,
                                 args,
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
        if (!currentToken.type.equals(TT_CLOSE)) return res.failure(Error.InvalidSyntax(
                currentToken.pos_start.copy(), currentToken.pos_end.copy(),
                "Expected '}'"
        )); advance(); res.registerAdvancement();
        return res.success(new ClassDefNode(
                classNameTok,
                attributeDeclarations,
                argNameToks,
                ingredientNode,
                methods,
                currentToken.pos_end.copy()
        ));
    }

}
