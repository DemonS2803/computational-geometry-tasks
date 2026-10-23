"""
QuickHull (2D) — реализация Solution на Python.

Контракт совпадает с C++/Java:
    readInput(data: str)
    solve()
    compareExpected(expected: str) -> bool

Алгоритм — тот же, что в C++/Java-версиях: массив рёбер с конфликтными
списками, верхняя и нижняя цепочки, нормализация в конце.

Оговорка: Python в 30–100 раз медленнее C++ на этой задаче — используй
для корректности, не для бенчмарков. perf на CPython почти бессмыслен.
"""

from __future__ import annotations
import math
import sys
from typing import List, Tuple

EPS = 1e-12
CMP_EPS = 1e-6


class Point:
    __slots__ = ("x", "y", "id")

    def __init__(self, x: float, y: float, idx: int):
        self.x = x
        self.y = y
        self.id = idx

    def __repr__(self) -> str:
        return f"({self.x:.6f},{self.y:.6f})"


def cross(a: Point, b: Point, c: Point) -> float:
    return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)


class Edge:
    __slots__ = ("a", "b", "conflict", "alive")

    def __init__(self, a: int, b: int):
        self.a = a
        self.b = b
        self.conflict: List[int] = []
        self.alive = True


class QuickHullSolution:
    """Решение задачи 2D convex hull. Совместимо с раннером Solution."""

    def __init__(self) -> None:
        self.pts: List[Point] = []
        self.hull: List[int] = []
        self._edges: List[Edge] = []
        self._ecnt = 0

    # ------------------------- Solution API ---------------------------
    def readInput(self, data: str) -> None:
        tok = data.split()
        if not tok:
            self.pts = []
            return
        n = int(tok[0])
        self.pts = []
        pos = 1
        for i in range(n):
            x = float(tok[pos]); y = float(tok[pos + 1]); pos += 2
            self.pts.append(Point(x, y, i))
        self.hull = []

    def solve(self) -> None:
        self.hull = self._quick_hull(self.pts)
        self._normalize(self.pts, self.hull)

    def compareExpected(self, expected: str) -> bool:
        tok = expected.split()
        if not tok:
            print("compareExpected: empty expected", file=sys.stderr)
            return False
        n = int(tok[0])
        exp: List[Tuple[float, float]] = []
        pos = 1
        for _ in range(n):
            x = float(tok[pos]); y = float(tok[pos + 1]); pos += 2
            exp.append((x, y))

        if n != len(self.hull):
            print(
                f"compareExpected: size mismatch: expected {n}, got {len(self.hull)}",
                file=sys.stderr,
            )
            return False

        got = [(self.pts[v].x, self.pts[v].y) for v in self.hull]
        exp.sort()
        got.sort()
        for i, (e, g) in enumerate(zip(exp, got)):
            if abs(e[0] - g[0]) > CMP_EPS or abs(e[1] - g[1]) > CMP_EPS:
                print(
                    f"compareExpected: point #{i} expected ({e[0]:.6f},{e[1]:.6f})"
                    f" got ({g[0]:.6f},{g[1]:.6f})",
                    file=sys.stderr,
                )
                return False
        return True

    # ------------------------- алгоритм -------------------------------
    def _new_edge(self, a: int, b: int) -> int:
        idx = self._ecnt
        self._ecnt += 1
        self._edges.append(Edge(a, b))
        return idx

    def _build_chain(self, L: int, R: int) -> None:
        pts = self.pts
        start = self._new_edge(L, R)
        for i in range(len(pts)):
            if i == L or i == R:
                continue
            if cross(pts[L], pts[R], pts[i]) > EPS:
                self._edges[start].conflict.append(i)

        stack = [start]
        while stack:
            eid = stack.pop()
            e = self._edges[eid]
            if not e.alive or not e.conflict:
                continue

            best = -1
            best_d = 0.0
            for pi in e.conflict:
                d = cross(pts[e.a], pts[e.b], pts[pi])
                if d > best_d:
                    best_d = d
                    best = pi
            if best < 0:
                e.conflict.clear()
                continue

            P = best
            e1 = self._new_edge(e.a, P)
            e2 = self._new_edge(P, e.b)
            e.alive = False

            for pi in e.conflict:
                if pi == P:
                    continue
                if cross(pts[e.a], pts[P], pts[pi]) > EPS:
                    self._edges[e1].conflict.append(pi)
                elif cross(pts[P], pts[e.b], pts[pi]) > EPS:
                    self._edges[e2].conflict.append(pi)
            e.conflict.clear()

            stack.append(e1)
            stack.append(e2)

    def _collect_chain(self, src: int, dst: int) -> List[int]:
        path = [src]
        cur = src
        guard = 0
        while cur != dst and guard < len(self._edges) + 5:
            found = -1
            for i in range(self._ecnt):
                if self._edges[i].alive and self._edges[i].a == cur:
                    found = i
                    break
            if found < 0:
                break
            cur = self._edges[found].b
            path.append(cur)
            guard += 1
        return path

    def _quick_hull(self, pts: List[Point]) -> List[int]:
        n = len(pts)
        if n == 0:
            return []
        if n == 1:
            return [0]

        L = 0
        R = 0
        for i in range(1, n):
            if pts[i].x < pts[L].x or (pts[i].x == pts[L].x and pts[i].y < pts[L].y):
                L = i
            if pts[i].x > pts[R].x or (pts[i].x == pts[R].x and pts[i].y > pts[R].y):
                R = i
        if L == R:
            return [L]

        all_col = True
        for i in range(n):
            if i != L and i != R and abs(cross(pts[L], pts[R], pts[i])) > EPS:
                all_col = False
                break
        if all_col:
            return [L, R]

        self._edges = []
        self._ecnt = 0

        self._build_chain(L, R)
        self._build_chain(R, L)
        upper = self._collect_chain(L, R)
        lower = self._collect_chain(R, L)

        res = list(upper)
        for i in range(1, len(lower) - 1):
            res.append(lower[i])
        return res

    @staticmethod
    def _normalize(pts: List[Point], hull: List[int]) -> None:
        if len(hull) < 3:
            if len(hull) == 2:
                a, b = pts[hull[0]], pts[hull[1]]
                if a.x > b.x or (a.x == b.x and a.y > b.y):
                    hull[0], hull[1] = hull[1], hull[0]
            return

        area2 = 0.0
        m = len(hull)
        for i in range(m):
            a = pts[hull[i]]
            b = pts[hull[(i + 1) % m]]
            area2 += a.x * b.y - b.x * a.y
        if area2 < 0:
            hull.reverse()

        best = 0
        for i in range(1, len(hull)):
            a = pts[hull[i]]
            b = pts[hull[best]]
            if a.x < b.x or (a.x == b.x and a.y < b.y):
                best = i
        hull[:] = hull[best:] + hull[:best]
