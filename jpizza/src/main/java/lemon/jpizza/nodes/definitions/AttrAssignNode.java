package lemon.jpizza.nodes.definitions;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.List;

public class AttrAssignNode extends Node {
    public final Token var_name_tok;
    public final Node value_node;

    public AttrAssignNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = JPType.AttrAssign;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = (String) var_name_tok.value;
        Obj value = res.register(inter.visit(value_node, context));
        if (res.shouldReturn()) return res;

        RTError.ErrorDetails v = context.symbolTable.setattr(varName, value);
        if (v != null) return res.failure(v.build(
                pos_start, pos_end,
                context
        ));

        return res.success(value);
    }

    @Override
    public Node optimize() {
        return new AttrAssignNode(var_name_tok, value_node.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return List.of(value_node);
    }

    @Override
    public String visualize() {
        return "attr " + var_name_tok.value;
    }
}
