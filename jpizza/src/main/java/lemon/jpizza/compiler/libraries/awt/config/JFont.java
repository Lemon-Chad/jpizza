package lemon.jpizza.compiler.libraries.awt.config;

import java.awt.*;

public class JFont {
    private final String fontName;
    private final int style;
    private final int size;

    public JFont(String fontName, int style, int size) {
        this.fontName = fontName;
        this.style = style;
        this.size = size;
    }

    public Font getFont() {
        return new Font(fontName, style, size);
    }
}
