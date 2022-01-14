package lemon.jpizza;

import lemon.jpizza.errors.Error;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class RunMain {
    public static void main(String[] args) throws IOException {
        Shell.main(new String[]{ "main.devp", "-rf", "-r" });
    }
}
