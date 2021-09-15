package lemon.jpizza.Libraries.JDraw;

import java.awt.*;

public class Txt implements DrawSlice {
    String msg;
    int x, y;
    Color color;
    Fnt fnt;

    public Txt(int x, int y, String msg, Color color, Fnt fnt) {
        this.msg = msg;
        this.x = x;
        this.y = y;
        this.fnt = fnt;
        this.color = color;
    }

    public void draw(Graphics g) {
        g.setFont(fnt.asFont());
        if (g.getColor() != color)
            g.setColor(color);
        g.drawString(msg, x, y);
    }
}
