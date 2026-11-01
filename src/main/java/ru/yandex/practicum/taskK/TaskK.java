package ru.yandex.practicum.taskK;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.StringTokenizer;

public class TaskK {
    private static long[] points;
    private static String result;

    public static void main(String[] args) throws IOException {
        readInput();
        solve();
        printResult();
    }


    private static void solve() {
        long[] hull = buildConvexHull();
        long maxSquaredDistance = findDiameterSquared(hull);
        result = formatDistance(maxSquaredDistance);
    }

    private static long[] buildConvexHull() {
        Arrays.sort(points);

        int uniqueCount = 0;

        for (int i = 0; i < points.length; i++) {
            if (uniqueCount == 0 || points[i] != points[uniqueCount - 1]) {
                points[uniqueCount++] = points[i];
            }
        }

        if (uniqueCount <= 1) {
            return Arrays.copyOf(points, uniqueCount);
        }

        long[] hull = new long[2 * uniqueCount];
        int size = 0;

        for (int i = 0; i < uniqueCount; i++) {
            while (size >= 2 && Point.cross(hull[size - 2], hull[size - 1], points[i]) <= 0) {
                size--;
            }
            hull[size++] = points[i];
        }

        int lowerSize = size;
        for (int i = uniqueCount - 2; i >= 0; i--) {
            while (size > lowerSize
                    && Point.cross(hull[size - 2], hull[size - 1], points[i]) <= 0) {
                size--;
            }
            hull[size++] = points[i];
        }

        return Arrays.copyOf(hull, size - 1);
    }

    private static long findDiameterSquared(long[] hull) {
        int n = hull.length;

        if (n <= 1) {
            return 0;
        }

        if (n == 2) {
            return Point.squaredDistance(hull[0], hull[1]);
//            return hull[0].squaredDistanceTo(hull[1]);
        }

        long answer = 0;
        int opposite = 1;

        for (int current = 0; current < n; current++) {
            int currentNext = (current + 1) % n;

            while (true) {
                int oppositeNext = (opposite + 1) % n;

                long currentArea = Point.cross(hull[current], hull[currentNext], hull[opposite]);
                long nextArea = Point.cross(hull[current], hull[currentNext], hull[oppositeNext]);
//                long nextArea = cross(hull[current], hull[currentNext], hull[oppositeNext]);

                if (nextArea <= currentArea) {
                    break;
                }

                opposite = oppositeNext;
            }

            answer = Math.max(answer, Point.squaredDistance(hull[current], hull[opposite]));
            answer = Math.max(answer, Point.squaredDistance(hull[currentNext], hull[opposite]));

            int oppositeNext = (opposite + 1) % n;
            if (Point.cross(hull[current], hull[currentNext], hull[opposite]) ==
                    Point.cross(hull[current], hull[currentNext], hull[oppositeNext])) {
                answer = Math.max(answer, Point.squaredDistance(hull[current], hull[oppositeNext]));
                answer = Math.max(answer, Point.squaredDistance(hull[currentNext], hull[oppositeNext]));
            }
        }

        return answer;
    }

    private static String formatDistance(long squaredDistance) {
        int scale = 15;

        BigInteger scaledSquaredDistance = BigInteger.valueOf(squaredDistance)
                .multiply(BigInteger.TEN.pow(2 * scale));

        BigInteger scaledDistance = integerSquareRoot(scaledSquaredDistance);

        return new BigDecimal(scaledDistance, scale).toPlainString();
    }

    private static BigInteger integerSquareRoot(BigInteger value) {
        if (value.signum() == 0) {
            return BigInteger.ZERO;
        }

        BigInteger current = BigInteger.ONE.shiftLeft(
                (value.bitLength() + 1) / 2);

        while (true) {
            BigInteger next = current.add(value.divide(current)).shiftRight(1);

            if (next.compareTo(current) >= 0) {
                return current;
            }

            current = next;
        }
    }

    private static void readInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            int n = Integer.parseInt(br.readLine());
            points = new long[n];

            for (int i = 0; i < n; i++) {
                StringTokenizer st = new StringTokenizer(br.readLine());
                points[i] = Point.pack(st.nextToken(), st.nextToken());
            }
        }

    }
    private static void printResult() {
        System.out.println(result);
    }

    private static final class Point {
        private static final long OFFSET = 1_000_000_000L;
        private static final long MASK = (1L << 31) - 1;

        private Point() {
        }


        static long pack(String  x, String  y) {
            return ((Long.parseLong(x) + OFFSET) << 31) | (Long.parseLong(y) + OFFSET);
        }

        static long x(long point) {
            return (point >>> 31) - OFFSET;
        }

        static long y(long point) {
            return (point & MASK) - OFFSET;
        }

        static long cross(long a, long b, long c) {
            long ax = x(a);
            long ay = y(a);

            long abX = x(b) - ax;
            long abY = y(b) - ay;
            long acX = x(c) - ax;
            long acY = y(c) - ay;

            return abX * acY - abY * acX;
        }

        static long squaredDistance(long a, long b) {
            long dx = x(a) - x(b);
            long dy = y(a) - y(b);

            return dx * dx + dy * dy;
        }
    }

}
