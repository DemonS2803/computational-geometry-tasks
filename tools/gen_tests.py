"""
Генератор тестовых данных для задачи выпуклой оболочки.

Создаёт пары inputN.txt / expectedN.txt в каталоге --out (по умолчанию
tests/quick_hull). Формат полностью совпадает с тем, что ожидает раннер:

  inputN.txt    : N \n x1 y1 \n ... \n xN yN
  expectedN.txt : K \n x1 y1 \n ... \n xK yK   (CCW, старт с min-x,min-y)

Режимы (--mode):
  uniform    — равномерно в квадрате
  circle     — на окружности + шум
  clusters   — несколько гауссовых сгустков
  collinear  — почти все на прямой (стресс на вырожденность)
  duplicates — с повторами и точками на рёбрах
  mixed      — случайно выбирает один из режимов для каждого файла

Корректность expected вычисляется алгоритмом Эндрю (monotone chain) — это
эталон, независимый от твоего QuickHull, что полезно для перекрёстной проверки.
"""

import argparse
import math
import os
import random
import sys
from typing import List, Tuple

Point = Tuple[float, float]


# ---------------------------------------------------------------------------
# Эталонная выпуклая оболочка: monotone chain (Andrew, 1979)
# ---------------------------------------------------------------------------
def cross(o: Point, a: Point, b: Point) -> float:
    return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])


def convex_hull(points: List[Point]) -> List[Point]:
    pts = sorted(set(points))
    if len(pts) <= 1:
        return pts

    lower = []
    for p in pts:
        while len(lower) >= 2 and cross(lower[-2], lower[-1], p) <= 0:
            lower.pop()
        lower.append(p)

    upper = []
    for p in reversed(pts):
        while len(upper) >= 2 and cross(upper[-2], upper[-1], p) <= 0:
            upper.pop()
        upper.append(p)

    # lower и upper содержат концевые точки дважды — убираем
    return lower[:-1] + upper[:-1]


def normalize_ccw_min_first(hull: List[Point]) -> List[Point]:
    """CCW + старт с лексикографически минимальной точки."""
    if len(hull) < 3:
        return sorted(hull)
    # ориентированная площадь
    area2 = 0.0
    n = len(hull)
    for i in range(n):
        x1, y1 = hull[i]
        x2, y2 = hull[(i + 1) % n]
        area2 += x1 * y2 - x2 * y1
    if area2 < 0:
        hull = list(reversed(hull))
    # старт с min(x, y)
    best = min(range(len(hull)), key=lambda i: (hull[i][0], hull[i][1]))
    return hull[best:] + hull[:best]


# ---------------------------------------------------------------------------
# Генераторы облаков
# ---------------------------------------------------------------------------
def gen_uniform(n: int, rng: random.Random, lo: float = -1000, hi: float = 1000) -> List[Point]:
    return [(rng.uniform(lo, hi), rng.uniform(lo, hi)) for _ in range(n)]


def gen_circle(n: int, rng: random.Random, r: float = 1000.0, noise: float = 0.0) -> List[Point]:
    pts = []
    for _ in range(n):
        a = rng.uniform(0, 2 * math.pi)
        rr = r + rng.uniform(-noise, noise)
        pts.append((rr * math.cos(a), rr * math.sin(a)))
    return pts


def gen_clusters(n: int, rng: random.Random, k: int = 5, spread: float = 200.0) -> List[Point]:
    centers = [(rng.uniform(-1000, 1000), rng.uniform(-1000, 1000)) for _ in range(k)]
    pts = []
    for _ in range(n):
        cx, cy = rng.choice(centers)
        pts.append((rng.gauss(cx, spread), rng.gauss(cy, spread)))
    return pts


def gen_collinear(n: int, rng: random.Random) -> List[Point]:
    # все на прямой y = 2x + 3, плюс пара выбросов — гарантирует нетривиальную оболочку
    pts = []
    for _ in range(n - 2):
        x = rng.uniform(-1000, 1000)
        pts.append((x, 2 * x + 3))
    pts.append((rng.uniform(-1000, 1000), rng.uniform(-1000, 1000)))
    pts.append((rng.uniform(-1000, 1000), rng.uniform(-1000, 1000)))
    rng.shuffle(pts)
    return pts


