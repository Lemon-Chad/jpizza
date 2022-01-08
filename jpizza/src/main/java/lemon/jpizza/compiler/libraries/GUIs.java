package lemon.jpizza.compiler.libraries;

import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class GUIs extends JPExtension {

    @Override
    public String name() { return "guis"; }

    public GUIs(VM vm) {
        super(vm);
    }

    @Override
    public void setup() {

        func("createGUI", (args) -> {

            JFrame frame;
            
            frame = new JFrame(Arrays.toString(args));
            frame.setTitle(Arrays.toString(args));
            frame.setFocusTraversalKeysEnabled(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            try {
                URL url = new URL("https://raw.githubusercontent.com/Lemon-Chad/jpizza/main/pizzico512.png");
                Image image = ImageIO.read(url);
                frame.setIconImage(image);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return Ok;
        }, List.of("String"));

    }

}
