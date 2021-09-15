package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;

public class BreakNode extends Node {
    public BreakNode(Position start_pos, Position end_pos) {
        this.pos_start = start_pos.copy(); this.pos_end = end_pos.copy();
        jptype = Constants.JPType.Break;
    }

    public RTResult visit(Interpreter inter, Context context) { return new RTResult().sbreak(); }

}
