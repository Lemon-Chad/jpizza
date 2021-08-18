package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Primitives.EnumJ;
import lemon.jpizza.Objects.Primitives.EnumJChild;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnumNode extends ValueNode {
    public List<Token> children;
    public List< List<String> > childrenParams;
    public EnumNode(Token tok, List<Token> children, List< List<String> > childrenParams) {
        super(tok);
        this.childrenParams = childrenParams;
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
                    new EnumJChild(i, this.childrenParams.get(i))
            );
        }

        EnumJ e = new EnumJ(name, children);

        context.symbolTable.define(name, e);

        return new RTResult().success(e);
    }

}
