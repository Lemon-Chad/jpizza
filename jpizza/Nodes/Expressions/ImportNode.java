package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Token;
import lemon.jpizza.Nodes.Node;

public class ImportNode extends Node {
    public Token file_name_tok;

    public ImportNode(Token file_name_tok) {
        this.file_name_tok = file_name_tok;

        pos_start = file_name_tok.pos_start; pos_end = file_name_tok.pos_end;
    }

}
