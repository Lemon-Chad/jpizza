package lemon.jpizza.compiler.libraries.awt.config;

import lemon.jpizza.Shell;
import lemon.jpizza.compiler.libraries.awt.AbstractWindowToolkit;
import lemon.jpizza.compiler.libraries.awt.displays.Drawable;
import lemon.jpizza.compiler.libraries.awt.displays.Rectangle;
import lemon.jpizza.compiler.values.Value;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Window {
    private JFrame frame;
    private Canvas canvas;

    private Timer refreshLoop;

    private boolean fullscreen;
    private boolean changed;
    private boolean queue;

    private final List<Drawable> drawings;
    private final List<ColorSpan> colors;
    private final Map<Point, Rectangle> pixels;

    private JFont font;

    private final HashMap<Integer, Boolean> keyPressed;

    private final HashMap<Integer, Boolean> keyTyped;

    private int strokeSize;
    private int frames;

    private double start;

    private final boolean[] mouseButtons;

    public Window() {
        fullscreen = false;
        changed = false;
        queue = false;

        drawings = new ArrayList<>();
        colors = new ArrayList<>();
        pixels = new ConcurrentHashMap<>();

        strokeSize = -1;

        frames = 0;
        start = 1;

        keyPressed = new HashMap<>(){{
            for (Integer key : AbstractWindowToolkit.Keys.values())
                put(key, false);
        }};
        keyTyped = new HashMap<>(){{
            for (Integer key : AbstractWindowToolkit.Keys.values())
                put(key, false);
        }};

        mouseButtons = new boolean[]{ false, false, false };

        init();
    }

    private void change() {
        changed = true;
    }

    public Window isMain(boolean bool) {
        if (bool) {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
        else {
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }
        return this;
    }

    private void init() {
        frame = new JFrame("JPizzAwt");
        frame.setFocusTraversalKeysEnabled(false);

        canvas = new Canvas();
        canvas.setDoubleBuffered(true);
        canvas.setFocusable(true);
        canvas.setFocusTraversalKeysEnabled(false);
        canvas.requestFocusInWindow();

        try {
            URL url = new URL("https://raw.githubusercontent.com/Lemon-Chad/jpizza/main/pizzico512.png");
            Image image = ImageIO.read(url);
            frame.setIconImage(image);
        } catch (IOException ignored) {
            Shell.logger.warn("Failed to load icon");
        }

        MouseListener mouseListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            private void setMouseIndex(int button, boolean value) {
                int index = switch (button) {
                    case MouseEvent.BUTTON1 -> 0;
                    case MouseEvent.BUTTON2 -> 1;
                    case MouseEvent.BUTTON3 -> 2;
                    default -> -1;
                };

                if (index != -1)
                    mouseButtons[index] = value;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setMouseIndex(e.getButton(), true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setMouseIndex(e.getButton(), false);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        };
        canvas.addMouseListener(mouseListener);

        KeyListener kListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                keyPressed.put(e.getKeyCode(), true);
                keyTyped.put(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keyPressed.put(e.getKeyCode(), false);
            }
        };
        canvas.addKeyListener(kListener);

        start = System.currentTimeMillis();
    }

    public void flush() {
        change();
        if (queue) {
            drawings.clear();
            colors.clear();
            pixels.clear();
        }
        else {
            canvas.flush();
        }
    }

    private void repaint() {
        if (changed) {
            canvas.repaint();
            changed = false;
        }
        frames++;
    }

    public int getStrokeSize() {
        return strokeSize;
    }

    public JFont getFont() {
        return font;
    }

    public void refresh() {
        repaint();
    }

    public void draw(Drawable drawing) {
        change();
        if (queue) {
            Canvas.colorPush(drawing, drawings, colors);
        }
        else {
            canvas.draw(drawing);
        }
    }

    public void setPixel(Point pos, Color color) {
        change();
        if (queue) {
            Rectangle r = new Rectangle(pos.x, pos.y, 1, 1, color);
            if (pixels.containsKey(pos)) {
                pixels.replace(pos, r);
            }
            else {
                pixels.put(pos, r);
            }
        }
        else {
            canvas.setPixel(pos, color);
        }
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public void setSize(int width, int height) {
        Dimension dim = new Dimension(width, height);
        canvas.setPreferredSize(dim);
        change();
    }

    public void setIcon(Image icon) {
        frame.setIconImage(icon);
    }

    public void setFont(String name, String style, int size) {
        int format = switch (style) {
            case "B" -> Font.BOLD;
            case "I" -> Font.ITALIC;
            case "BI", "IB" -> Font.BOLD | Font.ITALIC;
            default -> Font.PLAIN;
        };

        if (size < 1) size = 1;

        font = new JFont(name, format, size);
    }

    public void setBackground(Color color) {
        canvas.setBackground(color);
        change();
    }

    public void lockSize(boolean bool) {
        frame.setResizable(!bool);
    }

    public void setStroke(int width) {
        strokeSize = Math.max(0, width);
    }

    public void close() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    public void open() {
        frame.add(canvas);
        frame.pack();
        frame.setVisible(true);
    }

    public void refresh(int delay) {
        if (refreshLoop != null)
            refreshLoop.stop();
        ActionListener listener = e -> refresh();
        refreshLoop = new Timer(delay, listener);
        refreshLoop.start();
    }

    public void stopLoop() {
        if (refreshLoop != null) {
            refreshLoop.stop();
            refreshLoop = null;
        }
    }

    public void q() {
        queue = !queue;
    }

    public void update() {
        if (changed)
            canvas.add(drawings, colors, pixels);
    }

    public double fps() {
        return 1000 * frames / (System.currentTimeMillis() - start);
    }

    public BufferedImage getImage() {
        BufferedImage img = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
        canvas.paint(img.getGraphics());
        return img;
    }

    public boolean getMouseDown(int key) {
        return mouseButtons[key];
    }

    public Point getMousePos() {
        Point p = canvas.getMousePosition();
        if (p != null) {
            return p;
        }
        return new Point(-1, -1);
    }

    public boolean mouseIn() {
        return canvas.getMousePosition() != null;
    }

    public boolean getKeyDown(Integer integer) {
        return keyPressed.get(integer);
    }

    public boolean getKeyTyped(Integer integer) {
        boolean wasTyped = keyTyped.get(integer);
        keyTyped.replace(integer, false);
        return wasTyped;
    }

    public String keyString() {
        StringBuilder sb = new StringBuilder();
        for (int key : keyTyped.keySet()) {
            if (keyTyped.get(key) &&
                AbstractWindowToolkit.KeyCode.containsKey(key) &&
                AbstractWindowToolkit.KeyCode.get(key).length() == 1) {
                sb.append(AbstractWindowToolkit.KeyCode.get(key));
            }
            keyTyped.replace(key, false);
        }
        return sb.toString();
    }

    public int getWidth() {
        return canvas.getWidth();
    }

    public int getHeight() {
        return canvas.getHeight();
    }
}
