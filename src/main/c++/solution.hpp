#pragma once
#include <string>

// Контракт решения. Наследники реализуют парсинг входа, решение и сравнение
// с ожидаемым выводом (нормализация, допуски, порядок — на усмотрение наследника).
class Solution {
public:
    virtual ~Solution() = default;

    // data — полное содержимое входного файла.
    virtual void readInput(const std::string& data) = 0;

    // Выполнить решение. Результат хранится внутри наследника.
    virtual void solve() = 0;

    // expected — полное содержимое expected-файла.
    // true  — совпало;
    // false — не совпало (наследник печатает диагностику в std::cerr).
    virtual bool compareExpected(const std::string& expected) = 0;
};
