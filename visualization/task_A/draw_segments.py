#!/usr/bin/env python3
"""
Визуализация пар отрезков из genTest().
Каждая пара (test case) рисует 2 отрезка одинаковым цветом.
Разные test cases — разные цвета.
"""

import re
import sys
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np


# ---------------------------------------------------------------------------
# Парсинг: извлекаем testId -> list[segments] из C++ исходника
# ---------------------------------------------------------------------------

def _extract_brace_block(text, start_pos):
    """Извлекает содержимое { ... } начиная с start_pos (указывает на '{')."""
    depth = 0
    i = start_pos
    while i < len(text):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return text[start_pos + 1:i]  # без внешних { }
        i += 1
    return ""


def parse_seg_intersection_tests(filepath: str) -> dict:
    """
    Возвращает dict: testId -> list[tuple[(x1,y1), (x2,y2), (x3,y3), (x4,y4)]]
    """
    with open(filepath, "r") as f:
        content = f.read()

    # Ищем switch(testId) { ... }
    switch_match = re.search(r"switch\s*\(\s*testId\s*\)\s*\{", content)
    if not switch_match:
        raise ValueError("Не найден switch(testId) в файле")

    switch_body = _extract_brace_block(content, switch_match.end() - 1)
    if not switch_body:
        raise ValueError("Не удалось извлечь тело switch")

    # Парсим каждый case N: return { ... };
    cases = {}
    case_pattern = r"case\s+(\d+):\s*return\s*\{"
    for m in re.finditer(case_pattern, switch_body):
        test_id = int(m.group(1))
        # Извлекаем числа из блока {...}
        # Находим позицию '{' после 'return'
        return_start = m.end() - 1  # указывает на '{'
        block = _extract_brace_block(switch_body, return_start)
        # Извлекаем все числа
        nums = [float(x) for x in re.findall(r"[-+]?\d*\.?\d+", block)]
        # Каждые 2 числа — точка, 4 точки = 2 отрезка
        pts = [(nums[i], nums[i + 1]) for i in range(0, len(nums), 2)]
        segments = []
        for i in range(0, len(pts), 2):
            seg = (pts[i], pts[i + 1])
            segments.append(seg)
        cases[test_id] = segments

    return cases


# ---------------------------------------------------------------------------
# Вспомогательные функции для геометрии
# ---------------------------------------------------------------------------

def _cross_product(o, a, b):
    """Векторное произведение (OA x OB)."""
    return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])


def _on_segment(a, b, p, eps=1e-9):
    """Проверка, лежит ли точка p на отрезке ab (при условии коллинеарности)."""
    return (min(a[0], b[0]) - eps <= p[0] <= max(a[0], b[0]) + eps and
            min(a[1], b[1]) - eps <= p[1] <= max(a[1], b[1]) + eps)


def _segments_intersect(seg1, seg2):
    """Проверяет, пересекаются ли два отрезка (по логике sol.cpp)."""
    a1, a2 = seg1
    b1, b2 = seg2

    d1 = _cross_product(b1, b2, a1)
    d2 = _cross_product(b1, b2, a2)
    d3 = _cross_product(a1, a2, b1)
    d4 = _cross_product(a1, a2, b2)

    EPS = 1e-12

    def sign(v):
        if v > EPS:
            return 1
        if v < -EPS:
            return -1
        return 0

    s1, s2, s3, s4 = sign(d1), sign(d2), sign(d3), sign(d4)

    if s1 * s2 < 0 and s3 * s4 < 0:
        return True  # строгое пересечение

    if s1 == 0 and _on_segment(b1, b2, a1):
        return True
    if s2 == 0 and _on_segment(b1, b2, a2):
        return True
    if s3 == 0 and _on_segment(a1, a2, b1):
        return True
    if s4 == 0 and _on_segment(a1, a2, b2):
        return True

    return False


def _intersection_point(seg1, seg2):
    """Находит точку пересечения двух отрезков (если есть)."""
    (x1, y1), (x2, y2) = seg1
    (x3, y3), (x4, y4) = seg2
    denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
    if abs(denom) < 1e-10:
        return None  # параллельные
    t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom
    return (x1 + t * (x2 - x1), y1 + t * (y2 - y1))


# ---------------------------------------------------------------------------
# Отрисовка
# ---------------------------------------------------------------------------

def _get_plot_limits(segments, padding=1.0):
    all_x = [p[0] for seg in segments for p in seg]
    all_y = [p[1] for seg in segments for p in seg]
    return ((min(all_x) - padding, max(all_x) + padding),
            (min(all_y) - padding, max(all_y) + padding))


