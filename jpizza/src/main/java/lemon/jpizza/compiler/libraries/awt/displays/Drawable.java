package lemon.jpizza.compiler.libraries.awt.displays;

import lemon.jpizza.compiler.libraries.awt.config.Canvas;

import java.awt.*;

public abstract class Drawable {
    public void draw(Graphics g, Canvas canvas) {
        draw(g);
    }
    protected abstract void draw(Graphics g);

    public abstract Color color();
}
