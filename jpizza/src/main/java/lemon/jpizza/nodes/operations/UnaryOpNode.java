package lemon.jpizza.nodes.operations;

import lemon.jpizza.*;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.values.BooleanNode;
import lemon.jpizza.nodes.values.NumberNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.results.RTResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class UnaryOpNode extends Node {
    public final TokenType op_tok;
    public final Node node;

    public UnaryOpNode(TokenType op_tok, Node node) {
        this.node = node;
        this.op_tok = op_tok;

        constant = node.constant;

        pos_start = node.pos_start.copy(); pos_end = node.pos_end.copy();
        jptype = JPType.UnaryOp;
    }

    public String toString() { return String.format("(%s, %s)", op_tok, node); }

    /*
    * All unary operations:
    * - $ => From Bytes
    * - ~ => Bitwise Complement
    * - - => Negative
    * - ! => Logical Not
    * - -- => Decrement
    * - ++ => Increment
    * - + => Stay the same
     */

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj number = res.register(inter.visit(node, context));
        if (res.shouldReturn()) return res;

        if (op_tok == TokenType.Tilde) {
            if (number.jptype != JPType.Number || number.floating()) return res.failure(RTError.Type(
                    number.get_start(), number.get_end(),
                    "Operand must be an integer",
                    context
            ));

            long n = Double.valueOf(number.number).longValue();

            return res.success(new Num(~n));
        }
        else if (op_tok == TokenType.DollarSign) {
            if (number.jptype != JPType.Bytes) return res.failure(RTError.Type(
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
                        "Internal byte error: " + e,
                        context
                ));
            }
        }

        TokenType opTokType = op_tok;
        Pair<Obj, RTError> ret = switch (opTokType) {
            case Minus -> number.mul(new Num(-1.0));
            case PlusPlus, MinusMinus -> number.add(new Num(opTokType.hashCode() == TokenType.PlusPlus.hashCode() ? 1.0 : -1.0));
            case Bang -> number.invert();
            default -> new Pair<>(number, null);
        };
        if (ret.b != null)
            return res.failure(ret.b);
        number = ret.a;
        return res.success(number.set_pos(node.pos_start, node.pos_end).set_context(context));
    }

    @Override
    public Node optimize() {
        if (node.constant && op_tok != TokenType.Tilde) {
            Node node = this.node.optimize();
            return switch (op_tok) {
                case Minus -> new NumberNode(-node.asNumber(), node.pos_start, node.pos_end);
                case Tilde -> new NumberNode(VM.bitOp(
                        node.asNumber(),
                        0,
                        (a, b) -> ~a
                ), node.pos_start, node.pos_end);
                case Bang -> new BooleanNode(!node.asBoolean(), node.pos_start, node.pos_end);
                case MinusMinus, PlusPlus -> new NumberNode(node.asNumber() + (op_tok == TokenType.PlusPlus ? 1.0 : -1.0), node.pos_start, node.pos_end);
                default -> node;
            };
        }
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(List.of(node));
    }

    @Override
    public String visualize() {
        return switch (op_tok) {
            case Minus -> "-";
            case Plus -> "+";
            case DollarSign -> "$";
            case Tilde -> "~";
            case Bang -> "!";
            case MinusMinus -> "--";
            case PlusPlus -> "++";
            default -> "";
        };
    }
}
