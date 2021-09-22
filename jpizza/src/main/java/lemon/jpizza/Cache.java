package lemon.jpizza;

import lemon.jpizza.objects.Obj;

public class Cache {
    public String funcName;
    public Obj[] args;
    public Object result;

    public Cache(String funcName, Obj[] args, Object result) {
        this.funcName = funcName;
        this.args = args;
        this.result = result;
    }

}
