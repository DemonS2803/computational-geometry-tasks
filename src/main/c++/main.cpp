#include "solution.hpp"
#include "quick_hull.hpp"
#include "quick_hull_imp.hpp"
#include <fstream>
#include <iostream>
#include <sstream>
#include <memory>
#include <string>

static std::string readFile(const std::string& path) {
    std::ifstream f(path);
    if (!f) { std::cerr << "cannot open " << path << "\n"; std::exit(2); }
    std::ostringstream ss; ss << f.rdbuf();
    return ss.str();
}

int main(int argc, char** argv) {
    if (argc < 4) {
        std::cerr << "usage: " << argv[0] << " <solution> <input> <expected>\n"
                  << "  solutions: quick_hull\n";
        return 2;
    }
    std::string name = argv[1];
    std::string inputPath = argv[2];
    std::string expectedPath = argv[3];

    std::unique_ptr<Solution> sol;
    if (name == "quick_hull") sol = std::make_unique<QuickHull>();
    else if (name == "quick_hull_imp") sol = std::make_unique<QuickHullImp>();
    else { std::cerr << "unknown solution: " << name << "\n"; return 2; }

    sol->readInput(readFile(inputPath));
    sol->solve();
    bool ok = sol->compareExpected(readFile(expectedPath));

    std::cout << (ok ? "OK" : "FAIL") << " " << name << "\n";
    return ok ? 0 : 1;
}
