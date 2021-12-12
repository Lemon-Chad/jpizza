package lemon.jpizza.compiler.headers;

import lemon.jpizza.compiler.values.Value;

public class Cache {
    String name;
    Value[] args;
    Value result;
    public Cache(String name, Value[] args, Value result) {
        this.name = name;
        this.args = args;
        this.result = result;
    }
    public boolean equals(Object o) {
        if (o instanceof Cache) {
            Cache c = (Cache) o;
            if (!c.name.equals(name)) return false;
            if (c.args.length != args.length) return false;
            for (int i = 0; i < args.length; i++) {
                if (!c.args[i].equals(args[i])) return false;
            }
            return true;
        }
        return false;
    }

    public boolean equals(String name, Value[] args) {
        if (!this.name.equals(name)) return false;
        if (this.args.length != args.length) return false;
        for (int i = 0; i < args.length; i++) {
            if (!this.args[i].equals(args[i])) return false;
        }
        return true;
    }

    public Value getValue() {
        return result;
    }

    public void store(Value result) {
        this.result = result;
    }
}
