package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Executables.ClassInstance;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Shell;
import lemon.jpizza.Token;
import lemon.jpizza.Nodes.Node;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExtendNode extends Node {
    public Token file_name_tok;
    public boolean fluctuating = true;

    public ExtendNode(Token file_name_tok) {
        this.file_name_tok = file_name_tok;

        pos_start = file_name_tok.pos_start.copy(); pos_end = file_name_tok.pos_end.copy();
        jptype = Constants.JPType.Import;
    }



    public RTResult vis(Context context) throws IOException {
        String fn = (String) file_name_tok.value;
        String file_name = System.getProperty("user.dir") + "/" + fn + ".jar";
        String modPath = Shell.root + "/extensions/" + fn;
        String modFilePath = modPath + "/" + fn + ".jar";
        var mkdirs = new File(Shell.root + "/extensions").mkdirs();
        RTResult res = new RTResult();
        String userDataDir = System.getProperty("user.dir");
            if (Files.exists(Paths.get(modFilePath))){
                URL[] urls = new URL[]{new URL("file://" + modFilePath)};
                URLClassLoader urlClassLoader = new URLClassLoader(urls);
                try {
                    Class<?> loadedClass = urlClassLoader.loadClass("jpext." + fn);
                    Constructor<?> constructor = loadedClass.getConstructor();
                    Object loadedObject = constructor.newInstance();
                    loadedClass.getMethod("initialize").invoke(loadedObject);
                } catch(Exception e) {
                    return res.failure(new RTError(pos_start,pos_end, "Error", context));
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
                    return res.failure(new RTError(pos_start,pos_end, "Error", context));
                }
            }else {
                return res.failure(new RTError(
                        pos_start, pos_end,
                        "Extension does not exist",
                        context
                ));}
            if (res.error != null) return res;
        return res.success(new Null());
    }

    public RTResult visit(Interpreter inter, Context context) {
        try {
            return vis(context);
        } catch (IOException e) {
            return new RTResult().failure(new RTError(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        }
    }

}
