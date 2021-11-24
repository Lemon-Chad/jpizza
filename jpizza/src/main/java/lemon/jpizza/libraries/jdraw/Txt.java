package lemon.jpizza.libraries.jdraw;

import java.awt.*;

public class Txt implements DrawSlice {
    final String msg;
    final int x;
    final int y;
    final Color color;
    final Fnt fnt;

    public Txt(int x, int y, String msg, Color color, Fnt fnt) {
        this.msg = msg;
        this.x = x;
        this.y = y;
        this.fnt = fnt;
        this.color = color;
    }

    public void draw(Graphics g) {
        if (fnt != null)
            g.setFont(fnt.asFont());
        if (!g.getColor().equals(color))
            g.setColor(color);
        g.drawString(msg, x, y);
    }
}
