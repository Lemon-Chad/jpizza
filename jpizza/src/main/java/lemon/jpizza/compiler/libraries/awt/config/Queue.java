package lemon.jpizza.compiler.libraries.awt.config;

public class Queue<T> {
    private T head;
    private T tail;

    public Queue(T head) {
        this.head = head;
        this.tail = head;
    }

    public void advance() {
        head = tail;
    }

    public T get() {
        return head;
    }

    public T next() {
        return tail;
    }

    public void add(T t) {
        tail = t;
    }
}
