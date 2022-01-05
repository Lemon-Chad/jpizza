package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BreakNode extends Node {
    public BreakNode(@NotNull Position start_pos, @NotNull Position end_pos) {
        this.pos_start = start_pos.copy(); this.pos_end = end_pos.copy();
        jptype = JPType.Break;
    }

    public RTResult visit(Interpreter inter, Context context) { return new RTResult().sbreak(); }

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
        return "break";
    }
}
