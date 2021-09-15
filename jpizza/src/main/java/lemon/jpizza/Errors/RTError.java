package lemon.jpizza.Errors;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Position;

public class RTError extends Error {
    Context context;

    public RTError(Position pos_start, Position pos_end, String details, Context context) {
        super(pos_start, pos_end, "Runtime Error", details);
        this.context = context;
    }

    public String asString() {
        if (pos_start == null || pos_end == null) return String.format("%s: %s", error_name, details);
        return String.format(
                    "%s\n%s: %s\nFile %s, line %s\n%s\n",
                    generateTraceback(),
                    error_name, details,
                    pos_start.fn, pos_start.ln + 1,
                    Constants.stringWithArrows(pos_start.ftext, pos_start, pos_end)
                );
    }

    public String generateTraceback() {
        String result = "";
        Position pos = pos_start.copy();
        Context ctx = context;

        while (ctx != null) {
            if (pos != null)
                result = String.format(
                            "  ╰──>  File %s, line %s, in %s\n%s",
                            pos.fn, pos.ln + 1, ctx.displayName,
                            result
                        );
            else
                result = String.format("\t%s\n%s", ctx.displayName, result);
            pos = ctx.parentEntryPos; ctx = ctx.parent;
        }

        return String.format("Traceback (most recent call last):\n%s", result);
    }

}
