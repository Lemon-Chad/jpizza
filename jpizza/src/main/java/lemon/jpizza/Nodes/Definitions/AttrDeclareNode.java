package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;
import org.w3c.dom.Attr;

public class AttrDeclareNode extends Node {
    public Token attrToken;
    public String type;
    public boolean isstatic;
    public boolean isprivate;
    public Node nValue;
    public Obj value;
    public String name;

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
