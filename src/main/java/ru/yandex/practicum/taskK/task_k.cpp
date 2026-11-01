#include <iostream>
#include <vector>
#include <algorithm>
#include <string>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <cstdio>

using uint64 = std::uint64_t;
using int64  = std::int64_t;

// ── BigInt: base 10, 80 decimal digits ────────────────────────────
class BigInt {
    static constexpr int ND = 80;
    uint8_t d[ND]; // little-endian, each 0-9

    BigInt() { memset(d, 0, ND); }

public:
    explicit BigInt(uint64_t v) {
        memset(d, 0, ND);
        int i = 0;
        while (v > 0 && i < ND) {
            d[i++] = (uint8_t)(v % 10);
            v /= 10;
        }
    }

    // Multiply by 10 in place
    void mul10() {
        uint8_t carry = 0;
        for (int i = 0; i < ND; i++) {
            uint16_t t = d[i] * 10 + carry;
            d[i] = (uint8_t)(t % 10);
            carry = (uint8_t)(t / 10);
        }
    }

    // Shift left by n positions (multiply by 10^n)
    BigInt shl(int n) const {
        BigInt r;
        for (int i = 0; i < ND - n; i++)
            r.d[i + n] = d[i];
        return r;
    }

    bool lt(const BigInt& o) const {
        for (int i = ND - 1; i >= 0; i--)
            if (d[i] != o.d[i]) return d[i] < o.d[i];
        return false;
    }
    int cmp(const BigInt& o) const {
        for (int i = ND - 1; i >= 0; i--)
            if (d[i] != o.d[i]) return d[i] > o.d[i] ? 1 : -1;
        return 0;
    }
    bool ge(const BigInt& o) const { return cmp(o) >= 0; }

    bool isZero() const { for (int i = 0; i < ND; i++) if (d[i]) return false; return true; }

    // Subtract: this - other (caller must ensure this >= other)
    BigInt sub(const BigInt& o) const {
        BigInt r;
        int borrow = 0;
        for (int i = 0; i < ND; i++) {
            int t = d[i] - o.d[i] - borrow;
            r.d[i] = (uint8_t)(t < 0 ? t + 10 : t);
            borrow = t < 0 ? 1 : 0;
        }
        return r;
    }

    // Multiply by small integer (0-9)
    static BigInt mul_small(const BigInt& o, uint8_t q) {
        if (q == 0) return BigInt();
        BigInt r;
        uint64_t carry = 0;
        for (int i = 0; i < ND; i++) {
            uint64_t t = (uint64_t)o.d[i] * q + carry;
            r.d[i] = (uint8_t)(t % 10);
            carry = t / 10;
        }
        return r;
    }

    // Addition
    BigInt add(const BigInt& o) const {
        BigInt r;
        uint8_t carry = 0;
        for (int i = 0; i < ND; i++) {
            uint16_t t = d[i] + o.d[i] + carry;
            r.d[i] = (uint8_t)(t % 10);
            carry = (uint8_t)(t / 10);
        }
        return r;
    }

    // Divide by 2: floor(n / 2)
    BigInt div2() const {
        BigInt r;
        int carry = 0;
        for (int i = ND - 1; i >= 0; i--) {
            int t = d[i] + carry * 10;
            r.d[i] = (uint8_t)(t / 2);
            carry = t % 2;
        }
        return r;
    }

    // Division: this / other (schoolbook long division, base 10)
    BigInt div(const BigInt& other) const {
        if (other.isZero() || this->isZero()) return BigInt();

        BigInt quotient, remainder = *this;

        int this_hi = 79;
        while (this_hi > 0 && d[this_hi] == 0) this_hi--;
        int other_hi = 79;
        while (other_hi > 0 && other.d[other_hi] == 0) other_hi--;

        int shift = this_hi - other_hi;
        if (shift < 0) return BigInt();

        for (int i = shift; i >= 0; i--) {
            uint8_t q = 0;
            // Estimate q using top two digits of remainder / top digit of other
            uint64_t partial = (uint64_t)remainder.d[i + other_hi] * 10
                             + (i + other_hi > 0 ? (uint64_t)remainder.d[i + other_hi - 1] : 0);
            q = (uint8_t)(partial / (other.d[other_hi] + 1));
            if (q > 9) q = 9;

            // Adjust q down until q * other * 10^i <= remainder
            while (q > 0) {
                BigInt product = mul_small(other.shl(i), q);
                if (remainder.ge(product)) {
                    break;
                }
                q--;
            }

            if (q > 0) {
                remainder = remainder.sub(mul_small(other.shl(i), q));
                quotient.d[i] = q;
            }
        }

        return quotient;
    }

    // Integer square root via Newton's method: floor(sqrt(n))
    BigInt sqrt() const {
        if (isZero()) return BigInt(0);

        // Find highest non-zero digit position (decimal digit count)
        int highest = 0;
        for (int i = ND - 1; i >= 0; i--) {
            if (d[i]) {
                highest = i + 1;
                break;
            }
        }

        // Initial guess: 10^(highest/2)
        BigInt x = BigInt::from_shift(highest / 2 + 1);

        for (int iter = 0; iter < 100; iter++) {
            BigInt nx = this->div(x);
            fprintf(stderr, "  iter %d: x=%s, nx=%s\n", iter, x.str().c_str(), nx.str().c_str());
            BigInt next = x.add(nx).div2();
            fprintf(stderr, "  iter %d: next=%s\n", iter, next.str().c_str());
            if (next.ge(x)) {
                fprintf(stderr, "  converging at iter %d, returning %s\n", iter, x.str().c_str());
                return x;
            }
            x = next;
        }
    }

