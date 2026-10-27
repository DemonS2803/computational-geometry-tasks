#include <bits/stdc++.h>
#include "seg_intersection_tests.h"

using namespace std;

struct Point {
    long double x, y;
    Point() : x(0), y(0) {}
    Point(long double x_, long double y_) : x(x_), y(y_) {}
};

static inline long double T(const Point& a, const Point& b, const Point& c) {
    return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
}

// is C on AB line
static bool isOnLine(const Point& a, const Point& b, const Point& c) {
    return min(a.x, b.x) <= c.x && c.x <= max(a.x, b.x) &&
           min(a.y, b.y) <= c.y && c.y <= max(a.y, b.y);
}

static bool isLinesIntersect(const Point& a, const Point& b,
                              const Point& c, const Point& d) {
    const long double EPS = 1e-12L;

    long double rotateAtoCD = T(c, d, a);
    long double rotateBtoCD = T(c, d, b);
    long double rotateCtoAB = T(a, b, c);
    long double rotateDtoAB = T(a, b, d);

    auto sign = [&](long double v) -> int {
        if (v >  EPS) return  1;
        if (v < -EPS) return -1;
        return 0;
    };

    int s1 = sign(rotateAtoCD);
    int s2 = sign(rotateBtoCD);
    int s3 = sign(rotateCtoAB);
    int s4 = sign(rotateDtoAB);

    if (
        ((rotateAtoCD > 0 && rotateBtoCD < 0) || (rotateAtoCD < 0 && rotateBtoCD > 0)) &&
        ((rotateCtoAB > 0 && rotateDtoAB < 0) || (rotateCtoAB < 0 && rotateDtoAB > 0))
    ) return true;

//    if (s1 * s2 < 0 && s3 * s4 < 0) return true;

    if (s1 == 0 && isOnLine(c, d, a)) return true;
    if (s2 == 0 && isOnLine(c, d, b)) return true;
    if (s3 == 0 && isOnLine(a, b, c)) return true;
    if (s4 == 0 && isOnLine(a, b, d)) return true;

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
        Point a1(inp[i+0], inp[i+1]);
        Point a2(inp[i+2], inp[i+3]);
        Point a3(inp[i+4], inp[i+5]);
        Point a4(inp[i+6], inp[i+7]);

        out.push_back(isLinesIntersect(a1, a2, a3, a4) ? 'Y' : 'N');
    }
    out.push_back('\n');
    cout << out;
    return 0;
}
