package lemon.jpizza.Libraries.JDraw;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.Error;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Pair;
import lemon.jpizza.Results.RTResult;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.awt.event.KeyEvent.*;

@SuppressWarnings("unused")
public class JDraw extends Library {
    static JFrame frame;
    static PizzaCanvas canvas;

    static Timer refreshLoop;

    static boolean changed = false;

    static Fnt font;

    static int frames = 0;
    static double start = 1;

    static boolean[] mouseButtons = { false, false, false };
    static Point mousePos = new Point(0, 0);

    static HashMap<String, Integer> keys = new HashMap<>(){{
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

    static HashMap<Integer, String> keycode = new HashMap<>(){{
        for (String key : keys.keySet())
            put(keys.get(key), key);
    }};

    static HashMap<Integer, Boolean> keypressed = new HashMap<>(){{
        for (Integer key : keys.values())
            put(key, false);
    }};

    static HashMap<Integer, Boolean> keytyped = new HashMap<>(){{
        for (Integer key : keys.values())
            put(key, false);
    }};

    static boolean queue = false;
    static Set<DrawSlice> slices = new HashSet<>();
    static ConcurrentHashMap<Point, Rect> pixels = new ConcurrentHashMap<>();

    public JDraw(String name) {
        super(name);
    }

    public static Pair<Integer[], Error> getColor(Object col) {
        RTResult res = new RTResult();

        Obj lis = res.register(checkType(col, "list", Constants.JPType.List));
        if (res.error != null) return new Pair<>(null, res.error);

        List<Obj> list = ((PList) lis).trueValue();
        String errmsg = "Expected list composed of 3 0-255 integers";
        if (list.size() != 3) return new Pair<>(null, new RTError(
                lis.get_start(), lis.get_end(),
                errmsg,
                lis.get_ctx()
        ));

        Integer[] color = new Integer[3];
        for (int i = 0; i < 3; i++) {
            Obj obj = list.get(i);
            res.register(checkInt(obj));
            if (res.error != null) return new Pair<>(null, res.error);
            int num = (int)((Num)obj).trueValue();
            if (0 > num || num > 255) return new Pair<>(null, new RTError(
                    obj.get_start(), obj.get_end(),
                    errmsg,
                    obj.get_ctx()
            ));
            color[i] = num;
        }

        return new Pair<>(color, null);
    }

    public RTResult isInit() {
        if (frame == null || canvas == null) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "AWT not initialized",
                context
        ));
        return new RTResult().success(null);
    }

    @SuppressWarnings("DuplicatedCode")
    public static Pair<Point, Error> getCoords(Context ctx) {
        RTResult res = new RTResult();
        Obj cx = res.register(checkInt(ctx.symbolTable.get("x")));
        Obj cy = res.register(checkInt(ctx.symbolTable.get("y")));

        if (res.error != null) return new Pair<>(null, res.error);

        int x = (int)((Num) cx).trueValue();
        int y = (int)((Num) cy).trueValue();
        return new Pair<>(new Point(x, y), null);
    }

    @SuppressWarnings("DuplicatedCode")
    public static Pair<Point, Error> getDimensions(Context ctx) {
        RTResult res = new RTResult();
        Obj width = res.register(checkInt(ctx.symbolTable.get("width")));
        Obj height = res.register(checkInt(ctx.symbolTable.get("height")));

        if (res.error != null) return new Pair<>(null, res.error);

        int w = (int)((Num) width).trueValue();
        int h = (int)((Num) height).trueValue();
        return new Pair<>(new Point(w, h), null);
    }

    public RTResult execute_setBackgroundColor(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj col = (Obj) execCtx.symbolTable.get("color");

        Pair<Integer[], Error> r = getColor(col);
        if (r.b != null) return res.failure(r.b);
        Color color = new Color(r.a[0], r.a[1], r.a[2]);

        canvas.setBackground(color);
        changed = true;
        return res.success(new Null());
    }

