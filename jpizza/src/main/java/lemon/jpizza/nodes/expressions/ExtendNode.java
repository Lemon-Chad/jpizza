package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.Token;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class ExtendNode extends Node {
    public final Token file_name_tok;

    public ExtendNode(Token file_name_tok) {
        this.file_name_tok = file_name_tok;

        pos_start = file_name_tok.pos_start.copy(); pos_end = file_name_tok.pos_end.copy();
        jptype = JPType.Import;
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public String visualize() {
        return "extend " + file_name_tok.value;
    }
}
