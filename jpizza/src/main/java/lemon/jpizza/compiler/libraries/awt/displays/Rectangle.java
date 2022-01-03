package lemon.jpizza.compiler.libraries.awt.displays;

import lemon.jpizza.libraries.jdraw.Rect;

import java.awt.*;

public class Rectangle extends Drawable {
    private final int x, y;
    private final int w, h;
    private final Color color;

    public Rectangle(int x, int y, int w, int h, Color color) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.color = color;
    }

    @Override
    public void draw(Graphics g) {
        g.fillRect(x, y, w, h);
    }

    @Override
    public Color color() {
        return color;
    }
}
