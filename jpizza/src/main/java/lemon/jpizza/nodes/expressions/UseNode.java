package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.List;

public class UseNode extends Node {
    public final Token useToken;
    public final List<Token> args;
    public boolean fluctuating = true;

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

                if (args.size() < 1) return new RTResult().failure(RTError.ArgumentCount(
                        pos_start, pos_end,
                        "Expected function name",
                        context
                ));

                inter.fnFinish = (String) args.get(0).value;
            }
            case "object" -> {
                if (!inter.main) break;

                if (args.size() < 1) return new RTResult().failure(RTError.ArgumentCount(
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
