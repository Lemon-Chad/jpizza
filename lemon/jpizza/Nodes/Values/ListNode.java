package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.PList;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;

import java.util.ArrayList;
import java.util.List;

public class ListNode extends Node {
    public List<Node> elements;

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
