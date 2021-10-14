package lemon.jpizza.libraries.jdraw;

import java.awt.*;

public class Polygon implements DrawSlice {
    final int[] x;
    final int[] y;
    final Color color;
    final boolean outline;
    final Integer width;

    public Polygon(Point[] points, Color color, boolean outline, Integer width) {
        int s = points.length;

        this.x = new int[s];
        this.y = new int[s];

        this.width = width;

        int i = 0;
        for (; i < s; i++) {
            x[i] = points[i].x;
            y[i] = points[i].y;
        }

        this.color = color;
        this.outline = outline;
    }

    public void draw(Graphics g) {
        if (width != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(width));
        }

        if (g.getColor() != color)
            g.setColor(color);

        if (outline)
            g.drawPolygon(x, y, x.length);
        else
            g.fillPolygon(x, y, x.length);
    }

}
