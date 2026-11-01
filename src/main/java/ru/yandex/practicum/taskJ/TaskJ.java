package ru.yandex.practicum.taskJ;

import java.io.*;
import java.util.*;

public class TaskJ {

    static class Point {
        long x, y;

        public Point(long x, long y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Line {
        long a, b, c;
        int dx, dy;
        ArrayList<Integer> points = new ArrayList<>();

        Line(Point p1, Point p2) {
            long x1 = p1.x, y1 = p1.y;
            long x2 = p2.x, y2 = p2.y;
            a = y1 - y2;
            b = x2 - x1;
            c = x1 * y2 - x2 * y1;

            dx = (int) (x2 - x1);
            dy = (int) (y2 - y1);

            if (dx < 0 || (dx == 0 && dy < 0)) {
                dx = -dx;
                dy = -dy;
            }
        }
    }

    static class Vertex {
        final long x, y, z;
        final ArrayList<Integer> out = new ArrayList<>();

        Vertex(long x, long y, long z) {
            if (z < 0) {
                x = -x;
                y = -y;
                z = -z;
            }

            long g = greaterCommonDelimiter(greaterCommonDelimiter(Math.abs(x), Math.abs(y)), z);

            this.x = x / g;
            this.y = y / g;
            this.z = z / g;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Vertex)) {
                return false;
            }

            Vertex other = (Vertex) obj;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            int result = Long.hashCode(x);
            result = 31 * result + Long.hashCode(y);
            result = 31 * result + Long.hashCode(z);
            return result;
        }
    }

    static class HalfEdge {
        final int from, to;
        final int dx, dy;

        HalfEdge(int from, int to, int dx, int dy) {
            this.from = from;
            this.to = to;
            this.dx = dx;
            this.dy = dy;
        }
    }

    static long greaterCommonDelimiter(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    static int half(int dx, int dy) {
        return (dy > 0 || (dy == 0 && dx > 0)) ? 0 : 1;
    }

    static int compareDirections(HalfEdge a, HalfEdge b) {
        int halfA = half(a.dx, a.dy);
        int halfB = half(b.dx, b.dy);

        if (halfA != halfB) {
            return Integer.compare(halfA, halfB);
        }

        long cross = (long) a.dx * b.dy - (long) a.dy * b.dx;

        if (cross > 0) {
            return -1;
        }
        if (cross < 0) {
            return 1;
        }
        return 0;
    }

    static void addEdge(
            int u,
            int v,
            int dx,
            int dy,
            ArrayList<Vertex> vertices,
            ArrayList<HalfEdge> edges
    ) {
        int id = edges.size();

        edges.add(new HalfEdge(u, v, dx, dy));
        edges.add(new HalfEdge(v, u, -dx, -dy));

        vertices.get(u).out.add(id);
        vertices.get(v).out.add(id + 1);
    }

    static double deltaX(Vertex p, Vertex origin) {
        long numerator = p.x * origin.z - origin.x * p.z;
        long denominator = p.z * origin.z;
        return (double) numerator / denominator;
    }

    static double deltaY(Vertex p, Vertex origin) {
        long numerator = p.y * origin.z - origin.y * p.z;
        long denominator = p.z * origin.z;
        return (double) numerator / denominator;
    }

    public static void main(String[] args) throws Exception {
        FastScanner fs = new FastScanner(System.in);

        int n = fs.nextInt();
        Line[] lines = new Line[n];

        for (int i = 0; i < n; i++) {
            int x1 = fs.nextInt();
            int y1 = fs.nextInt();
            int x2 = fs.nextInt();
            int y2 = fs.nextInt();

            lines[i] = new Line(new Point(x1, y1), new Point(x2, y2));
        }

        ArrayList<Vertex> vertices = new ArrayList<>();
        HashMap<Vertex, Integer> vertexIds = new HashMap<>();

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Line p = lines[i];
                Line q = lines[j];

                long z = p.a * q.b - q.a * p.b;

                if (z == 0) {
                    continue;
                }

                long x = p.b * q.c - q.b * p.c;
                long y = p.c * q.a - q.c * p.a;

                Vertex point = new Vertex(x, y, z);
                Integer id = vertexIds.get(point);

                if (id == null) {
                    id = vertices.size();
                    vertices.add(point);
                    vertexIds.put(point, id);
                }

                p.points.add(id);
                q.points.add(id);
            }
        }

