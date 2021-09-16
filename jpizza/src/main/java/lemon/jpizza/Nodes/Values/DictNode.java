package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Dict;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;

import java.util.HashMap;
import java.util.Map;

public class DictNode extends Node {
    public Map<Node, Node> dict;

    public DictNode(Map<Node, Node> dict, Position pos_start, Position pos_end) {
        this.dict = dict;
        this.pos_start = pos_start.copy();
        this.pos_end = pos_end.copy();
        jptype = Constants.JPType.Dict;
    }

    public Object get(Node key) {
        return dict.get(key);
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        Dict dict = new Dict(new HashMap<>());

        Map.Entry<Node, Node>[] entrySet = new Map.Entry[0];
        entrySet = this.dict.entrySet().toArray(entrySet);
        int length = entrySet.length;
        for (int i = 0; i < length; i++) {
            Map.Entry<Node, Node> entry = entrySet[i];
            Obj key = res.register(entry.getKey().visit(inter, context));
            if (res.shouldReturn()) return res;
            Obj value = res.register(entry.getValue().visit(inter, context));
            if (res.shouldReturn()) return res;
            dict.set(key, value);
        } return res.success(dict.set_context(context).set_pos(pos_start, pos_end));
    }

}