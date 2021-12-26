package lemon.jpizza.nodes.operations;

import lemon.jpizza.*;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.values.BooleanNode;
import lemon.jpizza.nodes.values.NumberNode;
import lemon.jpizza.nodes.values.StringNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Tokens.TT;

import java.util.Arrays;
import java.util.List;

public class BinOpNode extends Node {
    public final Node left_node;
    public final TT op_tok;
    public final Node right_node;

    public BinOpNode(Node left_node, TT op_tok, Node right_node) {
        this.left_node = left_node;
        this.op_tok = op_tok;
        this.right_node = right_node;

        constant = left_node.constant && right_node.constant;

        pos_start = left_node.pos_start.copy(); pos_end = right_node.pos_end.copy();
        jptype = JPType.BinOp;
    }

    public String toString() { return String.format("(%s, %s, %s)", left_node, op_tok, right_node); }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        Pair<Obj, RTError> ret;

        Operations.OP op = Constants.tto.get(op_tok);

        if (op_tok == Tokens.TT.BITE) {
            boolean leftfailed = false;
            boolean rightfailed = false;
            Obj left = res.register(inter.visit(left_node, context));
            if (res.shouldReturn()) leftfailed = true;
            Obj right = res.register(inter.visit(right_node, context));
            if (res.shouldReturn()) rightfailed = true;
            if (leftfailed || left.jptype == JPType.Null) {
                return res.success(rightfailed ? new Null() : right);
            }
            else return res.success(left);
        }

        Obj left = res.register(inter.visit(left_node, context));
        if (res.shouldReturn()) return res;
        Obj right = res.register(inter.visit(right_node, context));
        if (res.shouldReturn()) return res;

        if (op_tok == TT.EQ) {
            Pair<Obj, RTError> pair = left.mutate(right);
            if (pair.b != null) return res.failure(pair.b);
            return res.success(pair.a);
        }

        if (Arrays.asList(TT.BITAND, TT.BITOR, TT.BITXOR, TT.LEFTSHIFT,
                TT.RIGHTSHIFT, TT.SIGNRIGHTSHIFT)
                .contains(op_tok)) {
            if (left.jptype != JPType.Number || left.floating()) return res.failure(RTError.Type(
                    left.get_start(), left.get_end(),
                    "Left operand must be an integer",
                    context
            ));
            if (right.jptype != JPType.Number || right.floating()) return res.failure(RTError.Type(
                    right.get_start(), right.get_end(),
                    "Right operand must be an integer",
                    context
            ));

            long a = Double.valueOf(left.number).longValue();
            long b = Double.valueOf(right.number).longValue();

            return res.success(new Num(switch (op_tok) {
                case BITAND -> a & b;
                case BITOR -> a | b;
                case BITXOR -> a ^ b;
                case LEFTSHIFT -> a << b;
                case RIGHTSHIFT -> a >> b;
                case SIGNRIGHTSHIFT -> a >>> b;
                default -> -1;
            }));
        }

