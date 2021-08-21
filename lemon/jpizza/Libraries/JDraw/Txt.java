package lemon.jpizza.Libraries.JDraw;

import java.awt.*;

public class Txt implements DrawSlice {
    String msg;
    int x, y;
    Color color;

    public Txt(int x, int y, String msg, Color color) {
        this.msg = msg;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.drawString(msg, x, y);
    }
}
