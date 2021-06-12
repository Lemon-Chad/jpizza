package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Nodes.Values.ListNode;
import lemon.jpizza.Position;
import lemon.jpizza.Token;

import java.util.List;

public class ClassDefNode extends Node {
    public Token class_name_tok;
    public List<Token> attribute_name_toks;
    public List<Token> arg_name_toks;
    public Node make_node;
    public List<MethDefNode> methods;

    public ClassDefNode(Token class_name_tok, List<Token> attribute_name_toks, List<Token> arg_name_toks,
                        Node make_node, List<MethDefNode> methods, Position pos_end) {
        this.class_name_tok = class_name_tok;
        this.attribute_name_toks = attribute_name_toks;
        this.make_node = make_node;
        this.arg_name_toks = arg_name_toks;
        this.methods = methods;
        this.pos_end = pos_end;
        this.pos_start = class_name_tok.pos_start;
    }

}
