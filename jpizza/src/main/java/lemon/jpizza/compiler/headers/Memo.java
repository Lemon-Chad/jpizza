package lemon.jpizza.compiler.headers;

import lemon.jpizza.compiler.values.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Memo {
    List<Cache> caches;
    Stack<Cache> stack;

    public Memo() {
        this.caches = new ArrayList<>();
        this.stack = new Stack<>();
    }

    public void stackCache(String name, Value[] args) {
        Cache c = new Cache(name, args, null);
        caches.add(c);
        stack.add(c);
    }

    public void storeCache(Value result) {
        stack.pop().store(result);
    }

    public Value get(String key, Value[] args) {
        for (Cache cache : caches) {
            if (cache.equals(key, args)) {
                return cache.getValue();
            }
        }
        return null;
    }
}