def draw_all(tests: dict, output_path: str = "all_tests.png"):
    """Общая визуализация всех test cases в одной картинке (2×5 сетка).
    Каждая пара отрезков (segments[i], segments[i+1]) имеет свой цвет."""
    cmap = plt.cm.tab20
    num_pairs = sum(len(segments) // 2 for segments in tests.values())

    fig, axes = plt.subplots(2, 5, figsize=(20, 8))
    axes = axes.flatten()

    pair_idx = 0
    for test_id in sorted(tests.keys()):
        ax = axes[test_id - 1]
        segments = tests[test_id]
        num_pairs_in_test = len(segments) // 2

        for i in range(0, len(segments), 2):
            color = cmap(pair_idx % len(cmap.colors))
            pair_idx += 1
            (x1, y1), (x2, y2) = segments[i]
            (x3, y3), (x4, y4) = segments[i + 1]
            ax.plot([x1, x2], [y1, y2], color=color, linewidth=2.5, marker='o', markersize=4)
            ax.plot([x3, x4], [y3, y4], color=color, linewidth=2.5, marker='o', markersize=4)

        ax.set_xlim(*_get_plot_limits(segments)[0])
        ax.set_ylim(*_get_plot_limits(segments)[1])
        ax.set_aspect("equal")
        ax.grid(True, alpha=0.3)
        ax.set_title(f"Test {test_id}")

    plt.tight_layout()
    plt.savefig(output_path, dpi=150, bbox_inches="tight")
    print(f"[1/4] Сохранено: {output_path}")
    plt.close(fig)


def draw_single(test_id: int, segments: list, output_path: str = "test_single.png"):
    """Точечная визуализация одного test case.
    Каждая пара отрезков (segments[i], segments[i+1]) имеет свой цвет."""
    cmap = plt.cm.viridis
    pair_idx = 0

    fig, ax = plt.subplots(figsize=(8, 8))

    for i in range(0, len(segments), 2):
        color = cmap(pair_idx / cmap.N)
        pair_idx += 1
        (x1, y1), (x2, y2) = segments[i]
        (x3, y3), (x4, y4) = segments[i + 1]
        ax.plot([x1, x2], [y1, y2], color=color, linewidth=3,
                marker='o', markersize=6)
        ax.plot([x3, x4], [y3, y4], color=color, linewidth=3,
                marker='o', markersize=6)
        ax.annotate(f"s{pair_idx}", (x2, y2), fontsize=11, color=color,
                    xytext=(8, 10), textcoords='offset points')
        ax.annotate(f"s{pair_idx}", (x4, y4), fontsize=11, color=color,
                    xytext=(8, 10), textcoords='offset points')

    xlim, ylim = _get_plot_limits(segments)
    ax.set_xlim(xlim)
    ax.set_ylim(ylim)
    ax.set_aspect("equal")
    ax.grid(True, alpha=0.3)
    ax.set_title(f"Test {test_id} — segments")

    plt.tight_layout()
    plt.savefig(output_path, dpi=150, bbox_inches="tight")
    print(f"[2/4] Сохранено: {output_path}")
    plt.close(fig)


def draw_annotated(test_id: int, segments: list, output_path: str = "test_annotated.png"):
    """Визуализация с метками и точками пересечения.
    Каждая пара отрезков имеет свой цвет."""
    cmap = plt.cm.plasma
    pair_idx = 0

    fig, ax = plt.subplots(figsize=(10, 8))

    for i in range(0, len(segments), 2):
        color = cmap(pair_idx / cmap.N)
        pair_idx += 1
        (x1, y1), (x2, y2) = segments[i]
        (x3, y3), (x4, y4) = segments[i + 1]
        ax.plot([x1, x2], [y1, y2], color=color, linewidth=3,
                marker='o', markersize=6)
        ax.plot([x3, x4], [y3, y4], color=color, linewidth=3,
                marker='o', markersize=6)
        ax.annotate(f"s{pair_idx}", (x2, y2), fontsize=11, color=color,
                    xytext=(8, 10), textcoords='offset points')
        ax.annotate(f"s{pair_idx}", (x4, y4), fontsize=11, color=color,
                    xytext=(8, 10), textcoords='offset points')

    # Находим и отмечаем точки пересечения
    for i in range(len(segments)):
        for j in range(i + 1, len(segments)):
            if _segments_intersect(segments[i], segments[j]):
                pt = _intersection_point(segments[i], segments[j])
                if pt:
                    ax.plot(pt[0], pt[1], 'r*', markersize=18,
                            markeredgecolor='darkred', markeredgewidth=2)

    xlim, ylim = _get_plot_limits(segments)
    ax.set_xlim(xlim)
    ax.set_ylim(ylim)
    ax.set_aspect("equal")
    ax.grid(True, alpha=0.3)
    ax.set_title(f"Test {test_id} — annotated (red *= пересечение)")

    plt.tight_layout()
    plt.savefig(output_path, dpi=150, bbox_inches="tight")
    print(f"[3/4] Сохранено: {output_path}")
    plt.close(fig)


def draw_comparison(test_id: int, segments: list,
                    expected_output: str, output_path: str = "test_comparison.png"):
    """Сравнение: отрисовка + текстовая подсказка с expected output.
    Каждая пара отрезков имеет свой цвет."""
    cmap = plt.cm.coolwarm
    pair_idx = 0

    fig, (ax, text_ax) = plt.subplots(1, 2, figsize=(14, 6))

    for i in range(0, len(segments), 2):
        color = cmap(pair_idx / cmap.N)
        pair_idx += 1
        (x1, y1), (x2, y2) = segments[i]
        (x3, y3), (x4, y4) = segments[i + 1]
        ax.plot([x1, x2], [y1, y2], color=color, linewidth=2.5, marker='o', markersize=5)
        ax.plot([x3, x4], [y3, y4], color=color, linewidth=2.5, marker='o', markersize=5)

    # Отмечаем пересечения
    intersections = []
    for i in range(len(segments)):
        for j in range(i + 1, len(segments)):
            if _segments_intersect(segments[i], segments[j]):
                pt = _intersection_point(segments[i], segments[j])
                if pt:
                    intersections.append(pt)
                    ax.plot(pt[0], pt[1], 'r*', markersize=16,
                            markeredgecolor='darkred', markeredgewidth=2)

    xlim, ylim = _get_plot_limits(segments)
    ax.set_xlim(xlim)
    ax.set_ylim(ylim)
    ax.set_aspect("equal")
    ax.grid(True, alpha=0.3)
    ax.set_title(f"Test {test_id}")

    # Текстовая панель
    text_ax.axis('off')
    text_ax.text(0.1, 0.5,
                 f"Test {test_id}\n"
                 f"Segments: {len(segments)} pairs\n"
                 f"Intersections: {len(intersections)}\n"
                 f"Expected: {expected_output}",
                 fontsize=12, family='monospace',
                 verticalalignment='center')

    plt.tight_layout()
    plt.savefig(output_path, dpi=150, bbox_inches="tight")
    print(f"[4/4] Сохранено: {output_path}")
    plt.close(fig)


# ---------------------------------------------------------------------------
# Главный entry-point
# ---------------------------------------------------------------------------

def main():
    filepath = sys.argv[1] if len(sys.argv) > 1 else "../../src/main/c++/task_A/seg_intersection_tests.h"

    tests = parse_seg_intersection_tests(filepath)
    print(f"✅ Найдено test cases: {sorted(tests.keys())}")

    # Генерируем expected output для каждого test case
    for test_id in sorted(tests.keys()):
        segments = tests[test_id]
        expected = ""
        # Каждый тест состоит из пар сегментов: segments[0] vs segments[1], segments[2] vs segments[3], etc.
        for i in range(0, len(segments), 2):
            seg1 = segments[i]
            seg2 = segments[i + 1]
            expected += "Y" if _segments_intersect(seg1, seg2) else "N"
        print(f"   Test {test_id}: {expected}")

    # Визуализация
    draw_all(tests, "all_tests.png")

    for test_id in sorted(tests.keys()):
        segments = tests[test_id]
        expected = ""
        for i in range(0, len(segments), 2):
            expected += "Y" if _segments_intersect(segments[i], segments[i + 1]) else "N"

        draw_single(test_id, segments, f"test_{test_id}.png")
        draw_annotated(test_id, segments, f"test_{test_id}_annotated.png")
        draw_comparison(test_id, segments, expected, f"test_{test_id}_comparison.png")

    print(f"\n📊 Все изображения сохранены в текущую директорию")
    print(f"   - all_tests.png            (2×5 сетка всех тестов)")
    print(f"   - test_N.png               (отдельные plot для каждого test)")
    print(f"   - test_N_annotated.png     (с точками пересечения)")
    print(f"   - test_N_comparison.png    (с expected output)")


if __name__ == "__main__":
    main()
