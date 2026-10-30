package ru.yandex.practicum.quick_hull;

import ru.yandex.practicum.Solution;

import java.io.*;
import java.util.*;

public class QuickHull implements Solution {

    static final double EPS = 1e-12;
    static final double CMP_EPS = 1e-6;

    static class Pt {
        double x, y; int id;
        Pt(double x, double y, int id) { this.x = x; this.y = y; this.id = id; }
    }

    private Pt[] pts;
    private int[] hull;

    // ---------- Solution ----------
    @Override
    public void readInput(String data) {
        try {
            StreamTokenizer st = new StreamTokenizer(new StringReader(data));
            st.nextToken(); int n = (int) st.nval;
            pts = new Pt[n];
            for (int i = 0; i < n; i++) {
                st.nextToken(); double x = st.nval;
                st.nextToken(); double y = st.nval;
                pts[i] = new Pt(x, y, i);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void solve() {
        hull = quickHull(pts);
        normalize(pts, hull);
    }

    @Override
    public boolean compareExpected(String expected) {
        try {
            StreamTokenizer st = new StreamTokenizer(new StringReader(expected));
            st.nextToken(); int n = (int) st.nval;
            Pt[] exp = new Pt[n];
            for (int i = 0; i < n; i++) {
                st.nextToken(); double x = st.nval;
                st.nextToken(); double y = st.nval;
                exp[i] = new Pt(x, y, i);
            }
            if (n != hull.length) {
                System.err.println("compareExpected: size mismatch: expected " + n
                        + ", got " + hull.length);
                return false;
            }
            Pt[] got = new Pt[hull.length];
            for (int i = 0; i < hull.length; i++) got[i] = pts[hull[i]];

            Comparator<Pt> cmp = (a, b) ->
                    a.x != b.x ? Double.compare(a.x, b.x) : Double.compare(a.y, b.y);
            Arrays.sort(exp, cmp);
            Arrays.sort(got, cmp);
            for (int i = 0; i < n; i++) {
                if (Math.abs(exp[i].x - got[i].x) > CMP_EPS ||
                        Math.abs(exp[i].y - got[i].y) > CMP_EPS) {
                    System.err.printf(Locale.US,
                            "compareExpected: point #%d expected (%.6f, %.6f) got (%.6f, %.6f)%n",
                            i, exp[i].x, exp[i].y, got[i].x, got[i].y);
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

//            while (first + 1 < n && T(points[0], points[first], points[first + 1]) == 0) {
//                first++;
//            }

//            while (last - 1 > first && T(points[0], points[last], points[last - 1]) == 0) {
//                last--;
//            }
    }

    // ---------- алгоритм (как в предыдущем ответе) ----------
    static double cross(Pt a, Pt b, Pt c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    static class Edge {
        int a, b;
        ArrayList<Integer> conflict = new ArrayList<>();
        boolean alive = true;
    }

    private Edge[] edges;
    private int eCnt;

    private int newEdge(int a, int b) {
        int id = eCnt++;
        edges[id] = new Edge();
        edges[id].a = a; edges[id].b = b;
        return id;
    }

    private void buildChain(int L, int R) {
        int start = newEdge(L, R);
        for (int i = 0; i < pts.length; i++) {
            if (i == L || i == R) continue;
            if (cross(pts[L], pts[R], pts[i]) > EPS)
                edges[start].conflict.add(i);
        }
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int eid = stack.pop();
            if (!edges[eid].alive) continue;
            Edge e = edges[eid];
            if (e.conflict.isEmpty()) continue;
            int best = -1; double bestD = 0;
            for (int pi : e.conflict) {
                double d = cross(pts[e.a], pts[e.b], pts[pi]);
                if (d > bestD) { bestD = d; best = pi; }
            }
            if (best < 0) { e.conflict.clear(); continue; }
            int P = best;
            int e1 = newEdge(e.a, P);
            int e2 = newEdge(P, e.b);
            e.alive = false;
            for (int pi : e.conflict) {
                if (pi == P) continue;
                if (cross(pts[e.a], pts[P], pts[pi]) > EPS)
                    edges[e1].conflict.add(pi);
                else if (cross(pts[P], pts[e.b], pts[pi]) > EPS)
                    edges[e2].conflict.add(pi);
            }
            e.conflict.clear();
            stack.push(e1);
            stack.push(e2);
        }
    }

    private ArrayList<Integer> collectChain(int src, int dst) {
        ArrayList<Integer> path = new ArrayList<>();
        int cur = src; path.add(cur);
        int guard = 0;
        while (cur != dst && guard++ < edges.length + 5) {
            int found = -1;
            for (int i = 0; i < eCnt; i++)
                if (edges[i].alive && edges[i].a == cur) { found = i; break; }
            if (found < 0) break;
            cur = edges[found].b;
            path.add(cur);
        }
        return path;
    }

    private int[] quickHull(Pt[] pts) {
        int n = pts.length;
        if (n == 0) return new int[0];
        if (n == 1) return new int[]{0};
        int L = 0, R = 0;
        for (int i = 1; i < n; i++) {
            if (pts[i].x < pts[L].x || (pts[i].x == pts[L].x && pts[i].y < pts[L].y)) L = i;
            if (pts[i].x > pts[R].x || (pts[i].x == pts[R].x && pts[i].y > pts[R].y)) R = i;
        }
        if (L == R) return new int[]{L};
        boolean allCol = true;
        for (int i = 0; i < n && allCol; i++)
            if (i != L && i != R && Math.abs(cross(pts[L], pts[R], pts[i])) > EPS)
                allCol = false;
        if (allCol) return new int[]{L, R};

        int cap = Math.max(1, 3 + 2 * (n - 3));
        edges = new Edge[cap];
        eCnt = 0;

        buildChain(L, R);
        buildChain(R, L);
        ArrayList<Integer> upper = collectChain(L, R);
        ArrayList<Integer> lower = collectChain(R, L);
        ArrayList<Integer> res = new ArrayList<>(upper);
        for (int i = 1; i + 1 < lower.size(); i++) res.add(lower.get(i));
        int[] out = new int[res.size()];
        for (int i = 0; i < out.length; i++) out[i] = res.get(i);
        return out;
    }

    private static void normalize(Pt[] pts, int[] hull) {
        if (hull.length < 3) {
            if (hull.length == 2) {
                Pt a = pts[hull[0]], b = pts[hull[1]];
                if (a.x > b.x || (a.x == b.x && a.y > b.y)) {
                    int t = hull[0]; hull[0] = hull[1]; hull[1] = t;
                }
            }
            return;
        }
        double area2 = 0;
        for (int i = 0; i < hull.length; i++) {
            Pt a = pts[hull[i]], b = pts[hull[(i + 1) % hull.length]];
            area2 += a.x * b.y - b.x * a.y;
        }
        if (area2 < 0) {
            for (int i = 0, j = hull.length - 1; i < j; i++, j--) {
                int t = hull[i]; hull[i] = hull[j]; hull[j] = t;
            }
        }
        int best = 0;
        for (int i = 1; i < hull.length; i++) {
            Pt a = pts[hull[i]], b = pts[hull[best]];
            if (a.x < b.x || (a.x == b.x && a.y < b.y)) best = i;
        }
        int[] tmp = new int[hull.length];
        for (int i = 0; i < hull.length; i++) tmp[i] = hull[(best + i) % hull.length];
        System.arraycopy(tmp, 0, hull, 0, hull.length);
    }
}
