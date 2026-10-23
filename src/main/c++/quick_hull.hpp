#pragma once
#include "solution.hpp"
#include <vector>
#include <string>
#include <sstream>
#include <iostream>
#include <iomanip>
#include <cmath>
#include <algorithm>

struct Pt {
    double x, y;
    int id;
};

class QuickHull : public Solution {
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
        // Парсим expected: N, затем N пар (x, y). Порядок/старт могут отличаться —
        // сравниваем как мультимножество с допуском.
        std::istringstream in(expected);
        int n = 0;
        if (!(in >> n)) {
            std::cerr << "compareExpected: cannot parse expected N\n";
            return false;
        }
        std::vector<Pt> exp(n);
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
        // Сортируем обе последовательности лексикографически и сравниваем с EPS.
        std::vector<Pt> got(hull_.size());
        for (size_t i = 0; i < hull_.size(); ++i) got[i] = pts_[hull_[i]];
        auto key = [](const Pt& p){ return std::make_pair(p.x, p.y); };
        auto cmp = [&](const Pt& a, const Pt& b){ return key(a) < key(b); };
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
    std::vector<Pt> pts_;
    std::vector<int> hull_;

    // ----- сам алгоритм (тот же, что в прошлом ответе) -----
    static double cross(const Pt& a, const Pt& b, const Pt& c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }
    struct Edge {
        int a, b;
        std::vector<int> conflict;
        bool alive = true;
    };
    std::vector<Edge> edges_;
    int eCap_ = 0, eCnt_ = 0;

    int newEdge(int a, int b) {
        int id = eCnt_++;
        edges_[id] = Edge{};
        edges_[id].a = a; edges_[id].b = b;
        return id;
    }

    void buildChain(int L, int R) {
        int start = newEdge(L, R);
        for (int i = 0; i < (int)pts_.size(); ++i) {
            if (i == L || i == R) continue;
            if (cross(pts_[L], pts_[R], pts_[i]) > 1e-12)
                edges_[start].conflict.push_back(i);
        }
        std::vector<int> stack = {start};
        while (!stack.empty()) {
            int eid = stack.back(); stack.pop_back();
            if (!edges_[eid].alive) continue;
            auto& e = edges_[eid];
            if (e.conflict.empty()) continue;
            int best = -1; double bestD = 0;
            for (int pi : e.conflict) {
                double d = cross(pts_[e.a], pts_[e.b], pts_[pi]);
                if (d > bestD) { bestD = d; best = pi; }
            }
            if (best < 0) { e.conflict.clear(); continue; }
            int P = best;
            int e1 = newEdge(e.a, P);
            int e2 = newEdge(P, e.b);
            e.alive = false;
            for (int pi : e.conflict) {
                if (pi == P) continue;
                if (cross(pts_[e.a], pts_[P], pts_[pi]) > 1e-12)
                    edges_[e1].conflict.push_back(pi);
                else if (cross(pts_[P], pts_[e.b], pts_[pi]) > 1e-12)
                    edges_[e2].conflict.push_back(pi);
            }
            e.conflict.clear();
            stack.push_back(e1);
            stack.push_back(e2);
        }
    }

    std::vector<int> collectChain(int src, int dst) {
        std::vector<int> path{src};
        int cur = src, guard = 0;
        while (cur != dst && guard++ < (int)edges_.size() + 5) {
            int found = -1;
            for (int i = 0; i < eCnt_; ++i)
                if (edges_[i].alive && edges_[i].a == cur) { found = i; break; }
            if (found < 0) break;
            cur = edges_[found].b;
            path.push_back(cur);
        }
        return path;
    }

    std::vector<int> quickHull(const std::vector<Pt>& pts) {
        int n = pts.size();
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

        eCap_ = std::max(1, 3 + 2 * (n - 3));
        edges_.assign(eCap_, Edge{});
        eCnt_ = 0;

        buildChain(L, R);
        buildChain(R, L);
        auto upper = collectChain(L, R);
        auto lower = collectChain(R, L);
        std::vector<int> res = upper;
        for (size_t i = 1; i + 1 < lower.size(); ++i) res.push_back(lower[i]);
        return res;
    }

    static void normalize(const std::vector<Pt>& pts, std::vector<int>& hull) {
        if (hull.size() < 3) {
            if (hull.size() == 2) {
                const Pt& a = pts[hull[0]]; const Pt& b = pts[hull[1]];
                if (a.x > b.x || (a.x == b.x && a.y > b.y)) std::swap(hull[0], hull[1]);
            }
            return;
        }
        double area2 = 0;
        for (size_t i = 0; i < hull.size(); ++i) {
            const Pt& a = pts[hull[i]];
            const Pt& b = pts[hull[(i + 1) % hull.size()]];
            area2 += a.x * b.y - b.x * a.y;
        }
        if (area2 < 0) std::reverse(hull.begin(), hull.end());
        size_t best = 0;
        for (size_t i = 1; i < hull.size(); ++i) {
            const Pt& a = pts[hull[i]]; const Pt& b = pts[hull[best]];
            if (a.x < b.x || (a.x == b.x && a.y < b.y)) best = i;
        }
        std::rotate(hull.begin(), hull.begin() + best, hull.end());
    }
};
