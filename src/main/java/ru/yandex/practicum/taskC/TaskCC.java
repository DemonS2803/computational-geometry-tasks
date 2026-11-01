package ru.yandex.practicum.taskC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;

public class TaskCC {

    private static class Point {
        final long x;
        final long y;

        Point(String x, String y) {
            this.x = Long.parseLong(x);
            this.y = Long.parseLong(y);
        }
    }

    private static final Comparator<Point> BY_X =
            Comparator.comparingLong((Point p) -> p.x)
                    .thenComparingLong(p -> p.y);

    private static final Comparator<Point> BY_Y =
            Comparator.comparingLong((Point p) -> p.y)
                    .thenComparingLong(p -> p.x);

    private static Point[] points;
    private static Point[] mergeBuffer;
    private static Point[] strip;

    private static Point first;
    private static Point second;
    private static long bestDistanceSquared = Long.MAX_VALUE;

    public static void readInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            int n = Integer.parseInt(br.readLine());

            points = new Point[n];
            mergeBuffer = new Point[n];
            strip = new Point[n];

            for (int i = 0; i < n; i++) {
                StringTokenizer st = new StringTokenizer(br.readLine());
                points[i] = new Point(st.nextToken(), st.nextToken());
            }
        }
    }

    public static void solve() {
        Arrays.sort(points, BY_X);
        findClosest(0, points.length);
    }

    public static void findClosest(int left, int right) {
        if (right - left <= 3) {
            for (int i = left; i < right; i++) {
                for (int j = i + 1; j < right; j++) {
                    updateAnswer(points[i], points[j]);
                }
            }

            Arrays.sort(points, left, right, BY_Y);
            return;
        }

        int middle = (left + right) >>> 1;
        long middleX = points[middle].x;

        findClosest(left, middle);
        findClosest(middle, right);

        mergeByY(left, middle, right);

        int stripSize = 0;
        for (int i = left; i < right; i++) {
            long dx = points[i].x - middleX;
            if (dx * dx < bestDistanceSquared) {
                strip[stripSize++] = points[i];
            }
        }

        for (int i = 0; i < stripSize; i++) {
            for (int j = i + 1; j < stripSize; j++) {
                long dy = strip[j].y - strip[i].y;
                if (dy * dy >= bestDistanceSquared) {
                    break;
                }
                updateAnswer(strip[i], strip[j]);
            }
        }
    }

    public static void mergeByY(int left, int middle, int right) {
        int i = left;
        int j = middle;
        int k = left;

        while (i < middle && j < right) {
            if (BY_Y.compare(points[i], points[j]) <= 0) {
                mergeBuffer[k++] = points[i++];
            } else {
                mergeBuffer[k++] = points[j++];
            }
        }

        while (i < middle) {
            mergeBuffer[k++] = points[i++];
        }

        while (j < right) {
            mergeBuffer[k++] = points[j++];
        }

        System.arraycopy(mergeBuffer, left, points, left, right - left);
    }

    private static void updateAnswer(Point a, Point b) {
        long dx = a.x - b.x;
        long dy = a.y - b.y;
        long distanceSquared = dx * dx + dy * dy;

        if (distanceSquared < bestDistanceSquared) {
            bestDistanceSquared = distanceSquared;
            first = a;
            second = b;
        }
    }

    public static void printResult() {
        StringBuilder result = new StringBuilder();
        result.append(first.x).append(' ').append(first.y).append('\n');
        result.append(second.x).append(' ').append(second.y).append('\n');
        System.out.print(result);
    }

    public static void main(String[] args) throws IOException {
        readInput();
        solve();
        printResult();
    }
}
