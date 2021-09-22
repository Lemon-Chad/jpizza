package lemon.jpizza.libraries.jdraw;

import java.awt.*;

public class Ovl implements DrawSlice {
    public int x, y, width, height;
    public Color color;

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
