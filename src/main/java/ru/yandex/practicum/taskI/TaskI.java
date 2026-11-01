package ru.yandex.practicum.taskI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.StringTokenizer;

public class TaskI {

    static int n;
    static Point[] points;
    static int[] prev;
    static int[] next;
    static boolean[] alive;
    static boolean[] isInConvex;
    static boolean[] inQueue;
    static int[] count;
    static ArrayDeque<Integer> ears;


    static class Point {
        long x, y;

        Point(long x, long y) {
            this.x = x;
            this.y = y;
        }
    }

    static long cross(Point a, Point b, Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    static boolean isInsideTriangle(Point p, Point a, Point b, Point c) {
        return cross(a, b, p) >= 0
                && cross(b, c, p) >= 0
                && cross(c, a, p) >= 0;
    }

    public static void main(String[] args) {
        readInput();
        solve();
    }

    public static void solve() {
        ears = new ArrayDeque<>();
        prev = new int[n];
        next = new int[n];
        alive = new boolean[n];
        isInConvex = new boolean[n];
        inQueue = new boolean[n];
        count = new int[n];

        Arrays.fill(alive, true);

        long areaSquare = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            areaSquare += points[i].x * points[j].y - points[i].y * points[j].x;
        }

        for (int i = 0; i < n; i++) {
            if (areaSquare > 0) {
                prev[i] = (i + n - 1) % n;
                next[i] = (i + 1) % n;
            } else {
                prev[i] = (i + 1) % n;
                next[i] = (i + n - 1) % n;
            }
        }

        for (int v = 0; v < n; v++) {
            recalculate(v);
        }

        int remaining = n;

        while (remaining > 3) {
            int v = -1;

            while (!ears.isEmpty()) {
                int u = ears.poll();
                inQueue[u] = false;

                if (alive[u] && isInConvex[u] && count[u] == 0) {
                    v = u;
                    break;
                }
            }

            int a = prev[v];
            int b = next[v];

            System.out.println(a + " " + b);

            for (int u = 0; u < n; u++) {
                if (!alive[u] || u == v || u == a || u == b || !isInConvex[u]) {
                    continue;
                }

                if (isInsideTriangle(points[v], points[prev[u]], points[u], points[next[u]])) {
                    count[u]--;
                    addToQueue(u);
                }
            }

            alive[v] = false;
            next[a] = b;
            prev[b] = a;
            remaining--;

            recalculate(a);
            recalculate(b);
        }
    }

    static void addToQueue(int v) {
        if (alive[v] && isInConvex[v] && count[v] == 0 && !inQueue[v]) {
            ears.add(v);
            inQueue[v] = true;
        }
    }

    static void recalculate(int v) {
        int a = prev[v];
        int b = next[v];

        isInConvex[v] = cross(points[a], points[v], points[b]) > 0;
        count[v] = 0;

        if (isInConvex[v]) {
            for (int u = 0; u < n; u++) {
                if (!alive[u] || u == a || u == v || u == b) {
                    continue;
                }

                if (isInsideTriangle(points[u], points[a], points[v], points[b])) {
                    count[v]++;
                }
            }
        }

        addToQueue(v);
    }

    static void readInput() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            String line = br.readLine();
            if (line == null) return;
            n = Integer.parseInt(line.trim());

            points = new Point[n];
            for (int i = 0; i < n; i++) {
                StringTokenizer st = new StringTokenizer(br.readLine());
                long x = Long.parseLong(st.nextToken());
                long y = Long.parseLong(st.nextToken());
                points[i] = new Point(x, y);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
