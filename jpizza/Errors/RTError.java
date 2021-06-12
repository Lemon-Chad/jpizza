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
        return String.format(
                    "%s%s%s: %s\nFile %s, line %s\n\n%s%s",
                    Constants.TEXT_YELLOW,
                    generateTraceback(),
                    error_name, details,
                    pos_start.fn, pos_start.ln + 1,
                    Constants.stringWithArrows(pos_start.ftext, pos_start, pos_end),
                    Constants.TEXT_RESET
                );
    }

    public String generateTraceback() {
        String result = "";
        Position pos = pos_start.copy();
        Context ctx = context;

        while (ctx != null) {
            result = String.format(
                        "\tFile %s, line %s, in %s\n%s",
                        pos.fn, pos.ln + 1, ctx.displayName,
                        result
                    );
            pos = ctx.parentEntryPos; ctx = ctx.parent;
        }

        return String.format("Traceback (most recent call last):\n%s", result);
    }

}
