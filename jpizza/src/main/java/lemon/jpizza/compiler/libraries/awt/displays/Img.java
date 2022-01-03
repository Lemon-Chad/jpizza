package lemon.jpizza.compiler.libraries.awt.displays;

import lemon.jpizza.compiler.libraries.awt.config.Canvas;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

public class Img extends Drawable {
    private final int x, y;
    private final Image img;

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
