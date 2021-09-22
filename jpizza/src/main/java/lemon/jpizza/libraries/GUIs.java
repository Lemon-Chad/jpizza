package lemon.jpizza.libraries;

import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.objects.primitives.*;

import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings("unused")
public class GUIs extends Library {

    public GUIs(String name) { super(name, "GUIs"); }

    public static void initialize() {
        initialize("GUIs", GUIs.class, new HashMap<>(){{
            put("GUI", Collections.singletonList("value"));
        }});
    }

    public RTResult execute_GUI(Context execCtx) {
        // Get value arg from variables passed into function
        Obj value = (Obj) execCtx.symbolTable.get("value");
        // Convert value argument to a string type
        value = value.astring();
        // Print value argument
        System.out.println(value);
        // Return null
        return new RTResult().success(new Null());
    }
}
