package ru.yandex.practicum.taskF;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;


public class TaskF {

    private static int n;
    private static Point[] points;
    private static List<Point> hull;

    static class Point implements Comparable<Point> {
        long x, y;

        public Point(String x, String y) {
            this.x = Long.parseLong(x);
            this.y = Long.parseLong(y);
        }

        @Override
        public int compareTo(Point o) {
            if (this.x != o.x) {
                return Long.compare(this.x, o.x);
            }
            return Long.compare(this.y, o.y);
        }
    }

    public static void main(String[] args) {
        try {
            readInput();
            solve();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long cross(Point a, Point b, Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    public static void solve() {
        // сортировка по x, затем по y
        Arrays.sort(points);

        Set<Point> pointSet = new HashSet<>();
        List<Point> unique = new ArrayList<>();
        for (int i = 0; i < n; i++) {
//            if (i == 0 || points[i].x != points[i-1].x || points[i].y != points[i-1].y) {
//                unique.add(points[i]);
//            }
            if (!pointSet.contains(points[i])) {
                pointSet.add(points[i]);
                unique.add(points[i]);
            }
        }

        int m = unique.size();
        hull = new ArrayList<>();

//        if (m == 1) {
//            hull.add(unique.get(0));
//            System.out.println(1);
//            System.out.println(hull.get(0).x + " " + hull.get(0).y);
//            System.out.println("0.0");
//            return;
//        }

        // Andrew's monotone chain
        List<Point> lower = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            while (lower.size() >= 2 && cross(lower.get(lower.size()-2), lower.get(lower.size()-1), unique.get(i)) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(unique.get(i));
        }

        List<Point> upper = new ArrayList<>();
        for (int i = m - 1; i >= 0; i--) {
            while (upper.size() >= 2 && cross(upper.get(upper.size()-2), upper.get(upper.size()-1), unique.get(i)) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(unique.get(i));
        }

        // Combine lower[:-1] + upper[:-1]
        for (int i = 0; i < lower.size() - 1; i++) hull.add(lower.get(i));
        for (int i = 0; i < upper.size() - 1; i++) hull.add(upper.get(i));

        // Reverse to clockwise order (keep first point in place)
        int sz = hull.size();
        for (int i = 1; i <= sz / 2; i++) {
            Point tmp = hull.get(sz - i);
            hull.set(sz - i, hull.get(i));
            hull.set(i, tmp);
        }

        // Compute signed area * 2 using shoelace
        long area2 = 0;
        for (int i = 0; i < hull.size(); i++) {
            Point a = hull.get(i);
            Point b = hull.get((i + 1) % hull.size());
            area2 += a.x * b.y - b.x * a.y;
        }
        area2 = Math.abs(area2);

        // Output
        System.out.println(hull.size());
        for (Point p : hull) {
            System.out.println(p.x + " " + p.y);
        }
        System.out.printf("%.1f%n", area2 / 2.0);
    }

    public static void readInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            n = Integer.parseInt(st.nextToken());
            points = new Point[n];

            for (int i = 0; i < n; i++) {
                st = new StringTokenizer(br.readLine());
                Point p = new Point(st.nextToken(), st.nextToken());
                points[i] = p;
            }
        }
    }

}
