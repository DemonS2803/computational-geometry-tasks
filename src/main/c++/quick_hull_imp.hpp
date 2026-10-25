//#pragma once
//#include "solution.hpp"
//#include <vector>
//#include <string>
//#include <sstream>
//#include <iostream>
//#include <iomanip>
//#include <cmath>
//#include <algorithm>
//
//struct Point {
//    double x, y;
//    int id;
//};
//
//class QuickHullImp : public Solution {
//public:
//    void readInput(const std::string& data) override {
//        std::istringstream in(data);
//        int n = 0;
//        in >> n;
//        pts_.assign(n, {});
//        for (int i = 0; i < n; ++i) {
//            in >> pts_[i].x >> pts_[i].y;
//            pts_[i].id = i;
//        }
//        hull_.clear();
//    }
//
//    void solve() override {
//        hull_ = quickHull(pts_);
//        normalize(pts_, hull_);
//    }
//
//    bool compareExpected(const std::string& expected) override {
//        std::istringstream in(expected);
//        int n = 0;
//        if (!(in >> n)) {
//            std::cerr << "compareExpected: cannot parse expected N\n";
//            return false;
//        }
//        std::vector<Point> exp(n);
//        for (int i = 0; i < n; ++i) {
//            if (!(in >> exp[i].x >> exp[i].y)) {
//                std::cerr << "compareExpected: cannot parse expected point #" << i << "\n";
//                return false;
//            }
//        }
//        if (n != (int)hull_.size()) {
//            std::cerr << "compareExpected: size mismatch: expected " << n
//                      << ", got " << hull_.size() << "\n";
//            return false;
//        }
//        std::vector<Point> got(hull_.size());
//        for (size_t i = 0; i < hull_.size(); ++i) got[i] = pts_[hull_[i]];
//        auto key = [](const Point& p){ return std::make_pair(p.x, p.y); };
//        auto cmp = [&](const Point& a, const Point& b){ return key(a) < key(b); };
//        std::sort(exp.begin(), exp.end(), cmp);
//        std::sort(got.begin(), got.end(), cmp);
//        const double EPS = 1e-6;
//        for (int i = 0; i < n; ++i) {
//            if (std::fabs(exp[i].x - got[i].x) > EPS ||
//                std::fabs(exp[i].y - got[i].y) > EPS) {
//                std::cerr << std::fixed << std::setprecision(6)
//                          << "compareExpected: point #" << i
//                          << " expected (" << exp[i].x << ", " << exp[i].y << ")"
//                          << " got ("      << got[i].x << ", " << got[i].y << ")\n";
//                return false;
//            }
//        }
//        return true;
//    }
//
//private:
//    std::vector<Point> pts_;
//    std::vector<int> hull_;
//
//    static double cross(const Point& a, const Point& b, const Point& c) {
//        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
//    }
//
//    // Edge now stores:
//    //   - start:  index of the start point (A)
//    //   - end:    index of the end point (B)   <-- kept for convenience
//    //   - next:   index of the next edge in the chain (its .start == this->end)
//    //   - conflict: points on the outer side of this directed edge
//    struct Edge {
//        int start = -1;
//        int end   = -1;
//        int next  = -1;
//        std::vector<int> conflict;
//        bool alive = true;
//    };
//
//    std::vector<Edge> edges_;
//    int eCap_ = 0, eCnt_ = 0;
//
//    // Create a new edge (a -> b) and link it after `prev` if prev >= 0.
//    int newEdge(int a, int b) {
//        int id = eCnt_++;
//        edges_[id] = Edge{};
//        edges_[id].start = a;
//        edges_[id].end   = b;
//        return id;
//    }
//
//    // Build chain from L to R using the point indices.
//    // Returns the head edge id; edges are chained via `next`.
//    int buildChain(int L, int R) {
//        int head = newEdge(L, R);
//        for (int i = 0; i < (int)pts_.size(); ++i) {
//            if (i == L || i == R) continue;
//            if (cross(pts_[L], pts_[R], pts_[i]) > 1e-12)
//                edges_[head].conflict.push_back(i);
//        }
//        std::vector<int> stack = {head};
//        while (!stack.empty()) {
//            int eid = stack.back(); stack.pop_back();
//            if (!edges_[eid].alive) continue;
//            auto& e = edges_[eid];
//            if (e.conflict.empty()) continue;
//
//            int best = -1; double bestD = 0;
//            for (int pi : e.conflict) {
//                double d = cross(pts_[e.start], pts_[e.end], pts_[pi]);
//                if (d > bestD) { bestD = d; best = pi; }
//            }
//            if (best < 0) { e.conflict.clear(); continue; }
//
//            int P = best;
//            // Split edge (A -> B) into (A -> P) and (P -> B).
//            int e1 = newEdge(e.start, P);
//            int e2 = newEdge(P, e.end);
//
//            // Link the new chain: e1 -> e2, and e1 inherits e's next-pointer.
//            edges_[e1].next = e2;
//            edges_[e2].next = e.next;
//
//            e.next = e1;
//
//            e.alive = false;
//
//            for (int pi : e.conflict) {
//                if (pi == P) continue;
//                if (cross(pts_[e.start], pts_[P], pts_[pi]) > 1e-12)
//                    edges_[e1].conflict.push_back(pi);
//                else if (cross(pts_[P], pts_[e.end], pts_[pi]) > 1e-12)
//                    edges_[e2].conflict.push_back(pi);
//            }
//            e.conflict.clear();
//
//            stack.push_back(e1);
//            stack.push_back(e2);
//        }
//        return head;
//    }
//
//    // Walk the chain starting at `head` until we reach `dst` (or run out).
//    // Collects start points of each edge, then appends `dst` at the end.
//    std::vector<int> collectChain(int head, int dst) {
//        std::vector<int> path;
//        int cur = head;
//        int guard = 0;
//        while (cur != -1 && guard++ < eCnt_ + 5) {
//            const Edge& e = edges_[cur];
//                    if (e.alive) {
//                        path.push_back(e.start);
//                        if (e.end == dst) {
//                            path.push_back(dst);
//                            break;
//                        }
//                    }
//                    cur = e.next;
//        }
//        return path;
//    }
//
//    std::vector<int> quickHull(const std::vector<Point>& pts) {
//        int n = pts.size();
//        if (n == 0) return {};
//        if (n == 1) return {0};
//        int L = 0, R = 0;
//        for (int i = 1; i < n; ++i) {
//            if (pts[i].x < pts[L].x || (pts[i].x == pts[L].x && pts[i].y < pts[L].y)) L = i;
//            if (pts[i].x > pts[R].x || (pts[i].x == pts[R].x && pts[i].y > pts[R].y)) R = i;
//        }
//        if (L == R) return {L};
//        bool allCol = true;
//        for (int i = 0; i < n && allCol; ++i)
//            if (i != L && i != R && std::fabs(cross(pts[L], pts[R], pts[i])) > 1e-12)
//                allCol = false;
//        if (allCol) return {L, R};
//
//        eCap_ = std::max(1, 3 + 2 * (n - 3));
//        edges_.assign(eCap_, Edge{});
//        eCnt_ = 0;
//
//        int upperHead = buildChain(L, R);
//        int lowerHead = buildChain(R, L);
//
//        auto upper = collectChain(upperHead, R);
//        auto lower = collectChain(lowerHead, L);
//
//        std::vector<int> res = upper;
//        // `upper` ends with R; `lower` starts with R and ends with L.
//        // Skip the first element of lower (R) to avoid duplicate.
//        for (size_t i = 1; i + 1 < lower.size(); ++i) res.push_back(lower[i]);
//        return res;
//    }
//
//    static void normalize(const std::vector<Point>& pts, std::vector<int>& hull) {
//        if (hull.size() < 3) {
//            if (hull.size() == 2) {
//                const Point& a = pts[hull[0]]; const Point& b = pts[hull[1]];
//                if (a.x > b.x || (a.x == b.x && a.y > b.y)) std::swap(hull[0], hull[1]);
//            }
//            return;
//        }
//        double area2 = 0;
//        for (size_t i = 0; i < hull.size(); ++i) {
//            const Point& a = pts[hull[i]];
//            const Point& b = pts[hull[(i + 1) % hull.size()]];
//            area2 += a.x * b.y - b.x * a.y;
//        }
//        if (area2 < 0) std::reverse(hull.begin(), hull.end());
//        size_t best = 0;
//        for (size_t i = 1; i < hull.size(); ++i) {
//            const Point& a = pts[hull[i]]; const Point& b = pts[hull[best]];
//            if (a.x < b.x || (a.x == b.x && a.y < b.y)) best = i;
//        }
//        std::rotate(hull.begin(), hull.begin() + best, hull.end());
//    }
//};

