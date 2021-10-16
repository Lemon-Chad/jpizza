package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.Pair;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.nodes.Node;

public class RefNode extends Node {
    public RefNode(Node inner) {
        jptype = Constants.JPType.String;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult();
    }

}
