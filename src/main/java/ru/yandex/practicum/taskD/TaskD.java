package ru.yandex.practicum.taskD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class TaskD {
    private static Point[] points;
    private static int first;
    private static int last;

    static class Point {
        long x, y;

        public Point(String x, String y) {
            this.x = Long.parseLong(x);
            this.y = Long.parseLong(y);
        }
    }

    private static long T(Point a, Point b, Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static boolean onSegment(Point a, Point b, Point p) {
        return T(a, b, p) == 0
                && p.x >= Math.min(a.x, b.x)
                && p.x <= Math.max(a.x, b.x)
                && p.y >= Math.min(a.y, b.y)
                && p.y <= Math.max(a.y, b.y);
    }

    private static boolean isDirectHit(Point p) {
        long firstCross = T(points[0], points[first], p);
        long lastCross = T(points[0], points[last], p);

        if (firstCross < 0 || lastCross > 0) {
            return false;
        }

        if (firstCross == 0) {
            return onSegment(points[0], points[first], p);
        }

        if (lastCross == 0) {
            return onSegment(points[0], points[last], p);
        }

        int left = first;
        int right = last;

        while (right - left > 1) {
            int mid = left + (right - left) / 2;

            if (T(points[0], points[mid], p) >= 0) {
                left = mid;
            } else {
                right = mid;
            }
        }

        return T(points[left], points[right], p) >= 0;
    }

    public static void main(String[] args) throws IOException {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            int n = Integer.parseInt(st.nextToken());
            int m = Integer.parseInt(st.nextToken());

            int k = Integer.parseInt(st.nextToken());
            if (k == 0) {
                System.out.println("YES");
                return;
            }

            points = new Point[n];
            first = 1;
            last = n - 1;

            for (int i = 0; i < n; i++) {
                st = new StringTokenizer(br.readLine());
                points[i] = new Point(st.nextToken(), st.nextToken());
            }

            int hits = 0;
            for (int i = 0; i < m; i++) {
                st = new StringTokenizer(br.readLine());
                Point p = new Point(st.nextToken(), st.nextToken());

                if (isDirectHit(p)) {
                    hits++;

                    if (hits >= k) {
                        System.out.println("YES");
                        return;
                    }
                }
            }

            System.out.println("NO");
        }
    }
}
