package ru.yandex.practicum.taskG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class TaskG {

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
            solve();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long T(Point a, Point b, Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static double distanceSquare(Point a, Point b) {
        long dx = a.x - b.x;
        long dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double distanceFromPointToLineSegment(Point p, Point a, Point b) {
        long dx = b.x - a.x;
        long dy = b.y - a.y;
        double hypoSquare = dx * dx + dy * dy;

        if (hypoSquare == 0) {
            return distanceSquare(p, a);
        }

        double t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / hypoSquare;
        t = Math.max(0, Math.min(1, t));

        double px = a.x + t * dx;
        double py = a.y + t * dy;

        return Math.sqrt((p.x - px) * (p.x - px) + (p.y - py) * (p.y - py));
    }

    public static void solve() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // Read first polygon
        int n = Integer.parseInt(br.readLine().trim());
        Point[] poly1 = new Point[n];
        for (int i = 0; i < n; i++) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            poly1[i] = new Point(st.nextToken(), st.nextToken());
        }

        // Read second polygon
        int m = Integer.parseInt(br.readLine().trim());
        Point[] poly2 = new Point[m];
        for (int i = 0; i < m; i++) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            poly2[i] = new Point(st.nextToken(), st.nextToken());
        }

        // Find minimum distance between two convex polygons using rotating calipers
        double minDist = Double.POSITIVE_INFINITY;

        // For each edge of poly1, find the closest vertex in poly2
        int j = 0;
        for (int i = 0; i < n; i++) {
            Point a = poly1[i];
            Point b = poly1[(i + 1) % n];

            // Find vertex in poly2 that maximizes cross(a, b, P)
            // (farthest vertex from the line ab in the direction pointing away from poly1)
            while (T(a, b, poly2[(j + 1) % m]) >= T(a, b, poly2[j])) {
                j = (j + 1) % m;
            }

            // The closest point on poly2 to edge ab is poly2[j-1] or poly2[j]
            int jClosest = (j - 1 + m) % m;

            // Compute distances from poly2[j] and poly2[jClosest] to edge ab
            minDist = Math.min(minDist, distanceFromPointToLineSegment(poly2[j], a, b));
            minDist = Math.min(minDist, distanceFromPointToLineSegment(poly2[jClosest], a, b));
        }

        // Similarly for edges of poly2, find closest vertex in poly1
        int i2 = 0;
        for (int j2 = 0; j2 < m; j2++) {
            Point a = poly2[j2];
            Point b = poly2[(j2 + 1) % m];

            // Find vertex in poly1 that maximizes cross(a, b, P)
            while (T(a, b, poly1[(i2 + 1) % n]) >= T(a, b, poly1[i2])) {
                i2 = (i2 + 1) % n;
            }

            // The closest point on poly1 to edge ab is poly1[i2-1] or poly1[i2]
            int iClosest = (i2 - 1 + n) % n;

            // Compute distances from poly1[i2] and poly1[iClosest] to edge ab
            minDist = Math.min(minDist, distanceFromPointToLineSegment(poly1[i2], a, b));
            minDist = Math.min(minDist, distanceFromPointToLineSegment(poly1[iClosest], a, b));
        }

        System.out.println(minDist);
    }
}
