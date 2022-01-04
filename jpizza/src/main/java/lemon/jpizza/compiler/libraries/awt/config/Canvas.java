package lemon.jpizza.compiler.libraries.awt.config;

import lemon.jpizza.compiler.libraries.awt.displays.Drawable;
import lemon.jpizza.compiler.libraries.awt.displays.Rectangle;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Canvas extends JPanel {
    Queue<List<Drawable>> drawings;
    Queue<List<ColorSpan>> colors;
    Queue<Map<Point, Rectangle>> pixels;
    boolean painting;
    JFont font;

    public Canvas() {
        drawings = new Queue<>(new ArrayList<>());
        colors   = new Queue<>(new ArrayList<>());
        pixels   = new Queue<>(new ConcurrentHashMap<>());
        painting = false;
        font = null;
    }

    public void flush() {
        drawings.get().clear();
        drawings.next().clear();
        colors.get().clear();
        colors.next().clear();
        pixels.get().clear();
        pixels.next().clear();
    }

    public void add(List<Drawable> drawables, List<ColorSpan> colors, Map<Point, Rectangle> pixels) {
        this.drawings.add(drawables);
        this.colors.add(colors);
        this.pixels.add(pixels);
    }

    public static void colorPush(Drawable drawing, List<Drawable> drawables, List<ColorSpan> colors) {
        if (colors.size() > 0 && Objects.equals(drawing.color(), colors.get(colors.size() - 1).color)) {
            colors.get(colors.size() - 1).span++;
        }
        else {
            colors.add(new ColorSpan(1, drawing.color()));
        }
        drawables.add(drawing);
    }

    public void setPixel(Point point, Color color) {
        Rectangle rect = new Rectangle(point.x, point.y, 1, 1, color);

        if (pixels.get().containsKey(point)) {
            pixels.get().replace(point, rect);
        }
        else {
            pixels.get().put(point, rect);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        pixels.advance();
        drawings.advance();
        colors.advance();

        List<Drawable> drawings = new ArrayList<>(this.drawings.get());
        List<ColorSpan> colors = new ArrayList<>(this.colors.get());
        Map<Point, Rectangle> pixels = new ConcurrentHashMap<>(this.pixels.get());

        if (font != null) {
            g.setFont(font.getFont());
        }

        for (Rectangle rect : pixels.values()) {
            g.setColor(rect.color());
            rect.draw(g);
        }

        int i = 0;
        for (int j = 0; j < colors.size() && i < drawings.size(); j++) {
            ColorSpan span = colors.get(j);
            if (span.color != null)
                g.setColor(span.color);
            for (int k = 0; k < span.span && i + k < drawings.size(); k++) {
                Drawable drawing = drawings.get(i + k);
                drawing.draw(g, this);
            }
            i += span.span;
        }
    }

    public void draw(Drawable drawing) {
        colorPush(drawing, drawings.get(), colors.get());
    }

}
