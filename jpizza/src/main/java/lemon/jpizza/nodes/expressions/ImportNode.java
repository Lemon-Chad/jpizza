package lemon.jpizza.nodes.expressions;

import lemon.jpizza.*;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class ImportNode extends Node {
    public final Token file_name_tok;
    public final Token as_tok;

    public ImportNode(Token file_name_tok) {
        this.file_name_tok = file_name_tok;
        this.as_tok = null;

        pos_start = file_name_tok.pos_start.copy(); pos_end = file_name_tok.pos_end.copy();
        jptype = JPType.Import;
    }

    public ImportNode(Token file_name_tok, Token as_tok) {
        this.file_name_tok = file_name_tok;
        this.as_tok = as_tok;

        pos_start = file_name_tok.pos_start.copy(); pos_end = as_tok.pos_end.copy();
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
        return "import " + file_name_tok.value;
    }
}
