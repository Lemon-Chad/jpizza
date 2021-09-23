package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.Pair;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;
import org.apache.commons.text.StringEscapeUtils;

public class StringNode extends ValueNode {
    public String val;
    public StringNode(Token tok) {
        super(tok);
        val = ((Pair<String, Boolean>) tok.value).a;
        jptype = Constants.JPType.String;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Str(StringEscapeUtils.unescapeJava(val)).set_context(context)
                .set_pos(pos_start, pos_end));
    }

}
