package lemon.jpizza.nodes.operations;

import lemon.jpizza.*;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.values.BooleanNode;
import lemon.jpizza.nodes.values.NumberNode;

import java.util.ArrayList;
import java.util.Collections;
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

    @Override
    public Node optimize() {
        if (node.constant && op_tok != TokenType.Tilde) {
            Node node = this.node.optimize();
            switch (op_tok) {
                case Minus: return new NumberNode(-node.asNumber(), node.pos_start, node.pos_end);
                case Tilde: return new NumberNode(VM.bitOp(
                        node.asNumber(),
                        0,
                        (a, b) -> ~a
                ), node.pos_start, node.pos_end);
                case Bang: return new BooleanNode(!node.asBoolean(), node.pos_start, node.pos_end);
                case MinusMinus:
                case PlusPlus: return new NumberNode(node.asNumber() + (op_tok == TokenType.PlusPlus ? 1.0 : -1.0), node.pos_start, node.pos_end);
                default: return node;
            }
        }
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(node));
    }

    @Override
    public String visualize() {
        switch (op_tok) {
            case Minus: return "-";
            case Plus: return "+";
            case DollarSign: return "$";
            case Tilde: return "~";
            case Bang: return "!";
            case MinusMinus: return "--";
            case PlusPlus: return "++";
            default: return "";
        }
    }
}
