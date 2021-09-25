package lemon.jpizza.nodes.definitions;

import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

public class AttrDeclareNode extends Node {
    public final Token attrToken;
    public final String type;
    public final boolean isstatic;
    public final boolean isprivate;
    public final Node nValue;
    public Obj value;
    public final String name;

    public AttrDeclareNode(Token attrToken) {
        this.attrToken = attrToken;

        type = "any";
        isstatic = false;
        isprivate = false;
        nValue = null;

        name = attrToken.value.toString();

        pos_start = attrToken.pos_start; pos_end = attrToken.pos_end;
    }

    public AttrDeclareNode(Token attrToken, String type, boolean isstatic, boolean isprivate, Node value) {
        this.attrToken = attrToken;
        this.type = type;
        this.isstatic = isstatic;
        this.isprivate = isprivate;
        this.nValue = value;

        name = attrToken.value.toString();

        pos_start = attrToken.pos_start; pos_end = attrToken.pos_end;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        if (nValue != null)
            value = res.register(this.nValue.visit(inter, context));
        else
            value = new Null();
        return res;
    }

}
