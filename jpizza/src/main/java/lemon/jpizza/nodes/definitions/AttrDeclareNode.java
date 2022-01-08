package lemon.jpizza.nodes.definitions;

import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AttrDeclareNode extends Node {
    public final Token attrToken;
    public final List<String> type;
    public final boolean isstatic;
    public final boolean isprivate;
    public final Node nValue;
    public final String name;

    public AttrDeclareNode(Token attrToken) {
        this.attrToken = attrToken;

        type = Collections.singletonList("any");
        isstatic = false;
        isprivate = false;
        nValue = null;

        name = attrToken.value.toString();

        pos_start = attrToken.pos_start; pos_end = attrToken.pos_end;
    }

    public AttrDeclareNode(Token attrToken, List<String> type, boolean isstatic, boolean isprivate, Node value) {
        this.attrToken = attrToken;
        this.type = type;
        this.isstatic = isstatic;
        this.isprivate = isprivate;
        this.nValue = value;

        name = attrToken.value.toString();

        pos_start = attrToken.pos_start; pos_end = attrToken.pos_end;
    }

    @Override
    public Node optimize() {
        return new AttrDeclareNode(
                attrToken,
                type,
                isstatic,
                isprivate,
                nValue == null ? null : nValue.optimize()
        );
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(nValue));
    }

    @Override
    public String visualize() {
        return "!attr " + name;
    }
}
