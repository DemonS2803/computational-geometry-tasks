package ru.yandex.practicum.taskC;

import java.io.*;
import java.util.*;

public class TaskC {

    public static int n;
    public static Point[] points;
    public static Pair result;

    static class Point {
        long x, y;
        Point(long x, long y) { this.x = x; this.y = y; }
    }

    static class Pair {
        Point p1, p2;
        long d2;
        Pair(Point p1, Point p2, long d2) {
            this.p1 = p1;
            this.p2 = p2;
            this.d2 = d2;
        }
    }

    public static void main(String[] args) throws IOException {
        readInput();
        solve();
        printResult();
    }

    static long distSquare(Point a, Point b) {
        long dx = a.x - b.x, dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    public static void solve() {
        Point[] byX = points.clone();
        Arrays.sort(byX, (a, b) -> {
            if (a.x != b.x) return Long.compare(a.x, b.x);
            return Long.compare(a.y, b.y);
        });

        Point[] byY = points.clone();
        Arrays.sort(byY, Comparator.comparingLong(a -> a.y));

        result = closest(points, byY, 0, n - 1);
    }

    static Pair closest(Point[] byX, Point[] byY, int start, int end) {
        int n = end - start + 1;
        if (n <= 3) {
            long minDistanceSquare = Long.MAX_VALUE;
            Point p1 = null;
            Point p2 = null;
            // stupid brute force (maybe TL?)
            for (int i = start; i <= end; i++)
                for (int j = i + 1; j <= end; j++) {
                    long d2 = distSquare(byX[i], byX[j]);
                    if (d2 < minDistanceSquare) {
                        minDistanceSquare = d2;
                        p1 = byX[i];
                        p2 = byX[j];
                    }
                }
            return new Pair(p1, p2, minDistanceSquare);
        }

        int mid = (start + end) / 2;
        Point midPt = byX[mid];

        Pair leftBest = closest(byX, byY, start, mid);
        Pair rightBest = closest(byX, byY, mid + 1, end);

        Pair best = leftBest.d2 <= rightBest.d2 ? leftBest : rightBest;


        Point[] byYcopy = Arrays.copyOfRange(byX, start, end + 1);
        Arrays.sort(byYcopy, Comparator.comparingLong(p -> p.y));

        Point[] strip = new Point[n];
        int k = 0;
        for (Point p : byYcopy) {
            long dx = p.x - midPt.x;
            if (dx * dx < best.d2) strip[k++] = p;
        }

        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j < k; j++) {
                long dy = strip[j].y - strip[i].y;
                if (dy * dy >= best.d2) break;
                long d2 = distSquare(strip[i], strip[j]);
                if (d2 < best.d2) best = new Pair(strip[i], strip[j], d2);
            }
        }

        return best;
    }

    public static void readInput() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        n = Integer.parseInt(br.readLine().trim());
        points = new Point[n];
        StringTokenizer st;
        for (int i = 0; i < n; i++) {
            st = new StringTokenizer(br.readLine().trim());
            points[i] = new Point(
                    Long.parseLong(st.nextToken()),
                    Long.parseLong(st.nextToken())
            );
        }
    }

    public static void printResult() {
        System.out.println(result.p1.x + " " + result.p1.y);
        System.out.println(result.p2.x + " " + result.p2.y);
    }
}
