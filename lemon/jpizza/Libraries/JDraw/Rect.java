package lemon.jpizza.Libraries.JDraw;

import java.awt.*;

public class Rect implements DrawSlice {
    public int x, y, width, height;
    public Color color;

    public Rect(int x, int y, int width, int height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }

}