#pragma once
#include "solution.hpp"
#include <vector>
#include <string>
#include <sstream>
#include <iostream>
#include <iomanip>
#include <cmath>
#include <algorithm>
#include <memory>

struct Point {
    double x, y;
    int id;
};

class QuickHullImp : public Solution {
public:
    void readInput(const std::string& data) override {
        std::istringstream in(data);
        int n = 0;
        in >> n;
        pts_.assign(n, {});
        for (int i = 0; i < n; ++i) {
            in >> pts_[i].x >> pts_[i].y;
            pts_[i].id = i;
        }
        hull_.clear();
    }

    void solve() override {
        hull_ = quickHull(pts_);
        normalize(pts_, hull_);
    }

    bool compareExpected(const std::string& expected) override {
        std::istringstream in(expected);
        int n = 0;
        if (!(in >> n)) {
            std::cerr << "compareExpected: cannot parse expected N\n";
            return false;
        }
        std::vector<Point> exp(n);
        for (int i = 0; i < n; ++i) {
            if (!(in >> exp[i].x >> exp[i].y)) {
                std::cerr << "compareExpected: cannot parse expected point #" << i << "\n";
                return false;
            }
        }
        if (n != (int)hull_.size()) {
            std::cerr << "compareExpected: size mismatch: expected " << n
                      << ", got " << hull_.size() << "\n";
            return false;
        }
        std::vector<Point> got(hull_.size());
        for (size_t i = 0; i < hull_.size(); ++i) got[i] = pts_[hull_[i]];
        auto key = [](const Point& p){ return std::make_pair(p.x, p.y); };
        auto cmp = [&](const Point& a, const Point& b){ return key(a) < key(b); };
        std::sort(exp.begin(), exp.end(), cmp);
        std::sort(got.begin(), got.end(), cmp);
        const double EPS = 1e-6;
        for (int i = 0; i < n; ++i) {
            if (std::fabs(exp[i].x - got[i].x) > EPS ||
                std::fabs(exp[i].y - got[i].y) > EPS) {
                std::cerr << std::fixed << std::setprecision(6)
                          << "compareExpected: point #" << i
                          << " expected (" << exp[i].x << ", " << exp[i].y << ")"
                          << " got ("      << got[i].x << ", " << got[i].y << ")\n";
                return false;
            }
        }
        return true;
    }

private:
    std::vector<Point> pts_;
    std::vector<int> hull_;

