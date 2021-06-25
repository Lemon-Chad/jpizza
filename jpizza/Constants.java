package lemon.jpizza;

import lemon.jpizza.Contextuals.Context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static char[] NUMBERS = "0123456789".toCharArray();
    public static char[] NUMDOT = "0123456789.".toCharArray();
    public static char[] LETTERS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static char[] LETTERS_DIGITS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();
    public static final String TEXT_RESET = "\u001B[0m";
    public static final String TEXT_BLACK = "\u001B[30m";
    public static final String TEXT_RED = "\u001B[31m";
    public static final String TEXT_GREEN = "\u001B[32m";
    public static final String TEXT_YELLOW = "\u001B[33m";
    public static final String TEXT_BLUE = "\u001B[34m";
    public static final String TEXT_PURPLE = "\u001B[35m";
    public static final String TEXT_CYAN = "\u001B[36m";
    public static final String TEXT_WHITE = "\u001B[37m";
    public static String[] KEYWORDS = {
            "async",
            "import",
            "bin",
            "function",
            "attr",
            "&",
            "|",
            "bake",
            "var",
            "for",
            "while",
            "null",
            "if",
            "elif",
            "else",
            "return",
            "continue",
            "break",
            "method",
            "ingredients",
            "recipe",
            "#",
            "fn",
    };
    public static char BREAK = ';';
    public static char[] IGNORE = new char[]{' ', '\n', '\t'};
    public static Map<String, Context> LIBRARIES = new HashMap<>();
    public static char splitter = BREAK;

    public static String stringWithArrows(String text, Position pos_start, Position pos_end) {
        StringBuilder result = new StringBuilder();
        int idx_start = Math.max(text.lastIndexOf(splitter, pos_start.idx), 0);
        int idx_end = text.indexOf(splitter, idx_start + 1);
        int line_count = pos_end.ln - pos_start.ln + 1;
        String line; int col_start; int col_end;
        for (int i = 0; i < line_count; i++) {
            if (idx_end < 0)
                    idx_end = text.length() + idx_end + 1;
            line = text.substring(idx_start, idx_end);
            col_start = i == 0 ? pos_start.col : 0;
            col_end = i == line_count - 1 ? pos_end.col : line.length() - 1;
            result.append(line).append('\n');
            result.append(" ".repeat(col_start)).append("^".repeat(col_end - col_start));
            idx_start = idx_end;
            idx_end = text.indexOf(splitter, idx_start + 1);
            if (idx_end < 0)
                idx_end = text.length();
        }
        return result.toString().replace("\t", "");
    }

}
