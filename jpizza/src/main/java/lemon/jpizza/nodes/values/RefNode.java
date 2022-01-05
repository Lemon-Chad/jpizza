package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.Ref;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class RefNode extends Node {
    public final Node inner;
    public RefNode(Node inner) {
        this.inner = inner;
        pos_start = inner.pos_start; pos_end = inner.pos_end;
        jptype = JPType.Ref;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        
        Obj inner = res.register(inter.visit(this.inner, context));
        if (res.error != null) return res;

        return res.success(new Ref(inner).set_pos(pos_start, pos_end).set_context(context));
    }

    @Override
    public Node optimize() {
        return new RefNode(inner.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(List.of(inner));
    }

    @Override
    public String visualize() {
        return "&Ref";
    }
}
