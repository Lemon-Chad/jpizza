package lemon.jpizza.libraries.jdraw;

import java.awt.*;

public class Fnt {
    public final String fontName;
    public final int fontType;
    public final int fontSize;

    public Fnt(String fontName, int fontType, int font) {
        this.fontName = fontName;
        this.fontType = fontType;
        this.fontSize = font;
    }

    public Font asFont() {
        return new Font(fontName, fontType, fontSize);
    }

}
