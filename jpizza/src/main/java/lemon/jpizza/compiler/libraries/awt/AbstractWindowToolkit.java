package lemon.jpizza.compiler.libraries.awt;

import lemon.jpizza.compiler.libraries.awt.config.Window;
import lemon.jpizza.compiler.libraries.awt.displays.Line;
import lemon.jpizza.compiler.libraries.awt.displays.Polygon;
import lemon.jpizza.compiler.libraries.awt.displays.Rectangle;
import lemon.jpizza.compiler.libraries.awt.displays.*;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.objects.TupleType;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static java.awt.event.KeyEvent.*;

public class AbstractWindowToolkit extends JPExtension {
    private static List<Window> windows = new ArrayList<>();
    private static Window window;
    private static Window focused;
    private static int index;

    private static final Type colorType = new TupleType(Types.INT, Types.INT, Types.INT);
    private static final Type pointType = new TupleType(Types.INT, Types.INT);

    public static final HashMap<String, Integer> Keys = new HashMap<String, Integer>(){{
        put("a", VK_A);
        put("b", VK_B);
        put("c", VK_C);
        put("d", VK_D);
        put("e", VK_E);
        put("f", VK_F);
        put("g", VK_G);
        put("h", VK_H);
        put("i", VK_I);
        put("j", VK_J);
        put("k", VK_K);
        put("l", VK_L);
        put("m", VK_M);
        put("n", VK_N);
        put("o", VK_O);
        put("p", VK_P);
        put("q", VK_Q);
        put("r", VK_R);
        put("s", VK_S);
        put("t", VK_T);
        put("u", VK_U);
        put("v", VK_V);
        put("w", VK_W);
        put("x", VK_X);
        put("y", VK_Y);
        put("z", VK_Z);

        put("up", VK_UP);
        put("down", VK_DOWN);
        put("left", VK_LEFT);
        put("right", VK_RIGHT);

        put("`", VK_BACK_QUOTE);
        put("'", VK_QUOTE);
        put("\"", VK_QUOTEDBL);

        put("0", VK_0);
        put("1", VK_1);
        put("2", VK_2);
        put("3", VK_3);
        put("4", VK_4);
        put("5", VK_5);
        put("6", VK_6);
        put("7", VK_7);
        put("8", VK_8);
        put("9", VK_9);

        put("!", VK_EXCLAMATION_MARK);
        put("@", VK_AT);
        put("#", VK_NUMBER_SIGN);
        put("$", VK_DOLLAR);
        put("^", VK_CIRCUMFLEX);
        put("&", VK_AMPERSAND);
        put("*", VK_ASTERISK);
        put("(", VK_LEFT_PARENTHESIS);
        put(")", VK_RIGHT_PARENTHESIS);

        put("-", VK_MINUS);
        put("=", VK_EQUALS);
        put("_", VK_UNDERSCORE);
        put("+", VK_PLUS);

        put("tab", VK_TAB);
        put("capslock", VK_CAPS_LOCK);
        put("enter", VK_ENTER);
        put("backspace", VK_BACK_SPACE);
        put("shift", VK_SHIFT);
        put("control", VK_CONTROL);

        put("[", VK_OPEN_BRACKET);
        put("]", VK_CLOSE_BRACKET);

        put("\\", VK_BACK_SLASH);

        put(";", VK_SEMICOLON);
        put(":", VK_COLON);

        put(",", VK_COMMA);
        put(".", VK_PERIOD);
        put("/", VK_SLASH);

        put(" ", VK_SPACE);
    }};

    public static final HashMap<Integer, String> KeyCode = new HashMap<Integer, String>(){{
        for (String key : Keys.keySet())
            put(Keys.get(key), key);
    }};

    @Override
    public String name() { return "awt"; }

    public AbstractWindowToolkit(VM vm) {
        super(vm);
    }

    private static int[] getColor(Value color) {
        Value[] values = color.asTuple();

        int[] c = new int[3];
        for (int i = 0; i < 3; i++) {
            c[i] = Math.min(255, Math.max(0, values[i].asNumber().intValue()));
        }

        return c;
    }

    private static JNative.Method ifInit(JNative.Method method) {
        return args -> {
            if (window == null) {
                return Err("Initialization", "AWT not initialized");
            }
            return method.call(args);
        };
    }

    private static Point getPoint(Value point) {
        Value[] p = point.asTuple();
        return new Point(p[0].asNumber().intValue(), p[1].asNumber().intValue());
    }

