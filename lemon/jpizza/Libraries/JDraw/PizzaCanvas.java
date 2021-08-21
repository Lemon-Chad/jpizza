package lemon.jpizza.Libraries.JDraw;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class PizzaCanvas extends JPanel {
    ArrayList<DrawSlice> slices;
    ConcurrentHashMap<Point, Rect> pixels;

    ArrayList<DrawSlice> _slices;
    ConcurrentHashMap<Point, Rect> _pixels;
    boolean painting = false;

    boolean fontChanged = false;
    Fnt font = null;

    public void push(ArrayList<DrawSlice> slices, ConcurrentHashMap<Point, Rect> pixels) {
        _slices = slices;
        _pixels = pixels;
    }

    public void flush() {
        _slices = new ArrayList<>();
        _pixels = new ConcurrentHashMap<>();
        slices = new ArrayList<>();
        pixels = new ConcurrentHashMap<>();
    }

    public void setPixel(Point pixel, Color color) {
        Rect r = new Rect(pixel.x, pixel.y, 1, 1, color);

        if (pixels.containsKey(pixel))
            pixels.replace(pixel, r);
        else
            pixels.put(pixel, r);
    }

    public PizzaCanvas() {
        flush();
    }

    public void setFont(Fnt font) {
        fontChanged = true;
        this.font = font;
    }

    public void paintComponent(Graphics g) {
        pixels = _pixels;
        slices = _slices;
        super.paintComponent(g);
        if (font != null)
            g.setFont(font.asFont());

        var sliceCopy = new ArrayList<>(slices);
        for (DrawSlice o : sliceCopy)
            o.draw(g);

        for (Rect p : pixels.values())
            p.draw(g);
        painting = false;
    }

    public void draw(DrawSlice o) {
        slices.add(o);
    }

}
