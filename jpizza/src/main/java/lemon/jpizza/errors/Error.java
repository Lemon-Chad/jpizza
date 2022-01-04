package lemon.jpizza.errors;

import lemon.jpizza.Constants;
import lemon.jpizza.Position;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class Error {
    public final Position pos_start;
    public final Position pos_end;
    public final String error_name;
    public final String details;

    public Error(@NotNull Position pos_start, @NotNull Position pos_end, String error_name, String details) {
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

    public static Error IllegalCharError(@NotNull Position start_pos, @NotNull Position end_pos, String details) {
        return new Error(start_pos, end_pos, "Illegal Character", details);
    }

    public static Error ExpectedCharError(@NotNull Position start_pos, @NotNull Position end_pos, String details) {
        return new Error(start_pos, end_pos, "Expected Character", details);
    }

    public static Error InvalidSyntax(@NotNull Position start_pos, @NotNull Position end_pos, String details) {
        return new Error(start_pos, end_pos, "Invalid Syntax", details);
    }

}
