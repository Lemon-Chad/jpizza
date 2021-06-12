package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Nodes.Values.ListNode;
import lemon.jpizza.Token;

import java.util.List;

public class FuncDefNode extends Node {
    public Token var_name_tok;
    public List<Token> arg_name_toks;
    public Node body_node;
    public boolean autoreturn;
    public boolean async;

    public FuncDefNode(Token var_name_tok, List<Token> arg_name_toks, Node body_node, boolean autoreturn,
                       boolean async) {
        this.var_name_tok = var_name_tok;
        this.async = async;
        this.arg_name_toks = arg_name_toks;
        this.body_node = body_node;
        this.autoreturn = autoreturn;

        pos_start = var_name_tok != null ? var_name_tok.pos_start : (
                arg_name_toks != null ? arg_name_toks.get(0).pos_start : body_node.pos_start
                );
        pos_end = body_node.pos_end;

    }

}
