package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.List;

public class UseNode extends Node {
    public Token useToken;
    public List<Token> args;

    public UseNode(Token useToken, List<Token> args) {
        this.useToken = useToken;
        this.args = args;
        pos_start = useToken.pos_start.copy(); pos_end = useToken.pos_end.copy();
        jptype = Constants.JPType.Use;
    }

    public RTResult visit(Interpreter inter, Context context) {
        Token useToken = this.useToken;
        switch ((String) useToken.value) {
            case "memoize" ->
                    context.doMemoize();
            case "func" -> {
                if (!inter.main) break;

                if (args.size() < 1) return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        "Expected function name",
                        context
                ));

                inter.fnFinish = (String) args.get(0).value;
            }
            case "object" -> {
                if (!inter.main) break;

                if (args.size() < 1) return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        "Expected object name",
                        context
                ));

                inter.clFinish = (String) args.get(0).value;
            }
        }
        return new RTResult().success(new Null());
    }

}
