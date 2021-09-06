package lemon.jpizza.Libraries.JDraw;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PizzaCanvas extends JPanel {
    ArrayList<DrawSlice> slices = new ArrayList<>();
    ConcurrentHashMap<Point, Rect> pixels = new ConcurrentHashMap<>();

    ArrayList<DrawSlice> _slices = new ArrayList<>();
    ConcurrentHashMap<Point, Rect> _pixels = new ConcurrentHashMap<>();
    boolean painting = false;

    boolean fontChanged = false;
    Fnt font = null;

    public void push(ArrayList<DrawSlice> slices, ConcurrentHashMap<Point, Rect> pixels) {
        _slices = slices;
        _pixels = pixels;
    }

    public void flush() {
        _slices.clear();
        _pixels.clear();
        slices.clear();
        pixels.clear();
    }

    public void setPixel(Point pixel, Color color) {
        Rect r = new Rect(pixel.x, pixel.y, 1, 1, color);

        if (pixels.containsKey(pixel)) {
            pixels.replace(pixel, r);
        } else
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

        HashSet<DrawSlice> sliceCopy = new HashSet<>(slices);
        for (DrawSlice slice: sliceCopy)
            slice.draw(g);

        for (Rect p : pixels.values())
            p.draw(g);
        painting = false;
    }

    public void draw(DrawSlice o) {
        slices.add(o);
    }

}
