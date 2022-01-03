package lemon.jpizza.compiler.libraries.awt.displays;

import java.awt.*;

public class Line extends Drawable {
    private final Point start, end;
    private final Color color;
    private final int width;

    public Line(Point start, Point end, Color color, int width) {
        this.start = start;
        this.end = end;
        this.color = color;
        this.width = width;
    }

    @Override
    public Color color() {
        return color;
    }

    @Override
    public void draw(Graphics g) {
        if (width != -1) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(width));
        }
        g.drawLine(start.x, start.y, end.x, end.y);
    }
}
