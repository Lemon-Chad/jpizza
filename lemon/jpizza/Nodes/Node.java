package lemon.jpizza.Nodes;

import lemon.jpizza.Constants.JPType;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;

import java.io.Serializable;

public class Node implements Serializable {
    public Position pos_start;
    public Position pos_end;
    public JPType jptype;
    public boolean fluctuating = false;

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().failure(new RTError(
                pos_start.copy(), pos_end.copy(),
                "No visit method for " + getClass().getSimpleName() + "!",
                context
        ));
    }

}
