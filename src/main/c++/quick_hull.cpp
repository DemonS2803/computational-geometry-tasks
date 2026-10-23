// quickhull.cpp
#include <bits/stdc++.h>
#include <fstream>
using namespace std;

struct Pt {
    double x, y;
    int id;                    // исходный индекс (для тестов)
};
static inline double cross(const Pt& a, const Pt& b, const Pt& c) {
    // (b-a) x (c-a)
    return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
}
const double EPS = 1e-12;

struct Edge {
    int a, b;              // start_point, end_point (индексы в pts)
    int next = -1, prev = -1;
    vector<int> conflict;  // индексы точек справа от ребра a->b
    bool alive = true;
};

struct QuickHull {
    vector<Pt> pts;
    vector<Edge> edges;
    int eCap = 0, eCnt = 0;

    QuickHull(vector<Pt> p) : pts(move(p)) {
        int n = pts.size();
        eCap = max(1, 3 + 2 * (n - 3));
        edges.resize(eCap);
    }

    int newEdge(int a, int b) {
        assert(eCnt < eCap);
        int id = eCnt++;
        edges[id] = Edge{};
        edges[id].a = a; edges[id].b = b;
        return id;
    }

    // Строим одну цепочку от L к R (CCW-ребро L->R, точки сверху = слева?
    // У нас: ребро a->b, «снаружи» = cross(a,b,p) > 0 (p справа от a->b в CCW смысле).
    // Для верхней цепочки L->R: точки сверху дают cross(L,R,p) > 0?
    // cross((R-L),(p-L)) = (R.x-L.x)(p.y-L.y)-(R.y-L.y)(p.x-L.x).
    // Для L=(-1,0), R=(1,0), p=(0,1): (2)(1)-0 = 2 > 0. Да, верх -> положительный cross.
    void buildChain(int L, int R, vector<int>& hullOut) {
        int start = newEdge(L, R);
        // собрать конфликты
        for (int i = 0; i < (int)pts.size(); ++i) {
            if (i == L || i == R) continue;
            if (cross(pts[L], pts[R], pts[i]) > EPS)
                edges[start].conflict.push_back(i);
        }
        // рекурсивный/итеративный разбор
        // Обрабатываем ребро start и все порождённые.
        // Используем стек рёбер для обработки.
        vector<int> stack = {start};
        while (!stack.empty()) {
            int eid = stack.back(); stack.pop_back();
            if (!edges[eid].alive) continue;
            auto& e = edges[eid];
            if (e.conflict.empty()) continue;

            // самая дальняя точка
            int best = -1; double bestD = 0;
            for (int pi : e.conflict) {
                double d = cross(pts[e.a], pts[e.b], pts[pi]);
                if (d > bestD) { bestD = d; best = pi; }
            }
            if (best < 0) { e.conflict.clear(); continue; }

            int P = best;
            // новые рёбра A->P, P->B
            int e1 = newEdge(e.a, P);
            int e2 = newEdge(P, e.b);
            // e «умирает»; связи next/prev переносим
            // В нашей модели рёбра не связаны в двусвязный список явно при построении цепочки;
            // свяжем при финальной сборке. Здесь просто гасим.
            e.alive = false;

            // распределяем конфликты старого ребра
            for (int pi : e.conflict) {
                if (pi == P) continue;
                if (cross(pts[e.a], pts[P], pts[pi]) > EPS)
                    edges[e1].conflict.push_back(pi);
                else if (cross(pts[P], pts[e.b], pts[pi]) > EPS)
                    edges[e2].conflict.push_back(pi);
                // иначе точка внутри нового треугольника — отбрасываем
            }
            e.conflict.clear();
            e.conflict.shrink_to_fit();

            stack.push_back(e1);
            stack.push_back(e2);
        }
    }

