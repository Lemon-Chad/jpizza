package lemon.jpizza.compiler.libraries.awt.displays;

import lemon.jpizza.compiler.libraries.awt.config.Canvas;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Img extends Drawable {
    private final int x, y;
    private Image img;

    public Img(String path, int x, int y) throws IOException {
        // Load file with ImageIcon
        URL url;

        File file = new File(path);
        if (file.exists()) {
            url = file.toURI().toURL();
        }
        else {
            url = new URL(path);
        }
        ImageIcon icon = new ImageIcon(url);
        img = icon.getImage();

        // Set position
        this.x = x;
        this.y = y;
    }

    public Img(String path, int x, int y, int w, int h) throws IOException {
        this(path, x, y);
        img = img.getScaledInstance(w, h, Image.SCALE_FAST);
    }

    @Override
    public Color color() {
        return null;
    }

    @Override
    public void draw(Graphics g, Canvas canvas) {
        g.drawImage(img, x, y, canvas);
    }

    @Override
    protected void draw(Graphics g) {
        // Do nothing
    }
}
