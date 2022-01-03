package lemon.jpizza.compiler.libraries.awt.displays;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Img extends Drawable {
    private final int x, y;
    private final Image img;

    public Img(String filename, int x, int y) throws IOException {
        this.img = ImageIO.read(new File(filename));
        this.x = x;
        this.y = y;
    }

    @Override
    public Color color() {
        return null;
    }

    @Override
    public void draw(Graphics g) {
        g.drawImage(img, x, y, null);
    }
}
