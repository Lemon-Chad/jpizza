package lemon.jpizza.nodes;

import static lemon.jpizza.Constants.repeat;

public class TreePrinter {
    //    ----[+]----
    //   |          |
    // [ 1 ]    [ 23456 ]

    public static String print(Node node) {
        String s = "";
        for (Node child : node.getChildren()) {
            if (child == null) continue;
            String p = print(child);
            s = merge(s, p);
        }

        String parent = "[ " + node.visualize() + " ]";

        int childWidth = width(s);
        int parentWidth = width(parent);
        int width = Math.max(childWidth, parentWidth);

        int parentOffset = (width - parentWidth) / 2;
        int childOffset = (width - childWidth) / 2;

        int centerPipeLoc = (width + 1) / 2 - 1;
        int left = s.split("\n")[0].indexOf("|");
        int right = s.split("\n")[0].lastIndexOf("|");
        String coverPipes = left == -1 ? "" : repeat(" ", left) + repeat("_", right - left + 1) + repeat(" ", width - right - 1);
        if (!coverPipes.isEmpty())
            coverPipes = coverPipes.substring(0, centerPipeLoc) + "|" + coverPipes.substring(centerPipeLoc + 1);

        StringBuilder sb = new StringBuilder();
        sb.append(repeat(" ", parentOffset)).append(parent).append(repeat(" ", width - parentOffset - parentWidth)).append("\n");
        sb.append(coverPipes).append("\n");
        for (String line : s.split("\n")) {
            sb.append(repeat(" ", childOffset)).append(line).append(repeat(" ", width - childOffset - childWidth)).append("\n");
        }

        String centerPipe = repeat(" ", (width + 1) / 2 - 1) + "|\n";
        return centerPipe + sb;
    }

    private static String merge(String a, String b) {
        if (a.isEmpty())
            return b;

        StringBuilder sb = new StringBuilder();
        String[] aLines = a.split("\n");
        String[] bLines = b.split("\n");
        int max = Math.max(aLines.length, bLines.length);
        for (int i = 0; i < max; i++) {
            if (i < aLines.length) {
                sb.append(aLines[i]).append(repeat(" ", width(a) - aLines[i].length()));
            }
            else {
                sb.append(repeat(" ", width(a)));
            }
            if (i < bLines.length) {
                sb.append("  ").append(bLines[i]);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static int width(String s) {
        int max = 0;
        for (String line : s.split("\n")) {
            max = Math.max(max, line.length());
        }
        return max;
    }
}