    private static void flush() {
        window.flush();
    }

    private static void refresh() {
        window.refresh();
    }

    private static void draw(Drawable drawing) {
        window.draw(drawing);
    }

    private static JNative.Method poly(boolean fill) {
        return ifInit(args -> {
            List<Value> pts = args[0].asList();
            Point[] points = new Point[pts.size()];
            for (int i = 0; i < pts.size(); i++) {
                Value p = pts.get(i);
                if (!p.type().equals("(int,int)"))
                    return Err("Type", "Expected (int,int)");
                points[i] = getPoint(p);
            }

            int[] color = getColor(args[1]);

            draw(new Polygon(
                    points,
                    new Color(color[0], color[1], color[2]),
                    fill,
                    window.getStrokeSize()
            ));
            return Ok;
        });
    }

    private static void setPixel(Point pos, Color color) {
        window.setPixel(pos, color);
    }

    @Override
    public void setup() {
        // Constants
        var("SAVE", 32.0, Types.INT);
        var("OPEN", 64.0, Types.INT);

        // Init
        func("init", args -> {
            windows = Collections.singletonList(window = new Window());
            window.isMain(true);
            focused = window;
            index = 0;
            return Ok;
        }, Types.VOID);

        // Config
        config();

        // Rendering
        rendering();

        // Drawing
        drawing();

        // Window Control
        windowControl();

        // Mouse + Keyboard
        input();

        // Output
        output();

    }

    private void config() {
        func("setTitle", ifInit(args -> {
            window.setTitle(args[0].asString());
            return Ok;
        }), Types.VOID, Types.STRING);
        func("setSize", ifInit(args -> {
            window.setSize(args[0].asNumber().intValue(), args[1].asNumber().intValue());
            return Ok;
        }), Types.VOID, Types.INT, Types.INT);
        func("setIcon", ifInit(args -> {
            String path = args[0].asString();

            try {
                Image img = ImageIO.read(new File(path));
                window.setIcon(img);
            } catch (IOException e) {
                return Err("Internal", "Could not load icon (" + e.getMessage() + ")");
            }
            return Ok;
        }), Types.VOID, Types.STRING);
        func("setFont", ifInit(args -> {
            window.setFont(
                    args[0].asString(),
                    args[1].asString(),
                    args[2].asNumber().intValue()
            );
            return Ok;
        }), Types.VOID, Types.STRING, Types.STRING, Types.INT);
        // I <3 Tuples
        // They make my life easier
        // I can simply make the type (num,num,num) and it will be parsed
        // Contrary to a list where I would have to check the size and type
        // of each element
        // So nice!
        func("setBackgroundColor", ifInit(args -> {
            int[] c = getColor(args[0]);
            window.setBackground(new Color(c[0], c[1], c[2]));
            return Ok;
        }), Types.VOID, colorType);
        func("lockSize", ifInit(args -> {
            window.lockSize(args[0].asBool());
            return Ok;
        }), Types.VOID, Types.BOOL);
        func("setStrokeSize", ifInit(args -> {
            window.setStroke(args[0].asNumber().intValue());
            return Ok;
        }), Types.VOID, Types.INT);
        func("exit", ifInit(args -> {
            window.close();
            windows.remove(window);
            if (windows.isEmpty())
                window = null;
            else
                window = windows.get(0);
            return Ok;
        }), Types.VOID);
    }

    private void rendering() {
        func("start", ifInit(args -> {
            window.open();
            return Ok;
        }), Types.VOID);
        func("clear", ifInit(args -> {
            flush();
            return Ok;
        }), Types.VOID);
        func("refresh", ifInit(args -> {
            refresh();
            return Ok;
        }), Types.VOID);
        func("refreshLoop", ifInit(args -> {
            window.refresh(args[0].asNumber().intValue());
            return Ok;
        }), Types.INT);
        func("refreshUnloop", ifInit(args -> {
            window.stopLoop();
            return Ok;
        }) , Types.VOID);
        func("screenshot", ifInit(args -> {
            String filename = args[0].asString();

            BufferedImage img = window.getImage();
            File file = new File("." + File.separator + filename);

            try {
                boolean created = file.createNewFile();
                ImageIO.write(img, "jpeg", file);
                return Ok(created);
            } catch (IOException e) {
                return Err("Internal", "Could not save screenshot (" + e.getMessage() + ")");
            }
        }), Types.BOOL, Types.STRING);
        func("fps", ifInit(args -> Ok(window.fps())), Types.FLOAT);
        func("gpuCompute", args -> {
            System.setProperty("sun.java2d.opengl", args[0].asBool() ? "true" : "false");
            return Ok;
        }, Types.VOID, Types.BOOL);

        // QRendering is an alternative render method
        // It puts all the elements in a queue and renders them once the
        // update method is called
        // This prevents flickering and buggy rendering
        // It also allows for a more fluid rendering
        func("toggleQRender", args -> {
            window.q();
            return Ok;
        }, Types.VOID);
        // Alternative name for the same function
        func("qrender", args -> {
            window.q();
            return Ok;
        }, Types.VOID);
        func("qUpdate", ifInit(args -> {
            window.update();
            return Ok;
        }), Types.VOID);
    }

