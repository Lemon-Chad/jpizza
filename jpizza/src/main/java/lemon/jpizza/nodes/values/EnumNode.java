package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.generators.Parser.EnumChild;
import lemon.jpizza.Token;

import java.util.List;

public class EnumNode extends ValueNode {
    public final List<EnumChild> children;
    public final boolean pub;

    public EnumNode(Token tok, List<EnumChild> children, boolean pub) {
        super(tok);
        this.children = children;
        this.pub = pub;
        jptype = JPType.Enum;
    }

    @Override
    public String visualize() {
        return "Enum";
    }
}
