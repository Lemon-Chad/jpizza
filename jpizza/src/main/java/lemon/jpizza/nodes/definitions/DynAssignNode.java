package lemon.jpizza.nodes.definitions;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.List;

public class DynAssignNode extends Node {
    public final Token var_name_tok;
    public final Node value_node;

    public DynAssignNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = JPType.DynAssign;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = (String) var_name_tok.value;

        context.symbolTable.setDyn(varName, value_node);

        return res.success(new Null());
    }

    @Override
    public Node optimize() {
        return new DynAssignNode(var_name_tok, value_node.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return List.of(value_node);
    }

    @Override
    public String visualize() {
        return "macro";
    }
}
