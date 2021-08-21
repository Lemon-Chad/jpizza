package lemon.jpizza.Libraries.JDraw;

import java.awt.*;

public class Fnt {
    public String fontName;
    public int fontType;
    public int fontSize;

    public Fnt(String fontName, int fontType, int font) {
        this.fontName = fontName;
        this.fontType = fontType;
        this.fontSize = font;
    }

    public Font asFont() {
        return new Font(fontName, fontType, fontSize);
    }

}
