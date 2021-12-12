package lemon.jpizza.compiler;

public class JStack<T> {
    public int count;
    T[] stack;
    int top;
    int size;

    static int MAX = 5000;
    static double GROW_RATE = 1.5;

    public JStack(int size) {
        this.size = size;
        stack = (T[]) new Object[size];
        top = 0;
    }

    public void push(T t) {
        stack[top++] = t;
        count++;
        if (size - top < 32) {
            int newSize = (int) (size * GROW_RATE);
            T[] newStack = (T[]) new Object[newSize];
            System.arraycopy(stack, 0, newStack, 0, size);
            stack = newStack;
            size = newSize;
        }
        if (count > MAX) {
            throw new RuntimeException("Stack overflow");
        }
    }

    public T pop() {
        count--;
        return stack[--top];
    }

    public T peek() {
        return peek(0);
    }

    public T peek(int offset) {
        return stack[top - offset - 1];
    }

    public T get(int index) {
        return stack[index];
    }

    public void set(int index, T t) {
        stack[index] = t;
    }

    public void setTop(int top) {
        this.top = top;
        count = top;
    }

    public T[] asArray() {
        return stack;
    }

    public void clear() {
        top = 0;
        count = 0;
        for (int i = 0; i < size; i++) {
            stack[i] = null;
        }
    }
}