    // Возвращает индексы вершин оболочки в CCW порядке.
    vector<int> solve() {
        int n = pts.size();
        if (n == 0) return {};
        if (n == 1) return {0};
        // найти L (min x), R (max x)
        int L = 0, R = 0;
        for (int i = 1; i < n; ++i) {
            if (pts[i].x < pts[L].x || (pts[i].x == pts[L].x && pts[i].y < pts[L].y)) L = i;
            if (pts[i].x > pts[R].x || (pts[i].x == pts[R].x && pts[i].y > pts[R].y)) R = i;
        }
        if (L == R) return {L};

        // проверим коллинеарность всех точек
        bool allCol = true;
        for (int i = 0; i < n && allCol; ++i)
            if (i != L && i != R && fabs(cross(pts[L], pts[R], pts[i])) > EPS)
                allCol = false;
        if (allCol) {
            // оболочка = две крайние точки (отрезок)
            return {L, R};
        }

        // верхняя цепочка L->R (точки с cross>0)
        vector<int> upper, lower;
        buildChain(L, R, upper);
        buildChain(R, L, lower);

        // Собрать цепочки в обход.
        // Верхняя цепочка: рёбра L->R, «живые»; идём от L по рёбрам:
        vector<int> upperVerts = collectChain(L, R);
        vector<int> lowerVerts = collectChain(R, L);

        // Итог CCW: L ... (верх) ... R ... (низ) ... обратно к L
        vector<int> res;
        for (int v : upperVerts) res.push_back(v);
        // lowerVerts начинается с R, заканчивается L — пропустим первый R и последний L
        for (size_t i = 1; i + 1 < lowerVerts.size(); ++i) res.push_back(lowerVerts[i]);
        return res;
    }

    // пройти по живым рёбрам от src к dst
    vector<int> collectChain(int src, int dst) {
        vector<int> path;
        int cur = src;
        path.push_back(cur);
        int guard = 0;
        while (cur != dst && guard++ < (int)edges.size() + 5) {
            // найти живое ребро с a == cur
            int found = -1;
            for (int i = 0; i < eCnt; ++i) {
                if (edges[i].alive && edges[i].a == cur) { found = i; break; }
            }
            if (found < 0) break;
            cur = edges[found].b;
            path.push_back(cur);
        }
        return path;
    }
};

static double polyArea2(const std::vector<Pt>& p, const std::vector<int>& h) {
    double s = 0;
    for (size_t i = 0; i < h.size(); ++i) {
        const Pt& a = p[h[i]];
        const Pt& b = p[h[(i + 1) % h.size()]];
        s += a.x * b.y - b.x * a.y;
    }
    return s;
}
static void normalize(std::vector<Pt>& p, std::vector<int>& hull) {
    if (hull.size() < 3) return;
    if (polyArea2(p, hull) < 0) std::reverse(hull.begin(), hull.end());
    // старт с лексикографически минимальной
    size_t best = 0;
    for (size_t i = 1; i < hull.size(); ++i) {
        const Pt& a = p[hull[i]];
        const Pt& b = p[hull[best]];
        if (a.x < b.x || (a.x == b.x && a.y < b.y)) best = i;
    }
    std::rotate(hull.begin(), hull.begin() + best, hull.end());
}

int main(int argc, char** argv) {
    if (argc < 2) {
        std::cerr << "usage: " << argv[0] << " <input_file>\n";
        return 2;
    }
    std::ifstream in(argv[1]);
    if (!in) { std::cerr << "cannot open " << argv[1] << "\n"; return 2; }

    int n;
    if (!(in >> n)) return 0;
    std::vector<Pt> p(n);
    for (int i = 0; i < n; ++i) { in >> p[i].x >> p[i].y; p[i].id = i; }

    QuickHull qh(p);
    auto hull = qh.solve();

   normalize(p, hull);

    std::cout << hull.size() << "\n";
    for (int v : hull)
        std::cout << std::fixed << std::setprecision(6) << p[v].x << " " << p[v].y << "\n";
    return 0;
}
