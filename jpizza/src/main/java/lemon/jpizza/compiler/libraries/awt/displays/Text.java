package lemon.jpizza.compiler.libraries.awt.displays;

import lemon.jpizza.compiler.libraries.awt.config.JFont;

import java.awt.*;

public class Text extends Drawable {
    private final String text;
    private final int x, y;
    private final Color color;
    private final JFont font;

    public Text(String text, int x, int y, Color color, JFont font) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.font = font;
    }

    @Override
    public void draw(Graphics g) {
        if (font != null) {
            g.setFont(font.getFont());
        }
        g.drawString(text, x, y);
    }

    @Override
    public Color color() {
        return color;
    }
}
