package lemon.jpizza.compiler.libraries.awt.displays;

import java.awt.*;

public class Polygon extends Drawable {
    private final int[] x, y;
    private final Color color;
    private final boolean filled;
    private final int width;

    public Polygon(Point[] points, Color color, boolean filled, int width) {
        this.x = new int[points.length];
        this.y = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            x[i] = points[i].x;
            y[i] = points[i].y;
        }
        this.color = color;
        this.filled = filled;
        this.width = width;
    }

    @Override
    public void draw(Graphics g) {
        if (width != -1) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(width));
        }

        if (filled) {
            g.fillPolygon(x, y, x.length);
        }
        else {
            g.drawPolygon(x, y, x.length);
        }
    }

    @Override
    public Color color() {
        return color;
    }
}
