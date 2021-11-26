package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Dict;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DictNode extends Node {
    public final Map<Node, Node> dict;

    public DictNode(Map<Node, Node> dict, Position pos_start, Position pos_end) {
        this.dict = dict;
        this.pos_start = pos_start.copy();
        this.pos_end = pos_end.copy();
        jptype = Constants.JPType.Dict;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        Dict dict = new Dict(new ConcurrentHashMap<>());

        Map.Entry<Node, Node>[] entrySet = new Map.Entry[0];
        entrySet = this.dict.entrySet().toArray(entrySet);
        int length = entrySet.length;
        for (int i = 0; i < length; i++) {
            Map.Entry<Node, Node> entry = entrySet[i];
            Obj key = res.register(inter.visit(entry.getKey(), context));
            if (res.shouldReturn()) return res;
            Obj value = res.register(inter.visit(entry.getValue(), context));
            if (res.shouldReturn()) return res;
            dict.set(key, value);
        } return res.success(dict.set_context(context).set_pos(pos_start, pos_end));
    }

}
