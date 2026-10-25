#include <bits/stdc++.h>
#include "seg_intersection_tests.h"

using namespace std;

struct Pt {
    long double x, y;
    Pt() : x(0), y(0) {}
    Pt(long double x_, long double y_) : x(x_), y(y_) {}
};

static inline long double cross(const Pt& a, const Pt& b, const Pt& c) {
    // (b-a) x (c-a)
    return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
}

static inline long double dist2(const Pt& a, const Pt& b) {
    long double dx = a.x - b.x, dy = a.y - b.y;
    return dx*dx + dy*dy;
}

// лежит ли c на отрезке ab (при условии коллинеарности)
static bool onSegment(const Pt& a, const Pt& b, const Pt& c) {
    return min(a.x, b.x) <= c.x && c.x <= max(a.x, b.x) &&
           min(a.y, b.y) <= c.y && c.y <= max(a.y, b.y);
}

// s1 = (p1,p2), s2 = (p3,p4)
static bool segmentsIntersect(const Pt& p1, const Pt& p2,
                              const Pt& p3, const Pt& p4) {
    const long double EPS = 1e-12L;

    long double d1 = cross(p3, p4, p1);
    long double d2 = cross(p3, p4, p2);
    long double d3 = cross(p1, p2, p3);
    long double d4 = cross(p1, p2, p4);

    auto sign = [&](long double v) -> int {
        if (v >  EPS) return  1;
        if (v < -EPS) return -1;
        return 0;
    };

    int s1 = sign(d1), s2 = sign(d2), s3 = sign(d3), s4 = sign(d4);

    // общий случай: строгое пересечение
    if (s1 * s2 < 0 && s3 * s4 < 0) return true;

    // вырожденные случаи (касание / коллинеарность)
    if (s1 == 0 && onSegment(p3, p4, p1)) return true;
    if (s2 == 0 && onSegment(p3, p4, p2)) return true;
    if (s3 == 0 && onSegment(p1, p2, p3)) return true;
    if (s4 == 0 && onSegment(p1, p2, p4)) return true;

    return false;
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int t;
    if (!(cin >> t)) return 0;
    auto inp = genTest(t);

    string out;
    out.reserve(inp.size() / 8);

    for (size_t i = 0; i + 7 < inp.size(); i += 8) {
        Pt a1(inp[i+0], inp[i+1]);
        Pt a2(inp[i+2], inp[i+3]);
        Pt a3(inp[i+4], inp[i+5]);
        Pt a4(inp[i+6], inp[i+7]);

        out.push_back(segmentsIntersect(a1, a2, a3, a4) ? 'Y' : 'N');
    }
    out.push_back('\n');
    cout << out;
    return 0;
}
