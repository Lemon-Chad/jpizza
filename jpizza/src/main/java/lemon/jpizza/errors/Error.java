package lemon.jpizza.errors;

import lemon.jpizza.Constants;
import lemon.jpizza.Position;

import java.io.Serializable;

public class Error implements Serializable {
    public Position pos_start;
    public Position pos_end;
    public String error_name;
    public String details;

    public Error(Position pos_start, Position pos_end, String error_name, String details) {
        this.pos_start = pos_start != null ? pos_start.copy() : null;
        this.pos_end = pos_end != null ? pos_end.copy() : null;
        this.error_name = error_name;
        this.details = details;
    }

    public String asString() {
        return String.format(
                "%s: %s\nFile %s, line %s\n%s",
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
