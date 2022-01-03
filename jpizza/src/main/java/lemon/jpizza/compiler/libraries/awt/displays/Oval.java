package lemon.jpizza.compiler.libraries.awt.displays;

import java.awt.*;

public class Oval extends Drawable {
    private final int x, y;
    private final int w, h;
    private final Color color;

    public Oval(int x, int y, int w, int h, Color color) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.color = color;
    }

    @Override
    public Color color() {
        return color;
    }

    @Override
    public void draw(Graphics g) {
        g.fillOval(x, y, w, h);
    }
}
