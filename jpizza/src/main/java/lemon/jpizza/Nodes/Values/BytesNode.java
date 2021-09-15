package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Results.RTResult;


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
