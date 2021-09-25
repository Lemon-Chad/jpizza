package lemon.jpizza.nodes.operations;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.results.RTResult;

import java.util.Arrays;

public class BinOpNode extends Node {
    public final Node left_node;
    public final Token op_tok;
    public final Node right_node;
    public boolean fluctuating = true;

    public BinOpNode(Node left_node, Token op_tok, Node right_node) {
        this.left_node = left_node;
        this.op_tok = op_tok;
        this.right_node = right_node;

        pos_start = left_node.pos_start.copy(); pos_end = right_node.pos_end.copy();
        jptype = Constants.JPType.BinOp;
    }

    public BinOpNode fluctuates(boolean f) {
        fluctuating = f;
        return this;
    }

    public String toString() { return String.format("(%s, %s, %s)", left_node, op_tok, right_node); }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        Pair<Obj, RTError> ret;

        Operations.OP op = Constants.tto.get(op_tok.type);

        if (op_tok.type == Tokens.TT.BITE) {
            boolean leftfailed = false;
            boolean rightfailed = false;
            Obj left = res.register(left_node.visit(inter, context));
            if (res.shouldReturn()) leftfailed = true;
            Obj right = res.register(right_node.visit(inter, context));
            if (res.shouldReturn()) rightfailed = true;
            if (leftfailed || left.jptype == Constants.JPType.Null) {
                return res.success(rightfailed ? new Null() : right);
            } else return res.success(left);
        }

        Obj left = res.register(left_node.visit(inter, context));
        if (res.shouldReturn()) return res;
        Obj right = res.register(right_node.visit(inter, context));
        if (res.shouldReturn()) return res;

        if (Arrays.asList(Tokens.TT.BITAND, Tokens.TT.BITOR, Tokens.TT.BITXOR, Tokens.TT.LEFTSHIFT,
                Tokens.TT.RIGHTSHIFT, Tokens.TT.SIGNRIGHTSHIFT)
                .contains(op_tok.type)) {
            if (left.jptype != Constants.JPType.Number || left.floating) return res.failure(RTError.Type(
                    left.get_start(), left.get_end(),
                    "Left operand must be an integer",
                    context
            ));
            if (right.jptype != Constants.JPType.Number || right.floating) return res.failure(RTError.Type(
                    right.get_start(), right.get_end(),
                    "Right operand must be an integer",
                    context
            ));

            long a = left.number.longValue();
            long b = right.number.longValue();

            return res.success(new Num(switch (op_tok.type) {
                case BITAND -> a & b;
                case BITOR -> a | b;
                case BITXOR -> a ^ b;
                case LEFTSHIFT -> a << b;
                case RIGHTSHIFT -> a >> b;
                case SIGNRIGHTSHIFT -> a >>> b;
                default -> -1;
            }));
        }

        if (op_tok.type == Tokens.TT.GT) {
            ret = right.lt(left);
        }
        else if (op_tok.type == Tokens.TT.GTE) {
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

}