        if (op_tok == Tokens.TT.GT) {
            ret = right.lt(left);
        }
        else if (op_tok == Tokens.TT.GTE) {
            ret = right.lte(left);
        }
        else ret = switch (op) {
            case ADD -> left.add(right);
            case SUB -> left.sub(right);
            case MUL -> left.mul(right);
            case DIV -> left.div(right);
            case FASTPOW -> left.fastpow(right);
            case MOD -> left.mod(right);

            case EQ -> left.eq(right);
            case NE -> left.ne(right);
            case LT -> left.lt(right);
            case LTE -> left.lte(right);
            case ALSO -> left.also(right);
            case INCLUDING -> left.including(right);

            case APPEND -> left.append(right);
            case EXTEND -> left.extend(right);
            case POP -> left.pop(right);
            case REMOVE -> left.remove(right);
            case BRACKET -> left.bracket(right);

            case GET -> left.get(right);

            default -> new Pair<>(new Null(), null);
        };
        if (ret.b != null) return res.failure(ret.b);
        return res.success(ret.a != null ? ret.a.set_pos(pos_start, pos_end).set_context(context) : new Null());
    }

    /*
     * Optimizable operands:
     * - & => Return true if both operands are true
     * - | => Return true if either operands are true
     * - ~^ => Run a bitwise XOR on the two operands
     * - ~> => Run a signed right shift on the left operand by the right operand
     * - ~~ => Run a right shift on the left operand by the right operand
     * - <~ => Run a left shift on the left operand by the right operand
     * - <, >, ==, !=, >=, <=
     * - +, -, /, *, ^, %
     */

    @Override
    public Node optimize() {
        if (left_node.constant && right_node.constant) {
            Node left = left_node.optimize();
            Node right = right_node.optimize();
            Node opt = new BinOpNode(left, op_tok, right);
            return switch (op_tok) {
                case AND -> new BooleanNode(left.asBoolean() && right.asBoolean(), pos_start, pos_end);
                case OR -> new BooleanNode(left.asBoolean() || right.asBoolean(), pos_start, pos_end);

                case BITXOR -> new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a ^ b
                ), pos_start, pos_end);
                case LEFTSHIFT -> new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a << b
                ), pos_start, pos_end);
                case RIGHTSHIFT -> new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a >> b
                ), pos_start, pos_end);
                case SIGNRIGHTSHIFT -> new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a >>> b
                ), pos_start, pos_end);

                case PLUS -> {
                    if (left instanceof NumberNode)
                        yield new NumberNode(left.asNumber() + right.asNumber(), pos_start, pos_end);
                    else if (left instanceof StringNode)
                        yield new StringNode(left.asString() + right.asString(), pos_start, pos_end);
                    yield opt;
                }
                case MINUS -> left instanceof NumberNode ? new NumberNode(left.asNumber() - right.asNumber(), pos_start, pos_end) : opt;
                case MUL -> left instanceof NumberNode ? new NumberNode(left.asNumber() * right.asNumber(), pos_start, pos_end) : opt;
                case DIV -> left instanceof NumberNode ? new NumberNode(left.asNumber() / right.asNumber(), pos_start, pos_end) : opt;
                case POWER -> left instanceof NumberNode ? new NumberNode(Math.pow(left.asNumber(), right.asNumber()), pos_start, pos_end) : opt;
                case MOD -> left instanceof NumberNode ? new NumberNode(left.asNumber() % right.asNumber(), pos_start, pos_end) : opt;

                case EQ -> new BooleanNode(left.equals(right), pos_start, pos_end);
                case NE -> new BooleanNode(!left.equals(right), pos_start, pos_end);
                case LT -> new BooleanNode(left.asNumber() < right.asNumber(), pos_start, pos_end);
                case LTE -> new BooleanNode(left.asNumber() <= right.asNumber(), pos_start, pos_end);
                case GTE -> new BooleanNode(left.asNumber() >= right.asNumber(), pos_start, pos_end);
                case GT -> new BooleanNode(left.asNumber() > right.asNumber(), pos_start, pos_end);

                default -> opt;
            };
        }
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(left_node, right_node);
    }

    @Override
    public String visualize() {
        return switch (op_tok) {
            case AND -> "&&";
            case OR -> "||";

            case EQ -> "=>";

            case BITE -> ":";

            case BITAND -> "&";
            case BITOR -> "|";
            case BITXOR -> "^";
            case LEFTSHIFT -> "<<";
            case RIGHTSHIFT -> ">>";
            case SIGNRIGHTSHIFT -> ">>>";

            case PLUS -> "+";
            case MINUS -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case POWER -> "pow";
            case MOD -> "%";

            case EQS -> "==";
            case NE -> "!=";
            case LT -> "<";
            case LTE -> "<=";
            case GTE -> ">=";
            case GT -> ">";

            default -> "";
        };
    }
}
