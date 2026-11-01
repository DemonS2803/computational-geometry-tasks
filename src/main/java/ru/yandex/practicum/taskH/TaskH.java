package ru.yandex.practicum.taskH;

import java.io.*;
import java.util.*;

public class TaskH {

    private static final int OFF = 10000;
    private static final int SIZE = 20001;   // x + OFF

    private int currentX;                    // текущая позиция заметающей прямой

    // ------------------------------------------------------------- отрезок

    private static final class Seg {
        final int x1, y1, x2, y2, id;
        final boolean probe;                 // «пробник» для запросов ceiling
        final long den;                      // x2 - x1 > 0
        final long num0;                     // y1*den - (y2-y1)*x1

        Seg(int x1, int y1, int x2, int y2, int id) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.id = id;
            this.probe = false;
            this.den = (long) x2 - x1;
            this.num0 = (long) y1 * den - (long) (y2 - y1) * x1;
        }

        Seg(long probeY) {
            this.x1 = this.y1 = this.x2 = this.y2 = 0;
            this.id = -1;
            this.probe = true;
            this.den = 1;
            this.num0 = probeY;
        }

        long num(int x) {                    // y(x) = num(x) / den
            return probe ? num0 : num0 + (long) (y2 - y1) * x;
        }
    }

    private int compare(Seg a, Seg b) {
        long l = a.num(currentX) * b.den;
        long r = b.num(currentX) * a.den;
        if (l != r) return l < r ? -1 : 1;
        if (a.probe || b.probe) return 0;
        return Integer.compare(a.id, b.id);   // ничьи бывают только при пересечении
    }

    // ------------------------------------------------ геометрия (точно)

    private static long cross(long ax, long ay, long bx, long by, long cx, long cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static boolean onSegment(long ax, long ay, long bx, long by, long px, long py) {
        return Math.min(ax, bx) <= px && px <= Math.max(ax, bx)
                && Math.min(ay, by) <= py && py <= Math.max(ay, by)
                && cross(ax, ay, bx, by, px, py) == 0;
    }

    private static boolean intersects(Seg a, Seg b) {
        long d1 = cross(b.x1, b.y1, b.x2, b.y2, a.x1, a.y1);
        long d2 = cross(b.x1, b.y1, b.x2, b.y2, a.x2, a.y2);
        long d3 = cross(a.x1, a.y1, a.x2, a.y2, b.x1, b.y1);
        long d4 = cross(a.x1, a.y1, a.x2, a.y2, b.x2, b.y2);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return true;

        if (d1 == 0 && onSegment(b.x1, b.y1, b.x2, b.y2, a.x1, a.y1)) return true;
        if (d2 == 0 && onSegment(b.x1, b.y1, b.x2, b.y2, a.x2, a.y2)) return true;
        if (d3 == 0 && onSegment(a.x1, a.y1, a.x2, a.y2, b.x1, b.y1)) return true;
        if (d4 == 0 && onSegment(a.x1, a.y1, a.x2, a.y2, b.x2, b.y2)) return true;
        return false;
    }

    // ------------------------------------------------------------ ввод

    private static final class FastReader {
        private final InputStream in;
        private final byte[] buf = new byte[1 << 16];
        private int pos, lim;

        FastReader(InputStream in) { this.in = in; }

        private int read() throws IOException {
            if (pos >= lim) {
                lim = in.read(buf, 0, buf.length);
                pos = 0;
                if (lim <= 0) return -1;
            }
            return buf[pos++];
        }

        int nextInt() throws IOException {
            int c;
            do { c = read(); } while (c != -1 && c <= ' ');
            int sign = 1;
            if (c == '-') { sign = -1; c = read(); }
            int val = 0;
            while (c > ' ') { val = val * 10 + (c - '0'); c = read(); }
            return val * sign;
        }
    }

    // ------------------------------------------------------------ решение

    @SuppressWarnings("unchecked")
    public void solve() throws IOException {
        FastReader in = new FastReader(System.in);
        int n = in.nextInt();

        List<Integer>[] startAt = new List[SIZE];
        List<Integer>[] endAt   = new List[SIZE];
        List<Integer>[] vertAt  = new List[SIZE];

        Seg[] seg = new Seg[n + 1];
        int[] vLo = new int[n + 1];
        int[] vHi = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            int x1 = in.nextInt(), y1 = in.nextInt();
            int x2 = in.nextInt(), y2 = in.nextInt();

            if (x1 == x2) {                       // вертикаль или точка
                vLo[i] = Math.min(y1, y2);
                vHi[i] = Math.max(y1, y2);
                int b = x1 + OFF;
                if (vertAt[b] == null) vertAt[b] = new ArrayList<>();
                vertAt[b].add(i);
            } else {                              // обычный отрезок
                if (x1 > x2) {
                    int t = x1; x1 = x2; x2 = t;
                    t = y1; y1 = y2; y2 = t;
                }
                seg[i] = new Seg(x1, y1, x2, y2, i);
                int b1 = x1 + OFF, b2 = x2 + OFF;
                if (startAt[b1] == null) startAt[b1] = new ArrayList<>();
                startAt[b1].add(i);
                if (endAt[b2] == null) endAt[b2] = new ArrayList<>();
                endAt[b2].add(i);
            }
        }

        TreeSet<Seg> active = new TreeSet<>(this::compare);
        int[] ans = null;

        outer:
        for (int x = -OFF; x <= OFF; x++) {
            currentX = x;

            // 1) вставляем начинающиеся здесь
            List<Integer> st = startAt[x + OFF];
            if (st != null) {
                for (int id : st) {
                    Seg s = seg[id];
                    Seg lo = active.lower(s);
                    Seg hi = active.higher(s);
                    if (lo != null && intersects(lo, s)) { ans = new int[]{lo.id, s.id}; break outer; }
                    if (hi != null && intersects(s, hi)) { ans = new int[]{s.id, hi.id}; break outer; }
                    active.add(s);
                }
            }

            // 2) вертикали при данном x
            List<Integer> vt = vertAt[x + OFF];
            if (vt != null) {
                int k = vt.size();

                // вертикаль vs вертикаль — перекрытие диапазонов y
                if (k > 1) {
                    Integer[] ord = vt.toArray(new Integer[0]);
                    Arrays.sort(ord, (a, b) -> Integer.compare(vLo[a], vLo[b]));
                    int bestHi = vHi[ord[0]], bestId = ord[0];
                    for (int t = 1; t < k; t++) {
                        int id = ord[t];
                        if (vLo[id] <= bestHi) { ans = new int[]{bestId, id}; break outer; }
                        if (vHi[id] > bestHi) { bestHi = vHi[id]; bestId = id; }
                    }
                }

                // вертикаль vs активные отрезки: первый с y(x) >= vLo
                for (int id : vt) {
                    Seg cand = active.ceiling(new Seg(vLo[id]));
                    if (cand != null && cand.num(x) <= (long) vHi[id] * cand.den) {
                        ans = new int[]{id, cand.id};
                        break outer;
                    }
                }
            }

            // 3) удаляем заканчивающиеся здесь, проверяя новых соседей
            List<Integer> en = endAt[x + OFF];
            if (en != null) {
                for (int id : en) {
                    Seg s = seg[id];
                    Seg lo = active.lower(s);
                    Seg hi = active.higher(s);
                    active.remove(s);
                    if (lo != null && hi != null && intersects(lo, hi)) {
                        ans = new int[]{lo.id, hi.id};
                        break outer;
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        if (ans == null) {
            sb.append("NO");
        } else {
            sb.append("YES\n").append(ans[0]).append(' ').append(ans[1]);
        }
        System.out.println(sb);
    }

    public static void main(String[] args) throws IOException {
        new TaskH().solve();
    }
}
