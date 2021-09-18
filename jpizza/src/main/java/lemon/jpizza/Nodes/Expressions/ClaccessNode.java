package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Executables.CMethod;
import lemon.jpizza.Objects.Executables.ClassInstance;
import lemon.jpizza.Objects.Executables.ClassPlate;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.EnumJ;
import lemon.jpizza.Objects.Primitives.EnumJChild;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Operations;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class ClaccessNode extends Node {
    public Node class_tok;
    public Token attr_name_tok;
    public boolean fluctuating = true;

    public ClaccessNode(Node cls, Token atr) {
        class_tok = cls;
        attr_name_tok = atr;
        pos_start = cls.pos_start.copy(); pos_end = atr.pos_end.copy();
        jptype = Constants.JPType.Claccess;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj var = res.register(class_tok.visit(inter, context));

        if (res.error != null) return res;
        if (var.jptype != Constants.JPType.ClassInstance && var.jptype != Constants.JPType.Enum &&
                var.jptype != Constants.JPType.ClassPlate) return res.failure(new RTError(
                pos_start, pos_end,
                "Expected class instance or enum",
                context
        ));

        switch (var.jptype) {
            case Enum:
                EnumJChild child = ((EnumJ) var).getChild((String) attr_name_tok.value);
                if (child == null)
                    return res.failure(new RTError(
                            pos_start.copy(), pos_end.copy(),
                            "Enum child is undefined",
                            context
                    ));
                return res.success(child.
                        set_context(context).set_pos(pos_start, pos_end));

            case ClassInstance:
                Object val = var.getattr(Operations.OP.ACCESS, new Str((String) attr_name_tok.value)
                        .set_pos(pos_start, pos_end)
                        .set_context(context));
                if (val instanceof String)
                    return Interpreter.getThis(val, context, pos_start, pos_end);
                else if (val instanceof CMethod && ((CMethod) val).isprivate) return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        "Method is private",
                        context
                ));
                else if (val instanceof RTError) return new RTResult().failure((RTError) val);
                return res.success(((Obj)val).set_context(((ClassInstance)var).value));

            case ClassPlate:
                Obj acval = res.register(((ClassPlate) var).access(new Str(attr_name_tok.value.toString())));
                if (res.error != null) return res;
                return res.success(acval.set_context(context).set_pos(pos_start, pos_end));

            default:
                return res.failure(new RTError(
                        pos_start, pos_end,
                        "Type has no accessible traits",
                        context
                ));
        }
    }

}
