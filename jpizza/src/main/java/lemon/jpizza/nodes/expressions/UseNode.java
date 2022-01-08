package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class UseNode extends Node {
    public final Token useToken;
    public final List<Token> args;

    public UseNode(Token useToken, List<Token> args) {
        this.useToken = useToken;
        this.args = args;
        pos_start = useToken.pos_start.copy(); pos_end = useToken.pos_end.copy();
        jptype = JPType.Use;
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
        String[] args = new String[this.args.size() + 1];
        for (int i = 0; i < args.length - 1; i++) {
            args[i + 1] = this.args.get(i).value.toString();
        }
        args[0] = useToken.value.toString();
        return "#" + String.join(" ", args);
    }
}
