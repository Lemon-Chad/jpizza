package lemon.jpizza.Errors;

import lemon.jpizza.Constants;
import lemon.jpizza.Position;

public class Error {
    Position pos_start;
    Position pos_end;
    String error_name;
    String details;

    public Error(Position pos_start, Position pos_end, String error_name, String details) {
        this.pos_start = pos_start;
        this.pos_end = pos_end;
        this.error_name = error_name;
        this.details = details;
    }

    public String asString() {
        return String.format(
                "%s: %s\nFile %s, line %s\n\n%s",
                error_name, details,
                pos_start.fn, pos_start.ln + 1,
                Constants.stringWithArrows(pos_start.ftext, pos_start, pos_end)
        );
    }

    public static Error IllegalCharError(Position start_pos, Position end_pos, String details) {
        return new Error(start_pos, end_pos, "Illegal Character", details);
    }

    public static Error ExpectedCharError(Position start_pos, Position end_pos, String details) {
        return new Error(start_pos, end_pos, "Expected Character", details);
    }

    public static Error InvalidSyntax(Position start_pos, Position end_pos, String details) {
        return new Error(start_pos, end_pos, "Invalid Syntax", details);
    }

}