    public RTResult execute_init(Context execCtx) {
        frame = new JFrame("JPizzAwt");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        start = System.currentTimeMillis();

        canvas = new PizzaCanvas();
        canvas.setDoubleBuffered(true);
        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

        try {
            URL url = new URL("https://raw.githubusercontent.com/Lemon-Chad/jpizza/main/pizzico512.png");
            Image image = ImageIO.read(url);
            frame.setIconImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        }


        MouseListener mListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mousePos = e.getPoint();
            }

            @Override
            @SuppressWarnings("DuplicatedCode")
            public void mousePressed(MouseEvent e) {
                mousePos = e.getPoint();
                int index = switch (e.getButton()) {
                    case MouseEvent.BUTTON1 -> 0;
                    case MouseEvent.BUTTON2 -> 1;
                    case MouseEvent.BUTTON3 -> 2;
                    default -> -1;
                };
                if (index != -1) mouseButtons[index] = true;
            }

            @Override
            @SuppressWarnings("DuplicatedCode")
            public void mouseReleased(MouseEvent e) {
                mousePos = e.getPoint();
                int index = switch (e.getButton()) {
                    case MouseEvent.BUTTON1 -> 0;
                    case MouseEvent.BUTTON2 -> 1;
                    case MouseEvent.BUTTON3 -> 2;
                    default -> -1;
                };
                if (index != -1) mouseButtons[index] = false;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                mousePos = e.getPoint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mousePos = new Point(0, 0);
            }
        };
        canvas.addMouseListener(mListener);

        KeyListener kListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                keypressed.put(e.getKeyCode(), true);
                keytyped.put(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keypressed.put(e.getKeyCode(), false);
            }
        };
        canvas.addKeyListener(kListener);

        return new RTResult().success(new Null());
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_drawCircle(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj rad = res.register(checkPosInt(execCtx.symbolTable.get("radius")));
        if (res.error != null) return res;

        int radius = (int)((Num) rad).trueValue();

        Pair<Point, Error> p = getCoords(execCtx);
        if (p.b != null) return res.failure(p.b);
        Point pos = p.a;

        Pair<Integer[], Error> r = getColor(execCtx.symbolTable.get("color"));
        if (r.b != null) return res.failure(r.b);
        Color color = new Color(r.a[0], r.a[1], r.a[2]);

        draw(new Ovl(pos.x - radius, pos.y - radius, radius * 2, radius * 2, color));
        return res.success(new Null());
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_drawSquare(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj rad = res.register(checkPosInt(execCtx.symbolTable.get("radius")));
        if (res.error != null) return res;

        int radius = (int)((Num) rad).trueValue();

        Pair<Point, Error> p = getCoords(execCtx);
        if (p.b != null) return res.failure(p.b);
        Point pos = p.a;

        Pair<Integer[], Error> r = getColor(execCtx.symbolTable.get("color"));
        if (r.b != null) return res.failure(r.b);
        Color color = new Color(r.a[0], r.a[1], r.a[2]);

        draw(new Rect(pos.x - radius / 2, pos.y - radius / 2, radius, radius, color));
        return res.success(new Null());
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_drawOval(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj width = res.register(checkPosInt(execCtx.symbolTable.get("width")));
        Obj height = res.register(checkPosInt(execCtx.symbolTable.get("height")));
        if (res.error != null) return res;

        Pair<Point, Error> p = getCoords(execCtx);
        if (p.b != null) return res.failure(p.b);
        Point pos = p.a;

        p = getDimensions(execCtx);
        if (p.b != null) return res.failure(p.b);
        Point dim = p.a;

        Pair<Integer[], Error> r = getColor(execCtx.symbolTable.get("color"));
        if (r.b != null) return res.failure(r.b);
        Color color = new Color(r.a[0], r.a[1], r.a[2]);

        draw(new Ovl(pos.x - dim.x / 2, pos.y - dim.y / 2, dim.x, dim.y, color));
        return res.success(new Null());
    }

    public RTResult poly(Context execCtx, boolean outln) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj lx = res.register(checkType(execCtx.symbolTable.get("points"), "list", Constants.JPType.List));
        if (res.error != null) return res;
        List<Obj> lst = ((PList) lx).trueValue();

        Point[] points = new Point[lst.size()];
        for (int i = 0; i < lst.size(); i++) {
            Obj p = lst.get(i);

            res.register(checkType(p, "list", Constants.JPType.List));
            if (res.error != null) return res;
            List<Obj> pL = ((PList) p).trueValue();

            if (pL.size() != 2) return res.failure(new RTError(
                    p.get_start(), p.get_end(),
                    "Expected coordinates (list of 2 numbers)",
                    context
            ));

            res.register(checkInt(pL.get(0)));
            res.register(checkInt(pL.get(1)));
            if (res.error != null) return res;

            int x = (int)((Num) pL.get(0)).trueValue();
            int y = (int)((Num) pL.get(1)).trueValue();

            points[i] = new Point(x, y);
        }

        Pair<Integer[], Error> r = getColor(execCtx.symbolTable.get("color"));
        if (r.b != null) return res.failure(r.b);
        Color color = new Color(r.a[0], r.a[1], r.a[2]);

        draw(new Polygon(points, color, outln));
        return res.success(new Null());
    }

    public RTResult execute_drawPoly(Context execCtx) {
        return poly(execCtx, false);
    }

    public RTResult execute_tracePoly(Context execCtx) {
        return poly(execCtx, true);
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_drawRect(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj width = res.register(checkPosInt(execCtx.symbolTable.get("width")));
        Obj height = res.register(checkPosInt(execCtx.symbolTable.get("height")));
        if (res.error != null) return res;

        Pair<Point, Error> p = getCoords(execCtx);
        if (p.b != null) return res.failure(p.b);
        Point pos = p.a;

        p = getDimensions(execCtx);
        if (p.b != null) return res.failure(p.b);
        Point dim = p.a;

        Pair<Integer[], Error> r = getColor(execCtx.symbolTable.get("color"));
        if (r.b != null) return res.failure(r.b);
        Color color = new Color(r.a[0], r.a[1], r.a[2]);

        draw(new Rect(pos.x - dim.x / 2, pos.y - dim.y / 2, dim.x, dim.y, color));
        return res.success(new Null());
    }

    public RTResult execute_setPixel(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Pair<Integer[], Error> r = getColor(execCtx.symbolTable.get("color"));
        if (r.b != null) return res.failure(r.b);
        Color color = new Color(r.a[0], r.a[1], r.a[2]);

        Pair<Point, Error> p = getCoords(execCtx);
        if (p.b != null) return res.failure(p.b);
        Point pos = p.a;

        if (res.error != null) return res;
        setPixel(pos, color);
        return res.success(new Null());
    }

    public RTResult execute_setTitle(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj value = (Obj) execCtx.symbolTable.get("value");
        frame.setTitle(value.toString());

        return res.success(new Null());
    }

    public RTResult execute_lockSize(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj value = ((Obj) execCtx.symbolTable.get("value")).bool();
        if (value.jptype != Constants.JPType.Boolean) return res.failure(new RTError(
                value.get_start(), value.get_end(),
                "Expected bool",
                execCtx
        ));
        frame.setResizable(!((Bool) value).trueValue());

        return res.success(new Null());
    }

    public RTResult execute_gpuCompute(Context execCtx) {
        RTResult res = new RTResult();

        Obj value = ((Obj) execCtx.symbolTable.get("value")).bool();
        if (value.jptype != Constants.JPType.Boolean) return res.failure(new RTError(
                value.get_start(), value.get_end(),
                "Expected bool",
                execCtx
        ));
        System.setProperty("sun.java2d.opengl", ((Bool) value).trueValue() ? "true" : "false");

        return res.success(new Null());
    }

    public RTResult execute_setFont(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        String name = execCtx.symbolTable.get("fontName").toString();

        String fontT = execCtx.symbolTable.get("fontType").toString();
        int fontType = switch (fontT) {
            case "B" -> Font.BOLD;
            case "I" -> Font.ITALIC;
            default -> Font.PLAIN;
        };

        Obj s = res.register(checkPosInt(execCtx.symbolTable.get("fontSize")));
        if (res.error != null) return res;
        int fontSize = (int)((Num)s).trueValue();

        font = new Fnt(name, fontType, fontSize);

        return res.success(new Null());
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_setSize(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj na = res.register(checkPosInt(execCtx.symbolTable.get("width")));
        Obj nb = res.register(checkPosInt(execCtx.symbolTable.get("height")));

        if (res.error != null) return res;

        Num width = (Num) na; Num height = (Num) nb;

        Dimension dim = new Dimension((int) width.trueValue(), (int) height.trueValue());

        canvas.setPreferredSize(dim);
        changed = true;

        return res.success(new Null());
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_drawText(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Pair<Point, Error> p = getCoords(execCtx);
        if (p.b != null) return res.failure(p.b);
        Point pos = p.a;

        Pair<Integer[], Error> col = getColor(execCtx.symbolTable.get("color"));
        if (p.b != null) return res.failure(p.b);
        Color color = new Color(col.a[0], col.a[1], col.a[2]);

        Obj txt = res.register(checkType(execCtx.symbolTable.get("txt"), "String", Constants.JPType.String));
        if (res.error != null) return res;
        String msg = ((Str)txt).trueValue();

        draw(new Txt(pos.x, pos.y, msg, color, font));
        return res.success(new Null());
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_drawImage(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Pair<Point, Error> p = getCoords(execCtx);
        if (p.b != null) return res.failure(p.b);
        Point pos = p.a;

        String filename = execCtx.symbolTable.get("filename").toString();

        try {
            draw(new Img(pos.x, pos.y, filename));
        } catch (IOException e) {
            return res.failure(new RTError(
                    pos_start, pos_end,
                    "Encountered IOException " + e.toString(),
                    execCtx
            ));
        }
        return res.success(new Null());
    }

    public RTResult execute_setIcon(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        String filename = execCtx.symbolTable.get("filename").toString();

        try {
            Image img = ImageIO.read(new File(filename));
            frame.setIconImage(img);
        } catch (IOException e) {
            return res.failure(new RTError(
                    pos_start, pos_end,
                    "Encountered IOException " + e.toString(),
                    execCtx
            ));
        }
        return res.success(new Null());
    }

    public RTResult execute_playSound(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        String filename = execCtx.symbolTable.get("filename").toString();

        try {
            File soundFile = new File(filename);

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat audioFormat = audioInputStream.getFormat();

            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

            new PlayThread(audioFormat, sourceDataLine, audioInputStream).start();
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            return res.failure(new RTError(
                    pos_start, pos_end,
                    "Encountered IOException " + e.toString(),
                    execCtx
            ));
        }
        return res.success(new Null());
    }

    public RTResult execute_start(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        frame.add(canvas);
        frame.pack();
        frame.setVisible(true);

        return res.success(new Null());
    }

    public RTResult execute_refresh(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        refresh();

        return res.success(new Null());
    }

    public RTResult execute_qUpdate(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        if (changed)
            canvas.push(slices, pixels);

        return res.success(new Null());
    }

    public RTResult execute_refreshLoop(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        if (refreshLoop != null) refreshLoop.stop();

        ActionListener taskPerformer = e -> refresh();
        Timer timer = new Timer(10, taskPerformer);
        timer.start();

        refreshLoop = timer;

        return res.success(new Null());
    }

    public RTResult execute_refreshUnloop(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        if (refreshLoop != null) {
            refreshLoop.stop();
            refreshLoop = null;
        }

        return res.success(new Null());
    }

    public RTResult execute_clear(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        flush();

        return res.success(new Null());
    }

    public RTResult execute_fps(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        double time = (System.currentTimeMillis() - start) / 1000;

        return res.success(new Num(frames / time));
    }

    public RTResult execute_toggleQRender(Context execCtx) {
        queue = !queue;

        return new RTResult().success(new Null());
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_keyDown(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        String key = execCtx.symbolTable.get("key").toString().toLowerCase();

        if (!keys.containsKey(key)) return res.failure(new RTError(
                pos_start, pos_end,
                "Invalid key",
                execCtx
        ));

        return res.success(new Bool(keypressed.get(keys.get(key))));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_keyTyped(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        String key = execCtx.symbolTable.get("key").toString().toLowerCase();

        if (!keys.containsKey(key)) return res.failure(new RTError(
                pos_start, pos_end,
                "Invalid key",
                execCtx
        ));

        boolean typed = keytyped.get(keys.get(key));
        keytyped.replace(keys.get(key), false);
        return res.success(new Bool(typed));
    }

    public RTResult execute_keyString(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        StringBuilder keystring = new StringBuilder();
        for (Integer key : keytyped.keySet()) {
            if (keytyped.get(key) && keycode.containsKey(key) && keycode.get(key).length() == 1)
                keystring.append(keycode.get(key));
            keytyped.replace(key, false);
        }
        return res.success(new Str(keystring.toString()));
    }

    public RTResult execute_mouseDown(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj i = res.register(checkPosInt(execCtx.symbolTable.get("button")));
        if (res.error != null) return res;
        int index = (int)((Num) i).trueValue();

        if (index > 2) return res.failure(new RTError(
                i.get_start(), i.get_end(),
                "Expected number where 0 <= n <= 2",
                execCtx
        ));

        return res.success(new Bool(mouseButtons[index]));
    }

    public RTResult execute_mousePos(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Point canvPos = canvas.getMousePosition();
        mousePos = canvPos != null ? canvPos : new Point(-1, -1);
        return res.success(
                new PList(Arrays.asList(
                        new Num(mousePos.x),
                        new Num(mousePos.y)
                ))
        );
    }

    public RTResult execute_mouseIn(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        return res.success(new Bool(canvas.getMousePosition() != null));
    }

    public RTResult execute_screenshot(Context execCtx) {
        RTResult res = new RTResult();

        res.register(isInit());
        if (res.error != null) return res;

        Obj fn = (Obj) execCtx.symbolTable.get("filename");
        String filename = fn.toString();

        BufferedImage img = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
        canvas.paint(img.createGraphics());
        File imageFile = new File("." + File.separator + filename);

        boolean fileCreated;
        try {
            fileCreated = imageFile.createNewFile();
            ImageIO.write(img, "jpeg", imageFile);
        } catch (IOException e) {
            return res.failure(new RTError(
                    fn.get_start(), fn.get_end(),
                    "Encountered IOException " + e.toString(),
                    execCtx
            ));
        }

        return res.success(new Bool(fileCreated));
    }

    static void refresh() {
        if (changed) {
            canvas.repaint();
            changed = false;
        }
        frames++;
    }

    static void draw(DrawSlice o) {
        changed = true;
        if (queue)
            slices.add(o);
        else
            canvas.draw(o);
    }

    static void setPixel(Point p, Color color) {
        changed = true;
        if (queue) {
            Rect r = new Rect(p.x, p.y, 1, 1, color);

            if (pixels.containsKey(p))
                pixels.replace(p, r);
            else
                pixels.put(p, r);
        } else
            canvas.setPixel(p, color);
    }

    static void flush() {
        changed = true;
        if (queue) {
            slices = new HashSet<>();
            pixels = new ConcurrentHashMap<>();
        } else canvas.flush();
    }
}