    private void drawing() {
        func("drawPoly", poly(true), Types.VOID, Types.LIST, colorType);
        func("tracePoly", poly(false), Types.VOID, Types.LIST, colorType);
        func("drawOval", ifInit(args -> {
            int x = args[0].asNumber().intValue();
            int y = args[1].asNumber().intValue();
            int w = args[2].asNumber().intValue();
            int h = args[3].asNumber().intValue();
            int[] c = getColor(args[4]);

            draw(new Oval(
                    x - w / 2,
                    y - h / 2,
                    w,
                    h,
                    new Color(c[0], c[1], c[2])
            ));
            return Ok;
        }), Types.VOID, Types.INT, Types.INT, Types.INT, Types.INT, colorType);
        func("drawCircle", ifInit(args -> {
            int r = args[0].asNumber().intValue();
            int x = args[1].asNumber().intValue();
            int y = args[2].asNumber().intValue();
            int[] c = getColor(args[3]);

            draw(new Oval(
                    x - r / 2,
                    y - r / 2,
                    r,
                    r,
                    new Color(c[0], c[1], c[2])
            ));
            return Ok;
        }), Types.VOID, Types.INT, Types.INT, Types.INT, colorType);
        func("drawRect", ifInit(args -> {
            int x = args[0].asNumber().intValue();
            int y = args[1].asNumber().intValue();
            int w = args[2].asNumber().intValue();
            int h = args[3].asNumber().intValue();
            int[] c = getColor(args[4]);

            draw(new Rectangle(
                    x - w / 2,
                    y - h / 2,
                    w,
                    h,
                    new Color(c[0], c[1], c[2])
            ));
            return Ok;
        }), Types.VOID, Types.INT, Types.INT, Types.INT, Types.INT, colorType);
        func("drawSquare", ifInit(args -> {
            int r = args[0].asNumber().intValue();
            int x = args[1].asNumber().intValue();
            int y = args[2].asNumber().intValue();
            int[] c = getColor(args[3]);

            draw(new Rectangle(
                    x - r / 2,
                    y - r / 2,
                    r,
                    r,
                    new Color(c[0], c[1], c[2])
            ));
            return Ok;
        }), Types.VOID, Types.INT, Types.INT, Types.INT, colorType);
        func("drawText", ifInit(args -> {
            String text = args[0].asString();
            int x = args[1].asNumber().intValue();
            int y = args[2].asNumber().intValue();
            int[] c = getColor(args[3]);

            draw(new Text(
                    text,
                    x,
                    y,
                    new Color(c[0], c[1], c[2]),
                    window.getFont()
            ));
            return Ok;
        }), Types.VOID, Types.STRING, Types.INT, Types.INT, colorType);
        func("drawImage", ifInit(args -> {
            String path = args[0].asString();
            int x = args[1].asNumber().intValue();
            int y = args[2].asNumber().intValue();

            try {
                draw(new Img(path, x, y));
                return Ok;
            } catch (IOException e) {
                return Err("Internal", "Could not load image (" + e.getMessage() + ")");
            }
        }), Types.VOID, Types.STRING, Types.INT, Types.INT);
        func("sizedImage", ifInit(args -> {
            String path = args[0].asString();
            int x = args[1].asNumber().intValue();
            int y = args[2].asNumber().intValue();
            int w = args[3].asNumber().intValue();
            int h = args[4].asNumber().intValue();

            try {
                draw(new Img(path, x, y, w, h));
                return Ok;
            } catch (IOException e) {
                return Err("Internal", "Could not load image (" + e.getMessage() + ")");
            }
        }), Types.VOID, Types.STRING, Types.INT, Types.INT, Types.INT, Types.INT);
        func("setPixel", ifInit(args -> {
            int x = args[0].asNumber().intValue();
            int y = args[1].asNumber().intValue();
            int[] c = getColor(args[2]);
            setPixel(new Point(x, y), new Color(c[0], c[1], c[2]));
            return Ok;
        }), Types.VOID, Types.INT, Types.INT, colorType);
        func("drawLine", ifInit(args -> {
            Point start = getPoint(args[0]);
            Point end = getPoint(args[1]);
            int[] c = getColor(args[2]);

            draw(new Line(start, end, new Color(c[0], c[1], c[2]), window.getStrokeSize()));
            return Ok;
        }), Types.VOID, pointType, pointType, colorType);
    }

