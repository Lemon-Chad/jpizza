package lemon.jpizza.nodes.operations;

import lemon.jpizza.*;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.values.*;

import java.util.*;

public class BinOpNode extends Node {
    public final Node left_node;
    public final TokenType op_tok;
    public final Node right_node;

    public BinOpNode(Node left_node, TokenType op_tok, Node right_node) {
        this.left_node = left_node;
        this.op_tok = op_tok;
        this.right_node = right_node;

        constant = left_node.constant && right_node.constant;

        pos_start = left_node.pos_start.copy(); pos_end = right_node.pos_end.copy();
        jptype = JPType.BinOp;
    }

    public String toString() { return String.format("(%s, %s, %s)", left_node, op_tok, right_node); }

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
            Node opt = new BinOpNode(left, op_tok, right).setStatic(false);
            if (left.getClass() != right.getClass())
                return opt;
            switch (op_tok) {
                case Ampersand: return new BooleanNode(left.asBoolean() && right.asBoolean(), pos_start, pos_end);
                case Pipe: return new BooleanNode(left.asBoolean() || right.asBoolean(), pos_start, pos_end);

                case TildeAmpersand: return new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a & b
                ), pos_start, pos_end);
                case TildePipe: return new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a | b
                ), pos_start, pos_end);
                case TildeCaret: return new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a ^ b
                ), pos_start, pos_end);
                case LeftTildeArrow: return new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a << b
                ), pos_start, pos_end);
                case TildeTilde: return new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a >> b
                ), pos_start, pos_end);
                case RightTildeArrow: return new NumberNode(VM.bitOp(
                        left.asNumber(),
                        right.asNumber(),
                        (a, b) -> a >>> b
                ), pos_start, pos_end);

                case Plus: {
                    if (left instanceof NumberNode)
                        return new NumberNode(left.asNumber() + right.asNumber(), pos_start, pos_end);
                    else if (left instanceof StringNode)
                        return new StringNode(left.asString() + right.asString(), pos_start, pos_end);
                    else if (left instanceof ListNode) {
                        List<Node> list = new ArrayList<>();
                        list.addAll(left.asList());
                        list.addAll(right.asList());
                        return new ListNode(list, pos_start, pos_end);
                    }
                    else if (left instanceof DictNode) {
                        Map<Node, Node> map = new HashMap<>();
                        map.putAll(left.asMap());
                        map.putAll(right.asMap());
                        return new DictNode(map, pos_start, pos_end);
                    }
                    return opt;
                }
                case Minus: return left instanceof NumberNode ? new NumberNode(left.asNumber() - right.asNumber(), pos_start, pos_end) : opt;
                case Star: return left instanceof NumberNode ? new NumberNode(left.asNumber() * right.asNumber(), pos_start, pos_end) : opt;
                case Slash: return left instanceof NumberNode ? new NumberNode(left.asNumber() / right.asNumber(), pos_start, pos_end) : opt;
                case Caret: return left instanceof NumberNode ? new NumberNode(Math.pow(left.asNumber(), right.asNumber()), pos_start, pos_end) : opt;
                case Percent: return left instanceof NumberNode ? new NumberNode(left.asNumber() % right.asNumber(), pos_start, pos_end) : opt;

                case EqualEqual: return new BooleanNode(left.equals(right), pos_start, pos_end);
                case BangEqual: return new BooleanNode(!left.equals(right), pos_start, pos_end);
                case LeftAngle: return new BooleanNode(left.asNumber() < right.asNumber(), pos_start, pos_end);
                case LessEquals: return new BooleanNode(left.asNumber() <= right.asNumber(), pos_start, pos_end);
                case GreaterEquals: return new BooleanNode(left.asNumber() >= right.asNumber(), pos_start, pos_end);
                case RightAngle: return new BooleanNode(left.asNumber() > right.asNumber(), pos_start, pos_end);

                default: return opt;
            }
        }
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Arrays.asList(left_node, right_node));
    }

    @Override
    public String visualize() {
        switch (op_tok) {
            case Ampersand: return "&&";
            case Pipe: return "||";

            case FatArrow: return "=>";

            case Colon: return ":";

            case TildeAmpersand: return "&";
            case TildePipe: return "|";
            case TildeCaret: return "^";
            case LeftTildeArrow: return "<<";
            case TildeTilde: return ">>";
            case RightTildeArrow: return ">>>";

            case Plus: return "+";
            case Minus: return "-";
            case Star: return "*";
            case Slash: return "/";
            case Caret: return "pow";
            case Percent: return "%";

            case EqualEqual: return "==";
            case BangEqual: return "!=";
            case LeftAngle: return "<";
            case LessEquals: return "<=";
            case GreaterEquals: return ">=";
            case RightAngle: return ">";

            default: return "";
        }
    }
}
