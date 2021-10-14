package lemon.jpizza.libraries.jdraw;

import java.awt.*;

public class Line implements DrawSlice {
    Point start;
    Point end;
    Color color;
    Integer width;

    public Line(Point start, Point end, Color color, Integer width) {
        this.start = start;
        this.end = end;
        this.width = width;
        this.color = color;
    }

    public void draw(Graphics g) {
        if (width != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(width));
        }
        g.setColor(color);
        g.drawLine(start.x, start.y, end.x, end.y);
    }
}