    private void windowControl() {
        func("windowCount", args -> Ok(windows.size()), Types.INT);
        func("windowIndex", args -> Ok(index), Types.INT);
        func("createWindow", args -> {
            windows.add(new Window().isMain(false));
            return Ok(windows.size() - 1);
        }, Types.INT);
        func("setWindow", args -> {
            index = args[0].asNumber().intValue();
            if (index >= windows.size() || index < 0) {
                return Err("Out of Bounds", "Window index out of bounds");
            }
            window = windows.get(index);
            return Ok;
        }, Types.VOID, Types.INT);
        func("focusWindow", ifInit(args -> {
            window.isMain(true);
            if (focused != null)
                focused.isMain(false);
            focused = window;
            return Ok;
        }), Types.VOID);
        func("width", args -> Ok(window.getWidth()) , Types.INT);
        func("height", args -> Ok(window.getHeight()) , Types.INT);
    }

    private void input() {
        // Mouse
        func("mouseDown", ifInit(args -> {
            int key = args[0].asNumber().intValue();
            if (key < 0 || key >= 3) {
                return Err("Out of Bounds", "Mouse key out of bounds");
            }
            return Ok(window.getMouseDown(key));
        }), Types.BOOL, Types.INT);
        func("mousePos", ifInit(args -> {
            Point pos = window.getMousePos();
            return Ok(new Value(new Value(pos.x), new Value(pos.y)));
        }), pointType);
        func("mouseIn", ifInit(args -> Ok(window.mouseIn())), Types.BOOL);

        // Keyboard
        func("keyDown", ifInit(args -> {
            String key = args[0].asString();
            if (!Keys.containsKey(key)) {
                return Err("Argument", "Key not found");
            }
            return Ok(window.getKeyDown(Keys.get(key)));
        }), Types.BOOL, Types.STRING);
        func("keyTyped", ifInit(args -> {
            String key = args[0].asString();
            if (!Keys.containsKey(key)) {
                return Err("Argument", "Key not found");
            }
            return Ok(window.getKeyTyped(Keys.get(key)));
        }), Types.BOOL, Types.STRING);
        // Returns all typed keys in a string
        func("keyString", ifInit(args -> Ok(window.keyString())), Types.STRING);

    }

    private void output() {
        // Audio
        func("playSound", ifInit(args -> {
            String path = args[0].asString();
            try {
                File file = new File(path);
                if (!file.exists()) {
                    return Err("Imaginary File", "File not found");
                }
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
                return Ok;
            } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
                return Err("Internal", "Could not load audio (" + e.getMessage() + ")");
            }
        }), Types.VOID, Types.STRING);
        // File
        func("chooseFile", args -> {
            String path = args[0].asString();
            JFileChooser fileChooser = new JFileChooser(path);

            Value fil = args[1];
            String filType = fil.type();
            if (filType.equals("(String,String)")) {
                List<Value> fils = fil.asList();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(fils.get(0).asString(), fils.get(1).asString());
                fileChooser.addChoosableFileFilter(filter);
                fileChooser.setFileFilter(filter);
            }
            else if (!filType.equals("void")) {
                return Err("Type", "Invalid file filter");
            }

            int res;
            switch (args[2].asNumber().intValue()) {
                case 32:
                    res = fileChooser.showSaveDialog(null);
                    break;
                case 64:
                    res = fileChooser.showOpenDialog(null);
                    break;
                default:
                    return Err("Type", "Invalid file dialog type");
            }

            if (res == JFileChooser.APPROVE_OPTION) {
                return Ok(fileChooser.getSelectedFile().getAbsolutePath());
            }
            else {
                return Ok;
            }
        }, Types.VOID, Types.STRING, Types.ANY, Types.INT);
    }

}
