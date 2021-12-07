package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;
import org.jetbrains.annotations.NotNull;

public class ContinueNode extends Node {
    public ContinueNode(@NotNull Position start_pos, @NotNull Position end_pos) {
        this.pos_start = start_pos.copy(); this.pos_end = end_pos.copy();
        jptype = Constants.JPType.Continue;
    }

    public RTResult visit(Interpreter inter, Context context) { return new RTResult().scontinue(); }

}
