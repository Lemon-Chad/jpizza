package lemon.jpizza;

import lemon.jpizza.objects.Obj;

// Cool!
public class Cache {
    public final String funcName;
    public final Obj[] args;
    public final Object result;

    public Cache(String funcName, Obj[] args, Object result) {
        this.funcName = funcName;
        this.args = args;
        this.result = result;
    }
}