    // Helper: construct 10^n
    static BigInt from_shift(int n) {
        BigInt r;
        if (n >= 0 && n < ND) r.d[n] = 1;
        return r;
    }

    // Convert to decimal string
    std::string str() const {
        int hi = 79;
        while (hi > 0 && d[hi] == 0) hi--;
        if (hi < 0) return "0";

        std::string result;
        char buf[12];
        snprintf(buf, sizeof(buf), "%u", d[hi]);
        result += buf;
        for (int i = hi - 1; i >= 0; i--) {
            snprintf(buf, sizeof(buf), "%01u", d[i]);
            result += buf;
        }
        return result;
    }
};

// ── Point struct ────────────────────────────────────────────────────
struct Point {
    int64_t x, y;
};

// ── 64-bit integer sqrt fallback ───────────────────────────────────
static uint64_t isqrt64(uint64_t n) {
    if (n == 0) return 0;
    uint64_t r = (uint64_t)std::sqrt((double)n);
    while (r * r > n) r--;
    while ((r + 1) * (r + 1) <= n) r++;
    return r;
}

// ── Format distance: integer sq → "X.XXXXX...X" (15 decimal places) ─
static std::string formatDistance(BigInt sq) {
    std::string s = sq.str();
    // Insert decimal point 15 digits from the right
    if ((int)s.size() <= 15) {
        s = "0." + std::string(15 - (int)s.size(), '0') + s;
    } else {
        s.insert(s.size() - 15, 1, '.');
    }
    return s;
}

// ── Cross product (signed area * 2) for points A, B, C ──────────────
static int64_t crossProduct(const Point& A, const Point& B, const Point& C) {
    return (B.x - A.x) * (C.y - A.y) - (B.y - A.y) * (C.x - A.x);
}

// ── Euclidean distance squared (64-bit) ─────────────────────────────
static uint64_t squaredDistance(const Point& a, const Point& b) {
    int64_t dx = a.x - b.x;
    int64_t dy = a.y - b.y;
    return (uint64_t)dx * dx + (uint64_t)dy * dy;
}

// Comparator for sorting points
static bool pointCompare(const Point& a, const Point& b) {
    if (a.x != b.x) return a.x < b.x;
    return a.y < b.y;
}

// ── Main ────────────────────────────────────────────────────────────
int main() {
    int n;
    if (scanf("%d", &n) != 1) return 1;

    std::vector<Point> points(n);
    for (int i = 0; i < n; i++) {
        scanf("%lld %lld", &points[i].x, &points[i].y);
    }

    if (n <= 2) {
        uint64_t sq = squaredDistance(points[0], points[n - 1]);
        BigInt big(0);
        for (int i = 0; i < 30; i++) big.mul10();
        // Multiply sq by 10^30
        for (int i = 0; i < 30; i++) big.mul10();
        // Wait, this is wrong. Let me redo.
        BigInt bigSq(sq);
        for (int i = 0; i < 30; i++) bigSq.mul10();
        BigInt root = bigSq.sqrt();
        printf("%s\n", formatDistance(root).c_str());
        return 0;
    }

    // Sort points
    std::sort(points.begin(), points.end(), pointCompare);

    // Convex hull using monotone chain algorithm
    std::vector<Point> lower, upper;
    for (int i = 0; i < n; i++) {
        while (lower.size() >= 2 && crossProduct(lower[lower.size() - 2], lower.back(), points[i]) <= 0) {
            lower.pop_back();
        }
        lower.push_back(points[i]);
    }
    for (int i = n - 1; i >= 0; i--) {
        while (upper.size() >= 2 && crossProduct(upper[upper.size() - 2], upper.back(), points[i]) <= 0) {
            upper.pop_back();
        }
        upper.push_back(points[i]);
    }

    // Concatenate: lower[:-1] + upper[:-1]
    lower.pop_back();
    upper.pop_back();
    lower.insert(lower.end(), upper.begin(), upper.end());

    // Find diameter (convex hull diameter can be found by rotating calipers,
    // but for simplicity we'll check all pairs)
    uint64_t maxSq = 0;
    int m = lower.size();
    fprintf(stderr, "Hull size: %d\n", m);
    for (int k = 0; k < m; k++) {
        fprintf(stderr, "  hull[%d] = (%lld, %lld)\n", k, (long long)lower[k].x, (long long)lower[k].y);
    }
    for (int i = 0; i < m; i++) {
        for (int j = i + 1; j < m; j++) {
            uint64_t sq = squaredDistance(lower[i], lower[j]);
            fprintf(stderr, "  dist^2(%d,%d) = %llu\n", i, j, (unsigned long long)sq);
            if (sq > maxSq) maxSq = sq;
        }
    }
    fprintf(stderr, "maxSq = %llu\n", (unsigned long long)maxSq);

    // Format: scale sq by 10^30, take sqrt, insert decimal point at 15th digit
    if (maxSq == 0) {
        printf("0.000000000000000\n");
    } else {
        BigInt bigSq(maxSq);
        for (int i = 0; i < 30; i++) bigSq.mul10();
        BigInt root = bigSq.sqrt();
        printf("%s\n", formatDistance(root).c_str());
    }

    return 0;
}
