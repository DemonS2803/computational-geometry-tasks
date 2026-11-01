package ru.yandex.practicum.taskF;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;


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
        // sort by x, then by y
        Arrays.sort(points);

        List<Point> unique = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i == 0 || points[i].compareTo(points[i - 1]) != 0) {
                unique.add(points[i]);
            }
        }

        int m = unique.size();
        hull = new ArrayList<>();

        if (m == 1) {
            hull.add(unique.get(0));
            System.out.println(1);
            System.out.println(hull.get(0).x + " " + hull.get(0).y);
            System.out.println("0.0");
            return;
        }

        List<Point> lower = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            // curr is inside. thx to ccw
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), unique.get(i)) <= 0) {
                // delete prev, which is on line
                lower.remove(lower.size() - 1);
            }
            lower.add(unique.get(i));
        }

        List<Point> upper = new ArrayList<>();
        for (int i = m - 1; i >= 0; i--) {
            // curr is inside. thx to ccw
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), unique.get(i)) <= 0) {
                // save on line
                upper.remove(upper.size() - 1);
            }
            upper.add(unique.get(i));
        }

        for (int i = 0; i < lower.size() - 1; i++) {
            hull.add(lower.get(i));
        }
        for (int i = 0; i < upper.size() - 1; i++) {
            hull.add(upper.get(i));
        }

//        Collections.reverse(hull);
        int sz = hull.size();
        for (int i = 1; i <= sz / 2; i++) {
            Point tmp = hull.get(sz - i);
            hull.set(sz - i, hull.get(i));
            hull.set(i, tmp);
        }

        BigDecimal area2 = BigDecimal.ZERO;
        for (int i = 0; i < hull.size(); i++) {
            Point a = hull.get(i);
            Point b = hull.get((i + 1) % hull.size());
            area2 = area2
                    .add(BigDecimal.valueOf(a.x).multiply(BigDecimal.valueOf(b.y)))
                    .subtract(BigDecimal.valueOf(a.y).multiply(BigDecimal.valueOf(b.x)));
        }
        area2 = area2.abs();

        StringBuilder sb = new StringBuilder();
        sb.append(hull.size()).append('\n');
        for (int i = 0; i < hull.size(); i++) {
            sb.append(hull.get(i).x).append(' ').append(hull.get(i).y).append('\n');
        }


        BigDecimal[] qr = area2.divideAndRemainder(BigDecimal.valueOf(2.0));
        BigDecimal bd = qr[0].add(qr[1]);
        sb.append(bd);


        System.out.println(sb);
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
