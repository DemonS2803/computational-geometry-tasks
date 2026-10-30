package ru.yandex.practicum.taskE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.StringTokenizer;

public class TaskE {

    public static Point previous;
    public static Point first;
    public static Point p;


    static class Point {
        long x, y;

        public Point(String x, String y) {
            this.x = Long.parseLong(x);
            this.y = Long.parseLong(y);
        }
    }

    static BigInteger cross(Point a, Point b, Point p) {
        BigInteger ax = BigInteger.valueOf(a.x);
        BigInteger ay = BigInteger.valueOf(a.y);

        BigInteger dx = BigInteger.valueOf(b.x).subtract(ax);
        BigInteger dy = BigInteger.valueOf(b.y).subtract(ay);
        BigInteger px = BigInteger.valueOf(p.x).subtract(ax);
        BigInteger py = BigInteger.valueOf(p.y).subtract(ay);

        return dx.multiply(py).subtract(dy.multiply(px));
    }

    static boolean inside = false;
    static boolean boundary = false;

    static void checkEdge(Point a, Point b, Point p) {
        int sign = cross(a, b, p).signum();

        if (sign == 0
                && Math.min(a.x, b.x) <= p.x
                && p.x <= Math.max(a.x, b.x)
                && Math.min(a.y, b.y) <= p.y
                && p.y <= Math.max(a.y, b.y)) {
            boundary = true;
        }

        if ((a.y > p.y) != (b.y > p.y)) {
            if ((b.y > a.y && sign > 0)
                    || (b.y < a.y && sign < 0)) {
                inside = !inside;
            }
        }
    }

    public static void main(String[] args) throws IOException {
            readInput();

            checkEdge(previous, first, p);

            System.out.println(boundary || inside ? "YES" : "NO");
    }

    public static void readInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            int n = Integer.parseInt(st.nextToken());
            p = new Point(st.nextToken(), st.nextToken());
            st = new StringTokenizer(br.readLine());
            first = new Point(st.nextToken(), st.nextToken());
            previous = first;

            for (int i = 1; i < n; i++) {
                st = new StringTokenizer(br.readLine());
                Point current = new Point(st.nextToken(), st.nextToken());
                checkEdge(previous, current, p);
                previous = current;
            }
        }
    }

}
