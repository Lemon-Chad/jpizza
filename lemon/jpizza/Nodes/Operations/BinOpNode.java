package lemon.jpizza.Nodes.Operations;

import lemon.jpizza.*;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Results.RTResult;

import java.util.Arrays;

public class BinOpNode extends Node {
    public Node left_node;
    public Token op_tok;
    public Node right_node;
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

        Obj left = res.register(left_node.visit(inter, context));
        if (res.shouldReturn()) return res;
        Obj right = res.register(right_node.visit(inter, context));
        if (res.shouldReturn()) return res;

        /* Pair3<Obj, Operations.OP, Obj> pair = new Pair3<>(
                left.set_context().set_pos(),
                op,
                right.set_context().set_pos()
        );

        if (Interpreter.compCache.containsKey(pair))
            return res.success(Interpreter.compCache.get(new Pair3<>(left, op, right)));
         */

        if (Arrays.asList(Tokens.TT.BITAND, Tokens.TT.BITOR, Tokens.TT.BITXOR, Tokens.TT.LEFTSHIFT,
                Tokens.TT.RIGHTSHIFT, Tokens.TT.SIGNRIGHTSHIFT)
                .contains(op_tok.type)) {
            if (left.jptype != Constants.JPType.Number || ((Num) left).floating) return res.failure(new RTError(
                    left.get_start(), left.get_end(),
                    "Left operand must be an integer",
                    context
            ));
            if (right.jptype != Constants.JPType.Number || ((Num) right).floating) return res.failure(new RTError(
                    right.get_start(), right.get_end(),
                    "Right operand must be an integer",
                    context
            ));

            long a = Double.valueOf(((Num) left).trueValue()).longValue();
            long b = Double.valueOf(((Num) right).trueValue()).longValue();

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

        if (op_tok.type == Tokens.TT.GT || op_tok.type == Tokens.TT.GTE) {
            ret = (Pair<Obj, RTError>) right.getattr(op_tok.type == Tokens.TT.GT ? Operations.OP.LT : Operations.OP.LTE,
                    left);
        }
        else ret = (Pair<Obj, RTError>) left.getattr(op, right);
        if (ret.b != null) return res.failure(ret.b);
        return res.success(ret.a.set_pos(pos_start, pos_end).set_context(context));
    }

}
