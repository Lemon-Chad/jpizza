package lemon.jpizza;

public record Pair3<T, U, V>(T first, U second, V third) {
    public String toString() {
        return String.format("%s, %s, %s", first, second, third);
    }

}
