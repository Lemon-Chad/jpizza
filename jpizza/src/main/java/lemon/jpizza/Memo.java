package lemon.jpizza;

import lemon.jpizza.objects.Obj;

import java.util.ArrayList;
import java.util.Arrays;

public class Memo {
    final ArrayList<Cache> caches = new ArrayList<>();
    public Object get(String name, Obj[] args) {
        int size = caches.size();
        for (int i = 0; i < size; i++) {
            Cache element = caches.get(i);
            if (element.funcName.equals(name)) {
                boolean eq = true;
                if (args.length != element.args.length) continue;
                int length = args.length;
                for (int j = 0; j < length; j++) {
                    if (args[j].equals(element.args[j])) {
                        eq = false;
                        break;
                    }
                }
                if (eq)
                    return element;
            }
        } return null;
    }
    public void add(Cache cache) { caches.add(cache); }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Cache cache : caches)
            sb.append(String.format("%s: %s: %s", cache.funcName, Arrays.toString(cache.args), cache.result))
                    .append("\n");
        return sb.toString();
    }
}
