#!/usr/bin/env python3
"""
Единый раннер Python-решений.

usage: main.py <solution> <input> <expected>
  solutions: quick_hull
"""

import os
import sys

# Разрешить импорт соседних модулей при запуске из любого cwd
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from quick_hull import QuickHullSolution  # noqa: E402


def make_solution(name: str):
    if name == "quick_hull":
        return QuickHullSolution()
    # if name == "quick_hull_3d":
    #     return QuickHull3DSolution()
    return None


def read_file(path: str) -> str:
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def main(argv) -> int:
    if len(argv) < 4:
        print(f"usage: {argv[0]} <solution> <input> <expected>", file=sys.stderr)
        return 2

    sol_name = argv[1]
    sol = make_solution(sol_name)
    if sol is None:
        print(f"unknown solution: {sol_name}", file=sys.stderr)
        return 2

    sol.readInput(read_file(argv[2]))
    sol.solve()
    ok = sol.compareExpected(read_file(argv[3]))
    print(("OK" if ok else "FAIL") + " " + sol_name)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
