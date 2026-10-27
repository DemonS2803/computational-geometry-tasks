import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Test runner for computational geometry tasks.
 * Reads inputN.txt, runs the corresponding Java task,
 * and compares stdout with expectedN.txt.
 *
 * Usage:
 *   javac -d build src/main/java/TestMain.java
 *   java -cp build TestMain <taskName>
 *
 *   e.g. java -cp build TestMain task_C
 */
public class TestMain {

    private static final String SRC_DIR = "src/main/java/ru/yandex/practicum";
    private static final String TESTS_DIR = "tests";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java TestMain <taskName>");
            System.exit(1);
        }

        String taskName = args[0];
        // className: task_A -> TaskA, task_C -> TaskC
        String className;
        if (taskName.equals("task_C")) {
            className = "TaskC";
        } else {
            String letter = taskName.split("_")[1];
            className = "Task" + letter.toUpperCase();
        }

        Path testsPath = Paths.get(TESTS_DIR, taskName);
        if (!Files.exists(testsPath)) {
            System.err.println("Test directory not found: " + testsPath);
            System.exit(1);
        }

        List<Path> inputs = Files.list(testsPath)
                .filter(p -> p.getFileName().toString().startsWith("input") && p.getFileName().toString().endsWith(".txt"))
                .sorted(Comparator.comparing(p -> Integer.parseInt(
                        p.getFileName().toString().replaceAll("\\D+", ""))))
                .collect(Collectors.toList());

        if (inputs.isEmpty()) {
            System.err.println("No input files found in " + testsPath);
            System.exit(1);
        }

        // Compile the task
        System.out.println("=== Compiling " + className + " ===");
        String srcDirName = taskName.contains("_") ? taskName.replace("_", "") : taskName;
        String srcFile = Paths.get(SRC_DIR, srcDirName, className + ".java").toString();
        int compileResult = compile(srcFile);
        if (compileResult != 0) {
            System.err.println("Compilation failed!");
            System.exit(1);
        }

        int[] counters = new int[2]; // [passed, failed]

        for (Path input : inputs) {
            int num = Integer.parseInt(input.getFileName().toString().replaceAll("\\D+", ""));
            Path expectedFile = testsPath.resolve("expected" + num + ".txt");

            System.out.println("Test " + num + "...");

            String expected = Files.exists(expectedFile)
                    ? Files.readString(expectedFile).strip()
                    : null;

            long start = System.currentTimeMillis();

            String fullPackage = "ru.yandex.practicum." + srcDirName;
            String output = runTask(fullPackage + "." + className, input);
            if (expected != null && !expected.equals(output.strip())) {
                System.out.println("  FAIL — output mismatch:");
                String expPreview = String.join(" | ", expected.lines().limit(3).collect(Collectors.toList()));
                String actPreview = String.join(" | ", output.strip().lines().limit(3).collect(Collectors.toList()));
                System.out.println("  expected: " + expPreview);
                System.out.println("  actual:   " + actPreview);
                counters[1]++;
            } else {
                System.out.println("  OK");
                counters[0]++;
            }

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("  Time: " + elapsed + " ms");
        }

        System.out.println();
        System.out.println("=================================");
        System.out.println("Results: " + counters[0] + " passed, " + counters[1] + " failed");
        System.out.println("=================================");
        if (counters[1] > 0) System.exit(1);
    }

    static int compile(String srcFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("javac", "-d", "build", srcFile);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process p = pb.start();
        return p.waitFor();
    }

    static String runTask(String fullClassName, Path input) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "build", fullClassName);
        pb.redirectInput(ProcessBuilder.Redirect.from(input.toFile()));
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process p = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        p.waitFor();
        return output;
    }
}
