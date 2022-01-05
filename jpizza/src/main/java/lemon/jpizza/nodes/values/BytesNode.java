package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;

import java.util.ArrayList;
import java.util.List;


public class BytesNode extends Node {
    public final Node toBytes;

    public BytesNode(Node toBytes) {
        this.toBytes = toBytes;
        this.pos_start = toBytes.pos_start.copy(); this.pos_end = toBytes.pos_end.copy();
        jptype = JPType.Bytes;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj toBytes = res.register(inter.visit(this.toBytes, context));
        if (res.error != null) return res;

        Obj bytearrq = toBytes.bytes();
        if (bytearrq.jptype != JPType.Bytes) return res.failure(RTError.Conversion(
                bytearrq.get_start(), bytearrq.get_end(),
                "Object has no {BYTE-ARRAY} form",
                context
        ));

        return new RTResult().success(bytearrq);
    }

    @Override
    public Node optimize() {
        return new BytesNode(toBytes.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(List.of(toBytes));
    }

    @Override
    public String visualize() {
        return "@";
    }
}
