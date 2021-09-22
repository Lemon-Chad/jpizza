package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;


public class BytesNode extends Node {
    public Node toBytes;

    public BytesNode(Node toBytes) {
        this.toBytes = toBytes;
        this.pos_start = toBytes.pos_start.copy(); this.pos_end = toBytes.pos_end.copy();
        jptype = Constants.JPType.Bytes;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj toBytes = res.register(this.toBytes.visit(inter, context));
        if (res.error != null) return res;

        Obj bytearrq = toBytes.bytes();
        if (bytearrq.jptype != Constants.JPType.Bytes) return res.failure(new RTError(
                bytearrq.get_start(), bytearrq.get_end(),
                "Object has no {BYTE-ARRAY} form",
                context
        ));

        return new RTResult().success(bytearrq);
    }

}
