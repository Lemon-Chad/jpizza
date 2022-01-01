package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Dict;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DictNode extends Node {
    public final Map<Node, Node> dict;

    public DictNode(Map<Node, Node> dict, @NotNull Position pos_start, @NotNull Position pos_end) {
        this.dict = dict;

        constant = true;
        for (Map.Entry<Node, Node> entry : dict.entrySet()) {
            if (!entry.getKey().constant || !entry.getValue().constant) {
                constant = false;
                break;
            }
        }

        this.pos_start = pos_start.copy();
        this.pos_end = pos_end.copy();
        jptype = JPType.Dict;
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

    @Override
    public Node optimize() {
        Map<Node, Node> newDict = new ConcurrentHashMap<>();
        for (Map.Entry<Node, Node> entry : dict.entrySet()) {
            Node val = entry.getValue().optimize();
            Node key = entry.getKey().optimize();
            newDict.put(key, val);
        }
        return new DictNode(newDict, pos_start, pos_end);
    }

    @Override
    public boolean asBoolean() {
        return !dict.isEmpty();
    }

    @Override
    public String asString() {
        StringBuilder result = new StringBuilder("{");
        dict.forEach((k, v) -> {
            if (k.jptype == JPType.String) {
                result.append('"').append(k.asString()).append('"');
            }
            else {
                result.append(k.asString());
            }
            result.append(": ");
            if (v.jptype == JPType.String) {
                result.append('"').append(v.asString()).append('"');
            }
            else {
                result.append(v.asString());
            }
            result.append(", ");
        });
        if (result.length() > 1) {
            result.setLength(result.length() - 2);
        } result.append("}");
        return result.toString();
    }

    @Override
    public Map<Node, Node> asMap() {
        return dict;
    }

    @Override
    public double asNumber() {
        return dict.size();
    }

    @Override
    public boolean equals(Node other) {
        if (other.jptype != JPType.Dict) return false;
        DictNode otherDict = (DictNode) other;
        if (dict.size() != otherDict.dict.size()) return false;
        for (Map.Entry<Node, Node> entry : dict.entrySet()) {
            Node key = entry.getKey();
            Node value = entry.getValue();
            if (!otherDict.dict.containsKey(key)) return false;
            if (!otherDict.dict.get(key).equals(value)) return false;
        } return true;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        for (Map.Entry<Node, Node> entry : dict.entrySet()) {
            children.add(entry.getKey());
            children.add(entry.getValue());
        } return children;
    }

    @Override
    public String visualize() {
        return "{ Map }";
    }
}
