package lemon.jpizza;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Constants {
    public static final char[] NUMBERS = "0123456789".toCharArray();
    public static final char[] NUMDOT = "0123456789._".toCharArray();
    public static final char[] LETTERS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static final char[] LETTERS_DIGITS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();
    public static final List<TokenType> TYPETOKS = Arrays.asList(
            TokenType.Identifier,
            TokenType.Keyword,
            TokenType.Float,
            TokenType.Int,
            TokenType.LeftParen,
            TokenType.RightParen,
            TokenType.LeftBracket,
            TokenType.RightBracket,
            TokenType.RightAngle,
            TokenType.LeftAngle
    );
    public static final String[] KEYWORDS = {
            "yields",
            "free",
            "assert",
            "let",
            "throw",
            "struct",
            "do",
            "loop",
            "pass",
            "cal",
            "yourmom",
            "async",
            "import",
            "scope",
            "as",
            "extend",
            "bin",
            "function",
            "attr",
            "bake",
            "const",
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
            "mthd",
            "md",
            "ingredients",
            "recipe",
            "class",
            "obj",
            "switch",
            "case",
            "fn",
            "enum",
            "default",
            "match",
            "pub",
            "prv",
            "static",
            "stc"
    };
    @SuppressWarnings("unused") public static char BREAK = ';';
    @SuppressWarnings("unused") public static char[] IGNORE = new char[]{' ', '\n', '\t'};
    public static final char splitter = '\n';

    public static final Map<String, String> STANDLIBS = new HashMap<String, String>(){{
        put("std", "enum pub Option {\n" +
                "    Some { val },\n" +
                "    None,\n" +
                "}\n" +
                "\n" +
                "enum pub StaticOption {\n" +
                "    Box(T){ val: T },\n" +
                "    Empty(T),\n" +
                "}\n" +
                "\n" +
                "class Iter {\n" +
                "    prv iterable: list;\n" +
                "    prv f;\n" +
                "    prv output;\n" +
                "\n" +
                "    ingredients<iterable, f, output> {\n" +
                "        attr iterable => iterable;\n" +
                "        attr f => f;\n" +
                "        attr output => output;\n" +
                "    }\n" +
                "\n" +
                "    mthd collect {\n" +
                "        let fin => for (x <- iterable) => f(x);\n" +
                "        output(..fin)\n" +
                "    }\n" +
                "\n" +
                "    mthd stream {\n" +
                "        for (x <- iterable) f(x);\n" +
                "    }\n" +
                "\n" +
                "    mthd bin type -> `Iter`;\n" +
                "\n" +
                "}\n" +
                "\n" +
                "class Array {\n" +
                "    prv internal: list;\n" +
                "\n" +
                "    ingredients<..items>(T) {\n" +
                "        internal => [];\n" +
                "        if (T == \"any\") {\n" +
                "            internal => for (item <- items) => &item;\n" +
                "            return null;\n" +
                "        }\n" +
                "\n" +
                "        let itype => None;\n" +
                "        for (item <- items) {\n" +
                "            itype => Some(type(item));\n" +
                "            if (itype::val != T)\n" +
                "                throw \"Type\", `Expected type ${T}, got ${itype::val}`;\n" +
                "            append(internal, &item);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    mthd bin string {\n" +
                "        let un => for (x <- internal) => str(x);\n" +
                "        str(un)\n" +
                "    }\n" +
                "\n" +
                "    mthd bin type -> `Array(${T})`;\n" +
                "    \n" +
                "    mthd bin eq<other> {\n" +
                "        if (type(other) != type(this)) return false;\n" +
                "        if (other::size() != this::size()) return false;\n" +
                "        \n" +
                "        list(this) == list(other)\n" +
                "    }\n" +
                "    \n" +
                "    mthd insert<item#T, index#num> = void {\n" +
                "        insert(internal, item, index);\n" +
                "    }\n" +
                "\n" +
                "    mthd remove<item#T> = void {\n" +
                "        if (!contains(internal, item)) throw \"Out of Bounds\", \"Item is not in Array\";\n" +
                "        internal /= item;\n" +
                "    }\n" +
                "\n" +
                "    mthd iter<func#function> = Iter {\n" +
                "        Iter(internal, func, !<..items> -> Array(..items)<any>)\n" +
                "    }\n" +
                "\n" +
                "    mthd pop<index#num> = T {\n" +
                "        let item => *(internal[index]);\n" +
                "        internal /= item;\n" +
                "        return item;\n" +
                "    }\n" +
                "\n" +
                "    mthd add<item#T> = void {\n" +
                "        append(internal, &item);\n" +
                "    }\n" +
                "\n" +
                "    mthd size -> size(internal);\n" +
                "\n" +
                "    mthd addAll<..items> = void {\n" +
                "        for (item <- items)\n" +
                "            add(item);\n" +
                "    }\n" +
                "\n" +
                "    mthd bin list -> internal;\n" +
                "\n" +
                "    mthd slice<min#num, max#num> {\n" +
                "        let s => for (x <- sublist(internal, min, max)) => *x;\n" +
                "        Array(..s)<T>\n" +
                "    }\n" +
                "\n" +
                "    mthd indexOf<item> = num {\n" +
                "        return indexOf(internal, &item);\n" +
                "    }\n" +
                "\n" +
                "    mthd bin bracket<index> -> internal[index];\n" +
                "\n" +
                "    mthd contains<x> = bool {\n" +
                "        return contains(internal, x);\n" +
                "    }\n" +
                "\n" +
                "    mthd join<str#String> -> join(str, internal);\n" +
                "\n" +
                "}\n" +
                "\n" +
                "class Tuple {\n" +
                "    prv items: list;\n" +
                "    ingredients<..entry> {\n" +
                "        items => [];\n" +
                "        for (item <- entry)\n" +
                "            append(items, &item);\n" +
                "    }\n" +
                "\n" +
                "    mthd size -> size(items);\n" +
                "\n" +
                "    mthd bin bracket<other> -> items[other];\n" +
                "\n" +
                "    mthd bin string -> `(${substr(str(items), 1, size(str(items)) - 1)})`;\n" +
                "    \n" +
                "    mthd bin eq<other> {\n" +
                "        if (type(other) != type(this)) return false;\n" +
                "        if (other::size() != this::size()) return false;\n" +
                "        \n" +
                "        list(this) == list(other)\n" +
                "    }\n" +
                "    \n" +
                "    mthd bin type {\n" +
                "        let types => [];\n" +
                "        for (item <- items)\n" +
                "            types.append(type(*item));\n" +
                "        return \"(\" + (\",\".join(types)) + \")\";\n" +
                "    }\n" +
                "    \n" +
                "    mthd contains<x> -> contains(items, x);\n" +
                "\n" +
                "    mthd bin list -> items;\n" +
                "}\n" +
                "\n" +
                "class Map {\n" +
                "    prv internal: dict;\n" +
                "\n" +
                "    ingredients<..pairs>(K, V) {\n" +
                "        internal => ({});\n" +
                "        for (pair <- pairs) {\n" +
                "            if (type(pair) != \"list\" | size(pair) != 2)\n" +
                "                throw \"Type\", \"Expected a key-value pair ([k, v])\";\n" +
                "            elif (K != \"any\" & type(pair[0]) != K)\n" +
                "                throw \"Type\", `Expected key type to be ${K}, got ${type(pair[0])}`;\n" +
                "            elif (V != \"any\" & type(pair[1]) != V)\n" +
                "                throw \"Type\", `Expected key type to be ${V}, got ${type(pair[1])}`;\n" +
                "            \n" +
                "            set(internal, pair[0], &pair[1]);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    mthd iter<f#function> -> Iter(\n" +
                "        list(pairArray()),\n" +
                "        f,\n" +
                "        !<..array> {\n" +
                "            let new => Map()<any, any>;\n" +
                "            for (pair <- list(array))\n" +
                "                new::set(pair[0], pair[1]);\n" +
                "            new\n" +
                "        }\n" +
                "    );\n" +
                "\n" +
                "    mthd bin string {\n" +
                "        let new => ({});\n" +
                "        for (key <- list(internal))\n" +
                "            set(new, str(key), str(internal[key]));\n" +
                "        str(new)\n" +
                "    }\n" +
                "\n" +
                "    mthd bin bracket<other#K> -> internal[other];\n" +
                "\n" +
                "    mthd contains<other> -> contains(list(internal), other);\n" +
                "\n" +
                "    mthd bin list -> list(internal);\n" +
                "\n" +
                "    mthd keyArray -> Array(..list(internal));\n" +
                "\n" +
                "    mthd get<other#K> {\n" +
                "        if (!this::contains(other)) throw \"Out of Bounds\", \"Key not in map\";\n" +
                "        internal[other]\n" +
                "    }\n" +
                "\n" +
                "    mthd getOrDefault<other#K, def> {\n" +
                "        if (!this::contains(other)) return def;\n" +
                "        internal[other]\n" +
                "    }\n" +
                "\n" +
                "    mthd size -> size(list(internal));\n" +
                "    \n" +
                "    mthd bin eq<other> {\n" +
                "        if (type(other) != type(this)) return false;\n" +
                "        \n" +
                "        dict(this) == dict(other)\n" +
                "    }\n" +
                "    \n" +
                "    mthd bin dictionary -> internal;\n" +
                "\n" +
                "    mthd set<key#K, value#V> {\n" +
                "        set(internal, key, value);\n" +
                "    }\n" +
                "\n" +
                "    mthd del<key#K> {\n" +
                "        if (!this::contains(key)) throw \"Out of Bounds\", \"Key not in map\";\n" +
                "        delete(internal, key);\n" +
                "    }\n" +
                "\n" +
                "    mthd pairArray {\n" +
                "        let arr => Array()<Array(any)>;\n" +
                "        for (key <- list(internal)) {\n" +
                "            var a => Array()<any>;\n" +
                "            a::addAll(key, internal[key]);\n" +
                "            arr::add(a);\n" +
                "        }\n" +
                "        arr\n" +
                "    }\n" +
                "}");
    }};

    public static int indexToLine(String code, int index) {
        return code.substring(0, index).split("\n").length - 1;
    }

    public static int leftPadding(String str) {
        int tabs = 0;
        while (tabs < str.length() && Character.isWhitespace(str.charAt(tabs))) {
            tabs++;
        }
        return tabs;
    }

    public static String highlightFlat(String source, int index, int len) {
        StringBuilder sb = new StringBuilder();

        String[] lines = source.split("\n");

        int line = 0;
        while (index >= lines[line].length() && line < lines.length - 1) {
            index -= lines[line].length() + 1;
            line++;
        }

        int tabs = leftPadding(lines[line]);
        index -= tabs;
        len += tabs;

        while (len > 0 && line < lines.length) {
            String lineStr = lines[line];
            int lineLen = lineStr.length();

            tabs = leftPadding(lineStr);
            len -= tabs;
            lineLen -= tabs;
            String text = lineStr.substring(tabs);

            if (lineLen == 0) {
                sb.append("\n");
                line++;
                continue;
            }

            int end = Math.min(index + len, lineLen);
            String highlight;
            if (end - index >= 2) {
                if (Shell.fileEncoding.equals("UTF-8"))
                    highlight = "╰" + repeat(end - index - 2, "─") + "╯";
                else
                    highlight = "\\" + repeat(end - index - 2, "_") + "/";
            }
            else {
                highlight = "^";
            }

            sb.append(text)
              .append("\n")
              .append(String.join("", repeat(index, " ")))
              .append(highlight)
              .append("\n");

            len -= lineLen - index;

            line++;
            index = 0;
        }

        return sb.toString();
    }

    public static int nonWhitespace(String string){
        char[] characters = string.toCharArray();
        for(int i = 0; i < string.length(); i++){
            if(!Character.isWhitespace(characters[i])){
                return i;
            }
        }
        return 0;
    }

    public static String stringWithArrows(String text, @NotNull Position pos_start, @NotNull Position pos_end) {
        StringBuilder result = new StringBuilder();

        int idxStart = Math.max(0, text.lastIndexOf(splitter, pos_start.tidx));
        int idxEnd = text.indexOf(splitter, idxStart + 1);

        if (idxEnd < 0) idxEnd = text.length();

        int line_count = pos_end.ln - pos_start.ln + 1;
        int offs = 0;
        int colStart, colEnd, dist;
        for (int i = 0; i < line_count; i++) {
            String line = text.substring(idxStart, idxEnd);

            colStart = i == 0 ? pos_start.tcol : nonWhitespace(line);
            colEnd = i == line_count - 1 ? pos_end.tcol : line.length() - 1;
            dist = colEnd - colStart;

            String grouping;
            if (dist >= 2) {
                if (Shell.fileEncoding.equals("UTF-8"))
                    grouping = "╰" + repeat("─", dist - 2) + "╯";
                else
                    grouping = "\\" + repeat("_", dist - 2) + "/";
            }
            else {
                grouping = "^";
            }

            result.append(line).append("\n")
                    .append(repeat(" ", Math.max(0, colStart + offs))).append(grouping);

            idxStart = idxEnd;
            idxEnd = text.indexOf(splitter, idxStart + 1);

            if (idxEnd < 0) idxEnd = text.length();
        }

        return result.toString().replace("\t", "");
    }

    public static byte[] objToBytes(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public static String repeat(String str, int times) {
        return new String(new char[times]).replace("\0", str);
    }

    public static String repeat(int times, String str) {
        return new String(new char[times]).replace("\0", str);
    }

    public static String readString(Path path) throws IOException {
        return Files.lines(path).collect(Collectors.joining("\n"));
    }
}
