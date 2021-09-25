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
    public final List<Token> children;
    public final List< List<String> > childrenParams;
    public final List< List<String> > childrenTypes;

    public EnumNode(Token tok, List<Token> children, List< List<String> > childrenParams,
                    List< List<String> > childrenTypes) {
        super(tok);
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
            children.put(
                    (String) this.children.get(i).value,
                    new EnumJChild(i, this.childrenParams.get(i), this.childrenTypes.get(i))
            );
        }

        EnumJ e = new EnumJ(name, children);

        context.symbolTable.define(name, e);

        return new RTResult().success(e);
    }

}
