package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClaccessNode extends Node {
    public final Node class_tok;
    public final Token attr_name_tok;

    public ClaccessNode(Node cls, Token atr) {
        class_tok = cls;
        attr_name_tok = atr;
        pos_start = cls.pos_start.copy(); pos_end = atr.pos_end.copy();
        jptype = JPType.Claccess;
    }

    @Override
    public Node optimize() {
        return new ClaccessNode(class_tok.optimize(), attr_name_tok);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(class_tok));
    }

    @Override
    public String visualize() {
        return "access::" + attr_name_tok.value;
    }
}
