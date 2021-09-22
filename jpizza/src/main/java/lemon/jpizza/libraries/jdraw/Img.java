package lemon.jpizza.libraries.jdraw;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Img implements DrawSlice {
    int x, y;
    Image img;

    public Img(int x, int y, String filename) throws IOException {
        img = ImageIO.read(new File(filename));

        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(Graphics g) {
        g.drawImage(img, x, y, null);
    }
}