def gen_duplicates(n: int, rng: random.Random) -> List[Point]:
    base = gen_uniform(max(1, n // 3), rng)
    pts = list(base)
    while len(pts) < n:
        if rng.random() < 0.5:
            pts.append(rng.choice(base))                 # точный дубликат
        else:
            x1, y1 = rng.choice(base)
            x2, y2 = rng.choice(base)
            t = rng.random()
            pts.append((x1 + t * (x2 - x1), y1 + t * (y2 - y1)))  # точка на отрезке
    rng.shuffle(pts)
    return pts


MODES = {
    "uniform":    lambda n, rng: gen_uniform(n, rng),
    "circle":     lambda n, rng: gen_circle(n, rng, noise=50.0),
    "clusters":   lambda n, rng: gen_clusters(n, rng),
    "collinear":  lambda n, rng: gen_collinear(n, rng),
    "duplicates": lambda n, rng: gen_duplicates(n, rng),
}


# ---------------------------------------------------------------------------
# Запись
# ---------------------------------------------------------------------------
def fmt_point(p: Point) -> str:
    return f"{p[0]:.6f} {p[1]:.6f}"


def write_pair(out_dir: str, idx: int, pts: List[Point], hull: List[Point]) -> None:
    os.makedirs(out_dir, exist_ok=True)
    inp_path = os.path.join(out_dir, f"input{idx}.txt")
    exp_path = os.path.join(out_dir, f"expected{idx}.txt")

    with open(inp_path, "w") as f:
        f.write(f"{len(pts)}\n")
        for p in pts:
            f.write(fmt_point(p) + "\n")

    with open(exp_path, "w") as f:
        f.write(f"{len(hull)}\n")
        for p in hull:
            f.write(fmt_point(p) + "\n")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
def parse_sizes(spec: str) -> List[int]:
    """'10,100,1000' или '10:1000:10' (start:stop:step, stop включительно)."""
    if ":" in spec:
        a, b, c = spec.split(":")
        return list(range(int(a), int(b) + 1, int(c)))
    return [int(x) for x in spec.split(",") if x.strip()]


def main() -> int:
    ap = argparse.ArgumentParser(description="Generate convex-hull test pairs")
    ap.add_argument("--out", default="tests/quick_hull", help="output directory")
    ap.add_argument("--sizes", default="10,100,1000",
                    help="comma list or start:stop:step, e.g. 10,100,1000 or 10:1000:10")
    ap.add_argument("--mode", default="uniform",
                    choices=list(MODES) + ["mixed"],
                    help="point distribution (or 'mixed' to pick per-file)")
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--start-index", type=int, default=1,
                    help="first N in inputN/expectedN (default: 1)")
    ap.add_argument("--overwrite", action="store_true",
                    help="overwrite existing files (default: refuse)")
    args = ap.parse_args()

    sizes = parse_sizes(args.sizes)
    if not sizes:
        print("error: empty --sizes", file=sys.stderr)
        return 2

    rng = random.Random(args.seed)
    idx = args.start_index
    written = 0

    for n in sizes:
        if n < 1:
            continue
        inp_path = os.path.join(args.out, f"input{idx}.txt")
        exp_path = os.path.join(args.out, f"expected{idx}.txt")
        if not args.overwrite and (os.path.exists(inp_path) or os.path.exists(exp_path)):
            print(f"skip {idx}: {inp_path} or {exp_path} exists (use --overwrite)", file=sys.stderr)
            idx += 1
            continue

        mode = args.mode
        if mode == "mixed":
            mode = rng.choice(list(MODES))
        pts = MODES[mode](n, rng)
        hull = normalize_ccw_min_first(convex_hull(pts))
        write_pair(args.out, idx, pts, hull)
        print(f"wrote {args.out}/input{idx}.txt  (n={n}, mode={mode}, hull={len(hull)})")
        idx += 1
        written += 1

    print(f"done: {written} pair(s) in {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
