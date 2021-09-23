package lemon.jpizza.nodes;

import lemon.jpizza.Constants.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;

import java.io.Serializable;

public class Node implements Serializable {
    public Position pos_start;
    public Position pos_end;
    public JPType jptype;
    public boolean fluctuating = false;

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().failure(RTError.Internal(
                pos_start.copy(), pos_end.copy(),
                "No visit method for " + getClass().getSimpleName(),
                context
        ));
    }

}
