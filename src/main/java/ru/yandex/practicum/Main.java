package ru.yandex.practicum;

import ru.yandex.practicum.quick_hull.QuickHull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: Main <solution> <input> <expected>");
            System.err.println("  solutions: quick_hull");
            System.exit(2);
        }
        String name = args[0];
        String inputPath = args[1];
        String expectedPath = args[2];

        Solution sol;
        switch (name) {
            case "quick_hull" -> sol = new QuickHull();
            default -> {
                System.err.println("unknown solution: " + name);
                System.exit(2);
                return;
            }
        }

        String input = Files.readString(Paths.get(inputPath), StandardCharsets.UTF_8);
        String expected = Files.readString(Paths.get(expectedPath), StandardCharsets.UTF_8);

        sol.readInput(input);
        sol.solve();
        boolean ok = sol.compareExpected(expected);

        System.out.println((ok ? "OK" : "FAIL") + " " + name);
        System.exit(ok ? 0 : 1);
    }
}
