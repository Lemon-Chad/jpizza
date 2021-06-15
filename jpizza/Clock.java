package lemon.jpizza;

public class Clock {
    long time = 0;
    public double tick() {
        long time = System.nanoTime();
        double diff = (time - this.time) / 1e6;
        this.time = time;
        return diff;
    }
}
