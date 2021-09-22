package lemon.jpizza.errors;

import lemon.jpizza.Constants;
import lemon.jpizza.Position;

public class Tip extends Error {
    String refactor;

    public Tip(Position pos_start, Position pos_end, String details, String refactor) {
        super(pos_start, pos_end, "Tip", details);
        this.refactor = refactor;
    }

    public String asString() {
        if (pos_start == null || pos_end == null) return String.format("%s: %s", error_name, details);
        return String.format(
                    "%s: %s\nFile %s, line %s\n%s\nExample: %s\n",
                    error_name, details,
                    pos_start.fn, pos_start.ln + 1,
                    Constants.stringWithArrows(pos_start.ftext, pos_start, pos_end),
                    refactor
                );
    }

}
