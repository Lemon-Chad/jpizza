package lemon.jpizza.Libraries;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Shell;

import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings("unused")
public class DemoLibrary extends Library {

    public DemoLibrary(String name) { super(name); }

    /*
    To initialize a library, you have to override the initialization method
    The code for this library would be:

                                       Reference to the class
                                              |
                      Name of import          |
                            vvv               ↓
    DemoLibrary.initialize("demo", DemoLibrary.class, new HashMap<>(){{

        put("printDemo", Collections.singletonList("value"));
             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        // This should be the name of the function and a list of the argument names

    }});

    Afterwards you must add YourLibraryName.initialize(); to Shell.java inside of initLibs()
    In this case, it would be:

    DemoLibrary.initialize();

    To use it in code you could then do:

    import demo;
    demo::printDemo("test"); <> Prints "test"

     */

    public static void initialize() {
        initialize("demo", DemoLibrary.class, new HashMap<>(){{
            put("printDemo", Collections.singletonList("value"));
        }});
    }

    public RTResult execute_printDemo(Context execCtx) {
        // Get value arg from variables passed into function
        Obj value = (Obj) execCtx.symbolTable.get("value");
        // Convert value argument to a string type
        value = value.astring();
        // Print value argument
        Shell.logger.outln(value);
        // Return null
        return new RTResult().success(new Null());
    }
}
