package lemon.jpizza.Libraries.JDraw;

import java.awt.*;

public class Polygon implements DrawSlice {
    int[] x;
    int[] y;
    Color color;
    boolean outline;

    public Polygon(Point[] points, Color color, boolean outline) {
        int s = points.length;

        this.x = new int[s];
        this.y = new int[s];

        int i = 0;
        for (; i < s; i++) {
            x[i] = points[i].x;
            y[i] = points[i].y;
        }

        this.color = color;
        this.outline = outline;
    }

    public void draw(Graphics g) {
        if (g.getColor() != color)
            g.setColor(color);
        if (outline)
            g.drawPolygon(x, y, x.length);
        else
            g.fillPolygon(x, y, x.length);
    }

}