        ArrayList<HalfEdge> edges = new ArrayList<>();

        for (Line line : lines) {
            final boolean vertical = line.dx == 0;

            line.points.sort((id1, id2) -> {
                Vertex p = vertices.get(id1);
                Vertex q = vertices.get(id2);

                if (vertical) {
                    return Long.compare(p.y * q.z, q.y * p.z);
                }
                return Long.compare(p.x * q.z, q.x * p.z);
            });

            int previous = -1;

            for (int id : line.points) {
                if (id == previous) {
                    continue;
                }

                if (previous != -1) {
                    addEdge(
                            previous,
                            id,
                            line.dx,
                            line.dy,
                            vertices,
                            edges
                    );
                }

                previous = id;
            }
        }

        int edgeCount = edges.size();
        int[] position = new int[edgeCount];

        for (Vertex vertex : vertices) {
            vertex.out.sort(
                    (id1, id2) -> compareDirections(
                            edges.get(id1),
                            edges.get(id2)
                    )
            );

            for (int i = 0; i < vertex.out.size(); i++) {
                position[vertex.out.get(i)] = i;
            }
        }

        int[] next = new int[edgeCount];

        for (int i = 0; i < edgeCount; i++) {
            HalfEdge edge = edges.get(i);
            ArrayList<Integer> out = vertices.get(edge.to).out;

            int reversePosition = position[i ^ 1];
            int nextPosition = (reversePosition - 1 + out.size())
                    % out.size();

            next[i] = out.get(nextPosition);
        }

        boolean[] used = new boolean[edgeCount];
        ArrayList<Double> faceAreas = new ArrayList<>();

        for (int start = 0; start < edgeCount; start++) {
            if (used[start]) {
                continue;
            }

            Vertex origin = vertices.get(edges.get(start).from);

            double twiceArea = 0.0;
            double compensation = 0.0;

            int current = start;

            do {
                used[current] = true;

                HalfEdge edge = edges.get(current);
                Vertex p = vertices.get(edge.from);
                Vertex q = vertices.get(edge.to);

                // Сдвигаем начало координат для уменьшения погрешности.
                double px = deltaX(p, origin);
                double py = deltaY(p, origin);
                double qx = deltaX(q, origin);
                double qy = deltaY(q, origin);

                double term = px * qy - py * qx;

                // Суммирование Кэхэна.
                double value = term - compensation;
                double sum = twiceArea + value;
                compensation = (sum - twiceArea) - value;
                twiceArea = sum;

                current = next[current];
            } while (current != start);

            faceAreas.add(twiceArea / 2.0);
        }

        int outerFace = -1;

        for (int i = 0; i < faceAreas.size(); i++) {
            if (outerFace == -1
                    || faceAreas.get(i) < faceAreas.get(outerFace)) {
                outerFace = i;
            }
        }

        ArrayList<Double> answer = new ArrayList<>();

        for (int i = 0; i < faceAreas.size(); i++) {
            if (i == outerFace) {
                continue;
            }

            double area = faceAreas.get(i);

            if (area >= 1e-8) {
                answer.add(area);
            }
        }

        Collections.sort(answer);

        PrintWriter out = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(System.out))
        );

        out.println(answer.size());

        for (double area : answer) {
            out.printf(Locale.US, "%.10f%n", area);
        }

        out.flush();
    }

    static class FastScanner {
        BufferedReader br;
        StringTokenizer st;

        public FastScanner(InputStream in) {
            br = new BufferedReader(new InputStreamReader(in));
        }

        String next() {
            while (st == null || !st.hasMoreTokens()) {
                try {
                    String line = br.readLine();
                    if (line == null) return null;
                    st = new StringTokenizer(line);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return st.nextToken();
        }

        int nextInt() {
            return Integer.parseInt(next());
        }
    }
}
