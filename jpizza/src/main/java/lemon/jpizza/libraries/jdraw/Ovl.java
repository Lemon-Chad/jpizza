package lemon.jpizza.libraries.jdraw;

import java.awt.*;

public class Ovl implements DrawSlice {
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    public final Color color;

    public Ovl(int x, int y, int width, int height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    public void draw(Graphics g) {
        if (g.getColor() != color)
            g.setColor(color);
        g.fillOval(x, y, width, height);
    }
}
