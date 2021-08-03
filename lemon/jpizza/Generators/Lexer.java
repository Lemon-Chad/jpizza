package lemon.jpizza.Generators;

import lemon.jpizza.Pair;
import lemon.jpizza.Errors.Error;
import lemon.jpizza.Position;
import lemon.jpizza.Token;

import java.util.*;

import static lemon.jpizza.Errors.Error.ExpectedCharError;
import static lemon.jpizza.Errors.Error.IllegalCharError;
import static lemon.jpizza.Tokens.*;
import static lemon.jpizza.Constants.*;

public class Lexer {
    String text;
    Position pos;
    String currentChar;

    public Lexer(String fn, String text) {
        this.text = text;
        this.pos = new Position(-1, 0, -1, fn, text);
        currentChar = null;
        advance();
    }

    public void advance() {
        pos.advance(currentChar == null ? ' ' : currentChar.charAt(0));
        currentChar = pos.idx < text.length() ? String.valueOf(text.charAt(pos.idx)) : null;
    }

    public String next(int i) {
        return pos.idx + i < text.length() ? String.valueOf(text.charAt(pos.idx + i)) : null;
    }
    public String next() {
        return next(1);
    }

    public void skip_comment() {
        advance();

        while (currentChar != null && !currentChar.equals(";") && currentChar.matches(".")) {
            advance();
        }

        advance();
    }

    public void skip_multiline_comment() {
        advance();

        while (currentChar != null && next() != null && !(currentChar + next()).equals("<<")) {
            advance();
        }

        advance(); advance();
    }

    public Pair<List<Token>, Error> make_tokens() {
        List<Token> tokens = new ArrayList<>();
        while (currentChar != null) {
            if (!currentChar.matches(".") || Character.isWhitespace(currentChar.toCharArray()[0])) {
                advance();
            } else if (next() != null && (currentChar + next()).equals("<>")) {
                skip_comment();
            } else if (next() != null && (currentChar + next()).equals("<<")) {
                skip_multiline_comment();
            } else if ("\"'".contains(currentChar)) {
                tokens.add(make_string(currentChar));
            } else if (currentChar.equals("}")) {
                tokens.add(new Token(TT.CLOSE, pos));
                advance();
            } else if (next() != null && TOKEY.containsKey(currentChar + next())) {
                tokens.add(new Token(TOKEY.get(currentChar + next()), pos, pos.copy().advance().advance()));
                advance(); advance();
            } else if (TOKEY.containsKey(currentChar)) {
                tokens.add(new Token(TOKEY.get(currentChar), pos));
                advance();
            } else if (String.valueOf(LETTERS).contains(currentChar)) {
                tokens.add(make_identifier());
            } else if (String.valueOf(NUMBERS).contains(currentChar)) {
                tokens.add(make_number());
            } else if (currentChar.equals("!")) {
                int nextdex = 1;
                while (next(nextdex) != null && Character.isWhitespace(next(nextdex).charAt(0)))
                    nextdex++;
                if (next(nextdex).equals("<") || next(nextdex).equals("{") ||
                        (next(nextdex) + next(nextdex + 1)).equals("->")) {
                    tokens.add(new Token(TT.KEYWORD, "fn", pos.copy(), pos.copy().advance()));
                    advance();
                } else if (currentChar.equals("!")){
                    @SuppressWarnings("DuplicatedCode") Pair<Token, Error> d = make_equals_expr();
                    Token tok = d.a; Error error = d.b;
                    if (error != null) {
                        return new Pair<>(
                                new ArrayList<>(),
                                error
                        );
                    } tokens.add(tok);
                }
            } else if ("<>=".contains(currentChar)) {
                Pair<Token, Error> d = make_equals_expr();
                Token tok = d.a; Error error = d.b;
                if (error != null) {
                    return new Pair<>(
                            new ArrayList<>(),
                            error
                    );
                } tokens.add(tok);
            } else {
                String c = currentChar;
                Position p = pos.copy();
                advance();
                return new Pair<>(
                        new ArrayList<>(),
                        IllegalCharError(p, pos, String.format("'%s'", c))
                );
            }
        }
        tokens.add(new Token(TT.EOF, pos));
        return new Pair<>(
                tokens,
                null
        );
    }

    public Token make_string(String q) {
        StringBuilder string = new StringBuilder();
        Position pos_start = pos.copy();
        boolean escaped = false;
        advance();

        Map<String, String> escapeCharacters = new HashMap<>() {{
            put("n", "\n");
            put("t", "\t");
        }};

        while (currentChar != null && (!currentChar.equals(q) || escaped)) {
            if (escaped) {
                if (escapeCharacters.containsKey(currentChar)) {
                    string.append(escapeCharacters.get(currentChar));
                } else {
                    string.append(currentChar);
                } escaped = false;
            }
            else if (currentChar.equals("\\")) {
                escaped = true;
            } else {
                string.append(currentChar);
            } advance();
        }

        advance();
        return new Token(TT.STRING, string.toString(), pos_start, pos);
    }

    public Pair<Token, Error> make_equals_expr() {
        Position pos_start = pos.copy();
        String c = currentChar;
        advance();

        if (currentChar != null && currentChar.equals("=")) {
            advance();

            return new Pair<>(
                    new Token(switch (c) {
                        case "!" -> TT.NE;
                        case ">" -> TT.GTE;
                        case "<" -> TT.LTE;
                        case "=" -> TT.EE;
                        default -> null;
                    }, pos_start),
                    null
            );
        } else if (c.equals("=")) {
            advance();
            return new Pair<>(
                    null,
                    ExpectedCharError(pos_start, pos, String.format("'=' (after '%s')", c))
            );
        } else {
            return new Pair<>(
                    new Token(switch (c) {
                        case ">" -> TT.GT;
                        case "<" -> TT.LT;
                        case "!" -> TT.NOT;
                        default -> null;
                    }, pos_start),
                    null
            );
        }
    }

    public Token make_identifier() {
        StringBuilder id_strb = new StringBuilder();
        Position pos_start = pos.copy();

        while (currentChar != null && String.valueOf(LETTERS_DIGITS).contains(currentChar)) {
            id_strb.append(currentChar);
            advance();
        } String id_str = id_strb.toString();

        if (id_str.equals("true") || id_str.equals("false")) {
            return new Token(TT.BOOL, id_str.equals("true"), pos_start, pos);
        }

        TT tok_type = Arrays.asList(KEYWORDS).contains(id_str) ? TT.KEYWORD : TT.IDENTIFIER;
        return new Token(tok_type, id_str, pos_start, pos);
    }

    public Token make_number() {
        StringBuilder num = new StringBuilder();
        int dot_count = 0;
        Position pos_start = pos.copy();

        while (currentChar != null && String.valueOf(NUMDOT).contains(currentChar)) {
            if (currentChar.equals(".")) {
                if (dot_count == 1)
                    break;
                dot_count++;
            } num.append(currentChar);
            advance();
        }

        if (dot_count == 0)
            return new Token(TT.INT, Double.valueOf(num.toString()), pos_start, pos);
        return new Token(TT.FLOAT, Double.valueOf(num.toString()), pos_start, pos);

    }

}