    static double cross(const Point& a, const Point& b, const Point& c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    // Ребро хранит (start, end) и срез в общем пуле конфликтов.
    // csize == 0  ->  конфликтов нет, coff не используется.
    struct Edge {
        int start  = -1;
        int end    = -1;
        int next   = -1;
        int coff   = 0;     // смещение в conflictPool_
        int csize  = 0;     // сколько int'ов в срезе
        bool alive = true;
    };

    // Ровно столько, сколько сказано: 3 + 2*(n-3) для n>=3.
    // Для n<3 quickHull выходит раньше и этот массив не используется.
    std::unique_ptr<Edge[]> edges_;
    int eCap_ = 0, eCnt_ = 0;

    // Один общий пул конфликтов на весь алгоритм — одна аллокация.
    std::vector<int> conflictPool_;

    int newEdge(int a, int b) {
        int id = eCnt_++;
        Edge& e = edges_[id];
        e.start = a; e.end = b;
        e.next = -1; e.coff = 0; e.csize = 0; e.alive = true;
        return id;
    }

    // Положить конфликтные точки [first, last) в пул и привязать к ребру.
    template <class It>
    void setConflict(int eid, It first, It last) {
        Edge& e = edges_[eid];
        e.coff  = (int)conflictPool_.size();
        e.csize = (int)(last - first);
        conflictPool_.insert(conflictPool_.end(), first, last);
    }

    void clearConflict(int eid) {
        edges_[eid].csize = 0;
    }

    // Итераторы по срезу конфликтов ребра — без копирования.
    const int* confBegin(int eid) const { return conflictPool_.data() + edges_[eid].coff; }
    const int* confEnd  (int eid) const { return conflictPool_.data() + edges_[eid].coff + edges_[eid].csize; }

    int buildChain(int L, int R) {
        int head = newEdge(L, R);

        // начальный конфликт head: все точки по внешнюю сторону L->R
        // кладём прямо в пул, минуя временный вектор
        {
            size_t base = conflictPool_.size();
            for (int i = 0; i < (int)pts_.size(); ++i) {
                if (i == L || i == R) continue;
                if (cross(pts_[L], pts_[R], pts_[i]) > 1e-12)
                    conflictPool_.push_back(i);
            }
            Edge& e = edges_[head];
            e.coff  = (int)base;
            e.csize = (int)(conflictPool_.size() - base);
        }

        std::vector<int> stack;
        stack.reserve(64);
        stack.push_back(head);

        while (!stack.empty()) {
            int eid = stack.back(); stack.pop_back();
            if (!edges_[eid].alive) continue;
            Edge& e = edges_[eid];
            if (e.csize == 0) continue;

            // найти самую дальнюю точку конфликта
            int best = -1; double bestD = 0;
            for (int k = 0; k < e.csize; ++k) {
                int pi = conflictPool_[e.coff + k];
                double d = cross(pts_[e.start], pts_[e.end], pts_[pi]);
                if (d > bestD) { bestD = d; best = pi; }
            }
            if (best < 0) { e.csize = 0; continue; }

            int P = best;

            // снять копию conflict-среза ДО того, как начнём писать в пул,
            // потому что insert может реаллоцировать буфер и инвалидировать указатели
            // Вместо копии — пройдём по индексам e.coff..e.coff+e.csize,
            // которые остаются валидными как смещения (пул только растёт).
            int oldCoff = e.coff, oldCsize = e.csize;
            int A = e.start, B = e.end;

            int e1 = newEdge(A, P);
            int e2 = newEdge(P, B);
            edges_[e1].next = e2;
            edges_[e2].next = e.next;
            e.next  = e1;    // мёртвый родитель остаётся хопом к живой под-цепочке
            e.alive = false;

            // два новых среза кладём в пул
            size_t base1 = conflictPool_.size();
            for (int k = 0; k < oldCsize; ++k) {
                int pi = conflictPool_[oldCoff + k];
                if (pi == P) continue;
                if (cross(pts_[A], pts_[P], pts_[pi]) > 1e-12)
                    conflictPool_.push_back(pi);
            }
            edges_[e1].coff  = (int)base1;
            edges_[e1].csize = (int)(conflictPool_.size() - base1);

            size_t base2 = conflictPool_.size();
            for (int k = 0; k < oldCsize; ++k) {
                int pi = conflictPool_[oldCoff + k];
                if (pi == P) continue;
                if (cross(pts_[P], pts_[B], pts_[pi]) > 1e-12)
                    conflictPool_.push_back(pi);
            }
            edges_[e2].coff  = (int)base2;
            edges_[e2].csize = (int)(conflictPool_.size() - base2);

            stack.push_back(e1);
            stack.push_back(e2);
        }
        return head;
    }

    std::vector<int> collectChain(int head, int dst) {
        std::vector<int> path;
        path.reserve(8);
        int cur = head;
        int guard = 0;
        while (cur != -1 && guard++ < eCnt_ + 10) {
            const Edge& e = edges_[cur];
            if (e.alive) {
                path.push_back(e.start);
                if (e.end == dst) { path.push_back(dst); break; }
            }
            cur = e.next;
        }
        return path;
    }

    std::vector<int> quickHull(const std::vector<Point>& pts) {
        int n = (int)pts.size();
        if (n == 0) return {};
        if (n == 1) return {0};
        int L = 0, R = 0;
        for (int i = 1; i < n; ++i) {
            if (pts[i].x < pts[L].x || (pts[i].x == pts[L].x && pts[i].y < pts[L].y)) L = i;
            if (pts[i].x > pts[R].x || (pts[i].x == pts[R].x && pts[i].y > pts[R].y)) R = i;
        }
        if (L == R) return {L};
        bool allCol = true;
        for (int i = 0; i < n && allCol; ++i)
            if (i != L && i != R && std::fabs(cross(pts[L], pts[R], pts[i])) > 1e-12)
                allCol = false;
        if (allCol) return {L, R};

        eCap_ = 3 + 2 * (n - 3);
        edges_.reset(new Edge[eCap_]);
        eCnt_ = 0;
        conflictPool_.clear();
        // грубая, но безопасная оценка сверху: N точек * (число шагов)
        // на практике сильно меньше; reserve не влияет на корректность.
        conflictPool_.reserve((size_t)n * 4);

        int upperHead = buildChain(L, R);
        int lowerHead = buildChain(R, L);

        auto upper = collectChain(upperHead, R);
        auto lower = collectChain(lowerHead, L);

        std::vector<int> res = upper;          // L ... R
        for (size_t i = 1; i + 1 < lower.size(); ++i)
            res.push_back(lower[i]);           // без первого (R) и последнего (L)
        return res;
    }

    static void normalize(const std::vector<Point>& pts, std::vector<int>& hull) {
        if (hull.size() < 3) {
            if (hull.size() == 2) {
                const Point& a = pts[hull[0]]; const Point& b = pts[hull[1]];
                if (a.x > b.x || (a.x == b.x && a.y > b.y)) std::swap(hull[0], hull[1]);
            }
            return;
        }
        double area2 = 0;
        for (size_t i = 0; i < hull.size(); ++i) {
            const Point& a = pts[hull[i]];
            const Point& b = pts[hull[(i + 1) % hull.size()]];
            area2 += a.x * b.y - b.x * a.y;
        }
        if (area2 < 0) std::reverse(hull.begin(), hull.end());
        size_t best = 0;
        for (size_t i = 1; i < hull.size(); ++i) {
            const Point& a = pts[hull[i]]; const Point& b = pts[hull[best]];
            if (a.x < b.x || (a.x == b.x && a.y < b.y)) best = i;
        }
        std::rotate(hull.begin(), hull.begin() + best, hull.end());
    }
};