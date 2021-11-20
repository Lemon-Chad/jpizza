package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.generators.Parser.EnumChild;
import lemon.jpizza.objects.executables.EnumJ;
import lemon.jpizza.objects.executables.EnumJChild;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnumNode extends ValueNode {
    final List<EnumChild> children;
    final boolean pub;

    public EnumNode(Token tok, List<EnumChild> children, boolean pub) {
        super(tok);
        this.children = children;
        this.pub = pub;
        jptype = Constants.JPType.Enum;
    }

    public RTResult visit(Interpreter inter, Context context) {
        Map<String, EnumJChild> children = new HashMap<>();
        String name = (String) tok.value;
        int size = this.children.size();

        for (int i = 0; i < size; i++) {
            EnumChild c = this.children.get(i);
            String key = c.token().value.toString();
            EnumJChild child = new EnumJChild(i, c.params(), c.types(), c.generics());

            children.put(key, child);

            if (pub)
                context.symbolTable.define(key, child);
        }

        EnumJ e = new EnumJ(name, children);

        context.symbolTable.define(name, e);

        return new RTResult().success(e);
    }

}
