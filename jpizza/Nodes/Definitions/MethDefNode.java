package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Nodes.Values.ListNode;
import lemon.jpizza.Token;

import java.util.List;

public class MethDefNode extends Node {
    public Token var_name_tok;
    public List<Token> arg_name_toks;
    public Node body_node;
    public boolean autoreturn;
    public boolean async;
    public boolean bin;

    public MethDefNode(Token var_name_tok, List<Token> arg_name_toks, Node body_node, boolean autoreturn,
                       boolean bin, boolean async) {
        this.var_name_tok = var_name_tok;
        this.async = async;
        this.arg_name_toks = arg_name_toks;
        this.body_node = body_node;
        this.autoreturn = autoreturn;
        this.bin = bin;

        pos_start = var_name_tok.pos_start;
        pos_end = body_node.pos_end;

    }

}
