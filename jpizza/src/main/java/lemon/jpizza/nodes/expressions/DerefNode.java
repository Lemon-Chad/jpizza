package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.Pair;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.nodes.Node;

public class DerefNode extends Node {
    public DerefNode(Node inner) {
        jptype = Constants.JPType.String;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult();
    }

}
