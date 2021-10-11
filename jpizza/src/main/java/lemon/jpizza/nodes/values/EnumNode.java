package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.EnumJ;
import lemon.jpizza.objects.primitives.EnumJChild;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnumNode extends ValueNode {
    final List<Token> children;
    final List< List<String> > childrenParams;
    final List< List<String> > childrenTypes;
    final boolean pub;

    public EnumNode(Token tok, List<Token> children, List< List<String> > childrenParams,
                    List< List<String> > childrenTypes, boolean pub) {
        super(tok);
        this.pub = pub;
        this.childrenParams = childrenParams;
        this.childrenTypes = childrenTypes;
        this.children = children;
        jptype = Constants.JPType.Enum;
    }

    public RTResult visit(Interpreter inter, Context context) {
        Map<String, EnumJChild> children = new HashMap<>();
        String name = (String) tok.value;
        int size = this.children.size();

        for (int i = 0; i < size; i++) {
            String key = this.children.get(i).value.toString();
            EnumJChild child = new EnumJChild(i, this.childrenParams.get(i), this.childrenTypes.get(i));

            children.put(key, child);

            if (pub)
                context.symbolTable.define(key, child);
        }

        EnumJ e = new EnumJ(name, children);

        context.symbolTable.define(name, e);

        return new RTResult().success(e);
    }

}
