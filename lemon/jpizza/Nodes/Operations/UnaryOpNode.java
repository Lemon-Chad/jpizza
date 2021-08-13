package lemon.jpizza.Nodes.Operations;

import lemon.jpizza.*;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Bytes;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Results.RTResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class UnaryOpNode extends Node {
    public Token op_tok;
    public Node node;

    public UnaryOpNode(Token op_tok, Node node) {
        this.node = node;
        this.op_tok = op_tok;

        pos_start = op_tok.pos_start.copy(); pos_end = node.pos_end.copy();
        jptype = Constants.JPType.UnaryOp;
    }

    public String toString() { return String.format("(%s, %s)", op_tok, node); }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj number = res.register(node.visit(inter, context));
        if (res.shouldReturn()) return res;

        if (op_tok.type == Tokens.TT.BITCOMPL) {
            if (number.jptype != Constants.JPType.Number || ((Num) number).floating) return res.failure(new RTError(
                    number.get_start(), number.get_end(),
                    "Operand must be an integer",
                    context
            ));

            long n = Double.valueOf(((Num) number).trueValue()).longValue();

            return res.success(new Num(~n));
        } else if (op_tok.type == Tokens.TT.QUEBACK) {
            if (number.jptype != Constants.JPType.Bytes) return res.failure(new RTError(
                    number.get_start(), number.get_end(),
                    "Operand must be bytes",
                    context
            ));
            byte[] bytes = ((Bytes) number).arr;
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            try {
                ObjectInputStream is = new ObjectInputStream(in);
                Object obj = is.readObject();
                Obj from = Constants.getFromValue(obj);
                return res.success(from);
            } catch (IOException | ClassNotFoundException e) {
                return res.failure(new RTError(
                        number.get_start(), number.get_end(),
                        "Internal byte error: " + e.toString(),
                        context
                ));
            }
        }


        Tokens.TT opTokType = op_tok.type;
        Pair<Obj, RTError> ret = switch (opTokType) {
            case MINUS -> (Pair<Obj, RTError>) number.getattr(Operations.OP.MUL, new Num(-1.0));
            case INCR, DECR -> (Pair<Obj, RTError>) number.getattr(Operations.OP.ADD,
                    new Num(opTokType.hashCode() == Tokens.TT.INCR.hashCode() ? 1.0 : -1.0));
            case NOT -> (Pair<Obj, RTError>) number.getattr(Operations.OP.INVERT);
            default -> new Pair<>(number, null);
        };
        if (ret.b != null)
            return res.failure(ret.b);
        number = ret.a;
        return res.success(number.set_pos(node.pos_start, node.pos_end).set_context(context));
    }

}
