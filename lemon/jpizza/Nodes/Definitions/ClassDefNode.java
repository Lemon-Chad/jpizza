package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Position;
import lemon.jpizza.Token;

import java.util.List;

public class ClassDefNode extends Node {
    public Token class_name_tok;
    public List<Token> attribute_name_toks;
    public List<Token> arg_name_toks;
    public Node make_node;
    public List<MethDefNode> methods;
    public List<Token> arg_type_toks;
    public List<Node> defaults;
    public int defaultCount;

    public ClassDefNode(Token class_name_tok, List<Token> attribute_name_toks, List<Token> arg_name_toks,
                        List<Token> arg_type_toks, Node make_node, List<MethDefNode> methods, Position pos_end,
                        List<Node> defaults, int defaultCount) {
        this.class_name_tok = class_name_tok;
        this.defaultCount = defaultCount;
        this.defaults = defaults;
        this.attribute_name_toks = attribute_name_toks;
        this.make_node = make_node;
        this.arg_name_toks = arg_name_toks;
        this.arg_type_toks = arg_type_toks;
        this.methods = methods;
        this.pos_end = pos_end;
        this.pos_start = class_name_tok.pos_start;
        jptype = Constants.JPType.ClassDef;
    }

}
