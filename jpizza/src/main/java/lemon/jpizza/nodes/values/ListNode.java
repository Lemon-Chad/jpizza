package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.PList;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;

import java.util.ArrayList;
import java.util.List;

public class ListNode extends Node {
    public final List<Node> elements;

    public ListNode(List<Node> element_nodes, Position pos_start, Position pos_end) {
        elements = element_nodes;
        this.pos_start = pos_start.copy(); this.pos_end = pos_end.copy();
        jptype = Constants.JPType.List;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        ArrayList<Obj> elements = new ArrayList<>();
        Object[] nodeElements = this.elements.toArray();
        int length = nodeElements.length;
        for (int i = 0; i < length; i++) {
            elements.add(res.register(((Node) nodeElements[i]).visit(inter, context)));
            if (res.shouldReturn())
                return res;
        } return res.success(new PList(elements).set_context(context).set_pos(pos_start, pos_end));
    }

}
