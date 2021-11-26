package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Shell;
import lemon.jpizza.Token;
import lemon.jpizza.nodes.Node;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExtendNode extends Node {
    public final Token file_name_tok;

    public ExtendNode(Token file_name_tok) {
        this.file_name_tok = file_name_tok;

        pos_start = file_name_tok.pos_start.copy(); pos_end = file_name_tok.pos_end.copy();
        jptype = Constants.JPType.Import;
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult vis(Context context) throws IOException {
        String fn = (String) file_name_tok.value;
        String file_name = System.getProperty("user.dir") + "/" + fn + ".jar";
        String modPath = Shell.root + "/extensions/" + fn;
        String modFilePath = modPath + "/" + fn + ".jar";

        //noinspection ResultOfMethodCallIgnored
        new File(Shell.root + "/extensions").mkdirs();

        RTResult res = new RTResult();
        if (Files.exists(Paths.get(modFilePath))) {
            URL[] urls = new URL[]{new URL("file://" + modFilePath)};
            URLClassLoader urlClassLoader = new URLClassLoader(urls);
            try {
                Class<?> loadedClass = urlClassLoader.loadClass("jpext." + fn);
                Constructor<?> constructor = loadedClass.getConstructor();
                Object loadedObject = constructor.newInstance();
                loadedClass.getMethod("initialize").invoke(loadedObject);
            } catch(Exception e) {
                return res.failure(RTError.Internal(pos_start,pos_end, e.toString(), context));
            }
        }
        else if (Files.exists(Paths.get(file_name))){
            URL[] urls = new URL[]{new URL("file://" + file_name)};
            URLClassLoader urlClassLoader = new URLClassLoader(urls);
            try {
                Class<?> loadedClass = urlClassLoader.loadClass("jpext." + fn);
                Constructor<?> constructor = loadedClass.getConstructor();
                Object loadedObject = constructor.newInstance();
                loadedClass.getMethod("initialize").invoke(loadedObject);
            } catch(Exception e) {
                return res.failure(RTError.Internal(pos_start,pos_end, e.toString(), context));
            }
        }
        else {
            return res.failure(RTError.FileNotFound(
                    pos_start, pos_end,
                    "Extension does not exist",
                    context
            ));
        }
        if (res.error != null) return res;
        return res.success(new Null());
    }

    public RTResult visit(Interpreter inter, Context context) {
        try {
            return vis(context);
        } catch (IOException e) {
            return new RTResult().failure(RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        }
    }

}
