package lemon.jpizza.nodes.operations;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.results.RTResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class UnaryOpNode extends Node {
    public final Token op_tok;
    public final Node node;
    public boolean fluctuating = true;

    public UnaryOpNode(Token op_tok, Node node) {
        this.node = node;
        this.op_tok = op_tok;

        pos_start = op_tok.pos_start.copy(); pos_end = node.pos_end.copy();
        jptype = Constants.JPType.UnaryOp;
    }

    public String toString() { return String.format("(%s, %s)", op_tok, node); }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj number = res.register(inter.visit(node, context));
        if (res.shouldReturn()) return res;

        if (op_tok.type == Tokens.TT.BITCOMPL) {
            if (number.jptype != Constants.JPType.Number || number.floating) return res.failure(RTError.Type(
                    number.get_start(), number.get_end(),
                    "Operand must be an integer",
                    context
            ));

            long n = number.number.longValue();

            return res.success(new Num(~n));
        } else if (op_tok.type == Tokens.TT.QUEBACK) {
            if (number.jptype != Constants.JPType.Bytes) return res.failure(RTError.Type(
                    number.get_start(), number.get_end(),
                    "Operand must be bytes",
                    context
            ));
            byte[] bytes = number.arr;
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            try {
                ObjectInputStream is = new ObjectInputStream(in);
                Object obj = is.readObject();
                Obj from = Constants.getFromValue(obj);
                return res.success(from);
            } catch (IOException | ClassNotFoundException e) {
                return res.failure(RTError.Internal(
                        number.get_start(), number.get_end(),
                        "Internal byte error: " + e.toString(),
                        context
                ));
            }
        }

        Tokens.TT opTokType = op_tok.type;
        Pair<Obj, RTError> ret = switch (opTokType) {
            case MINUS -> number.mul(new Num(-1.0));
            case INCR, DECR -> number.add(new Num(opTokType.hashCode() == Tokens.TT.INCR.hashCode() ? 1.0 : -1.0));
            case NOT -> number.invert();
            default -> new Pair<>(number, null);
        };
        if (ret.b != null)
            return res.failure(ret.b);
        number = ret.a;
        return res.success(number.set_pos(node.pos_start, node.pos_end).set_context(context));
    }

}
