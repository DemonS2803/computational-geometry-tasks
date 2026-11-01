#include <iostream>
#include <vector>
#include <queue>
#include <cassert>

using namespace std;

struct Point {
    long long x, y;
};

long long cross(const Point& a, const Point& b, const Point& c) {
    return (b.x - a.x) * (c.y - a.y)
         - (b.y - a.y) * (c.x - a.x);
}

// Вершины a, b, c перечислены против часовой стрелки.
// Граница треугольника также считается его частью.
bool insideTriangle(const Point& p,
                    const Point& a,
                    const Point& b,
                    const Point& c) {
    return cross(a, b, p) >= 0
        && cross(b, c, p) >= 0
        && cross(c, a, p) >= 0;
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;

    vector<Point> p(n);
    for (int i = 0; i < n; ++i) {
        cin >> p[i].x >> p[i].y;
    }

    long long area2 = 0;
    for (int i = 0; i < n; ++i) {
        int j = (i + 1) % n;
        area2 += p[i].x * p[j].y - p[i].y * p[j].x;
    }

    vector<int> prv(n), nxt(n);

    // Связный список всегда ориентируем против часовой стрелки.
    // Исходные номера вершин сохраняются.
    for (int i = 0; i < n; ++i) {
        if (area2 > 0) {
            prv[i] = (i + n - 1) % n;
            nxt[i] = (i + 1) % n;
        } else {
            prv[i] = (i + 1) % n;
            nxt[i] = (i + n - 1) % n;
        }
    }

    vector<char> alive(n, true);
    vector<char> convex(n, false);
    vector<char> inQueue(n, false);
    vector<int> cnt(n, 0);

    queue<int> ears;

    auto addToQueue = [&](int v) {
        if (alive[v] && convex[v] && cnt[v] == 0 && !inQueue[v]) {
            ears.push(v);
            inQueue[v] = true;
        }
    };

    auto recalculate = [&](int v) {
        int a = prv[v];
        int b = nxt[v];

        convex[v] = cross(p[a], p[v], p[b]) > 0;
        cnt[v] = 0;

        if (convex[v]) {
            for (int u = 0; u < n; ++u) {
                if (!alive[u] || u == a || u == v || u == b) {
                    continue;
                }

                if (insideTriangle(p[u], p[a], p[v], p[b])) {
                    ++cnt[v];
                }
            }
        }

        addToQueue(v);
    };

    for (int v = 0; v < n; ++v) {
        recalculate(v);
    }

    int remaining = n;

    while (remaining > 3) {
        int v = -1;

        // Записи в очереди могут устареть после изменения соседей.
        while (!ears.empty()) {
            int u = ears.front();
            ears.pop();
            inQueue[u] = false;

            if (alive[u] && convex[u] && cnt[u] == 0) {
                v = u;
                break;
            }
        }

        // У простого невырожденного многоугольника существует ухо.
        assert(v != -1);

        int a = prv[v];
        int b = nxt[v];

        cout << a << ' ' << b << '\n';

        // У всех вершин, кроме a и b, соседние вершины не меняются.
        // Из их треугольников нужно лишь убрать удаляемую точку v.
        for (int u = 0; u < n; ++u) {
            if (!alive[u] || u == v || u == a || u == b || !convex[u]) {
                continue;
            }

            if (insideTriangle(p[v], p[prv[u]], p[u], p[nxt[u]])) {
                --cnt[u];
                addToQueue(u);
            }
        }

        alive[v] = false;
        nxt[a] = b;
        prv[b] = a;
        --remaining;

        recalculate(a);
        recalculate(b);
    }

    return 0;
}