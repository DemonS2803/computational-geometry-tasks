# =====================================================================
#  Расширяемая сборка/тесты: языки (cpp/java) x задачи (tests/<task>)
# =====================================================================

# make-рецепты используют bash-синтаксис (${var#prefix} и т.п.)
SHELL      := /bin/bash

MVN        ?= mvn
JAVA       ?= /usr/lib/jvm/java-21-openjdk/bin/java
CXX        ?= g++
CXXFLAGS   ?= -O2 -std=c++17 -Wall -Wextra

# ---------- C++ ----------
CXX_INC    := src/main/c++
CXX_SRC    := $(CXX_INC)/main.cpp
CXX_HDRS   := $(wildcard $(CXX_INC)/*.hpp)
CXX_BIN    := build/runner_cpp

# ---------- Java ----------
JAVA_CP    := target/classes
JAVA_MAIN  := ru.yandex.practicum.Main

# ---------- Python ----------
PYTHON     ?= python3
PY_DIR     := src/main/python
PY_MAIN    := $(PY_DIR)/main.py
PY_SRCS    := $(wildcard $(PY_DIR)/*.py)

# ---------- Тесты ----------
TEST_ROOT  := tests
OUT_DIR    := build/out

# ---------- Генератор тестов ----------
PYTHON     ?= python3
GEN        := tools/gen_tests.py
GEN_MODE   ?= mixed
GEN_SIZES  ?= 10,100,1000,1000,1000,100000,1000000,10000000,100000000,1000000000
GEN_SEED   ?= 42

TASKS ?= quick_hull
# автовариант (если хочешь подхватывать каталоги автоматически):
# TASKS ?= $(notdir $(patsubst %/,%,$(wildcard $(TEST_ROOT)/*/)))

task_tests = $(sort $(wildcard $(TEST_ROOT)/$(1)/input*.txt))

.PHONY: all cpp java py test clean help \
        $(foreach t,$(TASKS),cpp-$(t) java-$(t) py-$(t) test-$(t)) \
        get-tests get-tests-clean

all: cpp java py

build:
	mkdir -p build $(OUT_DIR)

# ============================ C++ ====================================
cpp: $(CXX_BIN)

$(CXX_BIN): $(CXX_SRC) $(CXX_HDRS) | build
	$(CXX) $(CXXFLAGS) -I $(CXX_INC) $(CXX_SRC) -o $@

# ============================ Java ===================================
java:
	$(MVN) -q -DskipTests compile

# ============================ Python =================================
py: build
	@$(PYTHON) -m py_compile $(PY_SRCS) && echo "python: syntax ok"

# ============================ Тесты ==================================
# $(1) = язык (cpp|java), $(2) = задача

define run_task
	@mkdir -p $(OUT_DIR); \
	lang=$(1); task=$(2); sol=$(2); fail=0; total=0; \
	for inp in $(call task_tests,$(2)); do \
	    base=$$(basename $$inp .txt); \
	    num=$$(echo "$$base" | sed 's/^input//'); \
	    exp=$(TEST_ROOT)/$$task/expected$${num}.txt; \
	    total=$$((total+1)); \
	    perf_cmd=""; \
	    case "$$lang" in \
	      cpp)  cmd="./$(CXX_BIN) $$sol $$inp $$exp"; \
	            perf_cmd="perf stat -e L1-dcache-load-misses,L1-dcache-loads,LLC-load-misses,LLC-loads,cache-misses,cache-references -o $(OUT_DIR)/$${task}_$${base}_$${lang}.perf.txt ./$(CXX_BIN) $$sol $$inp $$exp";; \
	      java) cmd="$(JAVA) -cp $(JAVA_CP) $(JAVA_MAIN) $$sol $$inp $$exp";; \
				perf_cmd="perf stat -e cache-misses,cache-references -o $(OUT_DIR)/$${task}_$${base}_$${lang}.perf.txt \
					$(JAVA) -cp $(JAVA_CP) $(JAVA_MAIN) $$sol $$inp $$exp"; \
	      py)   cmd="PYTHONDONTWRITEBYTECODE=1 $(PYTHON) -B $(PY_MAIN) $$sol $$inp $$exp";; \
	      		perf stat -e cache-references,cache-misses,L1-dcache-loads,L1-dcache-load-misses,LLC-loads,LLC-load-misses \
                    python3 -B src/main/python/main.py quick_hull tests/quick_hull/input1.txt tests/quick_hull/expected1.txt
	      *)    echo "unknown lang: $$lang"; exit 1;; \
	    esac; \
	    printf '%-6s %-16s %-12s : ' "$$lang" "$$task" "$$base"; \
	    if $$cmd > $(OUT_DIR)/$${task}_$${base}_$${lang}.out 2> $(OUT_DIR)/$${task}_$${base}_$${lang}.err; then \
	        cat $(OUT_DIR)/$${task}_$${base}_$${lang}.out; \
	    else \
	        echo "FAIL"; cat $(OUT_DIR)/$${task}_$${base}_$${lang}.err; fail=$$((fail+1)); \
	    fi; \
	    if [ -n "$$perf_cmd" ]; then \
	        if $$perf_cmd > /dev/null 2> $(OUT_DIR)/$${task}_$${base}_$${lang}.perf.err; then \
	            { echo; echo "--- perf ---"; cat $(OUT_DIR)/$${task}_$${base}_$${lang}.perf.txt; } >> $(OUT_DIR)/$${task}_$${base}_$${lang}.out; \
	        else \
	            echo "  (perf skipped: $$(head -1 $(OUT_DIR)/$${task}_$${base}$${lang}.perf.err))"; \
	        fi; \
	    fi; \
	done; \
	echo "--- $$lang/$$task ($$sol): $$total tests, $$fail failures"; \
	test $$fail -eq 0
endef

define run_task
	@mkdir -p $(OUT_DIR); \
	lang=$(1); task=$(2); sol=$(2); fail=0; total=0; \
	for inp in $(call task_tests,$(2)); do \
	    base=$$(basename $$inp .txt); \
	    num=$$(echo "$$base" | sed 's/^input//'); \
	    exp=$(TEST_ROOT)/$$task/expected$${num}.txt; \
	    total=$$((total+1)); \
	    if [ "$$lang" = "cpp" ]; then \
	        cmd="./$(CXX_BIN) $$sol $$inp $$exp"; \
	        perf_cmd="perf stat -e L1-dcache-load-misses,L1-dcache-loads,LLC-load-misses,LLC-loads,cache-misses,cache-references \
				 -o $(OUT_DIR)/$${task}_$${base}_$${lang}.perf.txt ./$(CXX_BIN) $$sol $$inp $$exp"; \
	    else \
	        cmd="$(JAVA) -cp $(JAVA_CP) $(JAVA_MAIN) $$sol $$inp $$exp"; \
	        perf_cmd="perf stat -e cache-misses,cache-references -o $(OUT_DIR)/$${task}_$${base}_$${lang}.perf.txt \
				$(JAVA) -cp $(JAVA_CP) $(JAVA_MAIN) $$sol $$inp $$exp"; \
	    fi; \
	    printf '%-6s %-16s %-12s : ' "$$lang" "$$task" "$$base"; \
	    if $$cmd > $(OUT_DIR)/$${task}_$${base}_$${lang}.out 2> $(OUT_DIR)/$${task}_$${base}_$${lang}.err; then \
	        cat $(OUT_DIR)/$${task}_$${base}_$${lang}.out; \
	    else \
	        echo "FAIL"; cat $(OUT_DIR)/$${task}_$${base}_$${lang}.err; fail=$$((fail+1)); \
	    fi; \
	    if [ -n "$$perf_cmd" ]; then \
	        if $$perf_cmd > /dev/null 2> $(OUT_DIR)/$${task}_$${base}_$${lang}.perf.err; then \
	            { echo; echo "--- perf ---"; cat $(OUT_DIR)/$${task}_$${base}_$${lang}.perf.txt; } >> $(OUT_DIR)/$${task}_$${base}_$${lang}.out; \
	        else \
	            echo "  (perf skipped: $$(head -1 $(OUT_DIR)/$${task}_$${base}_$${lang}.perf.err))"; \
	        fi; \
	    fi; \
	done; \
	echo "--- $$lang/$$task ($$sol): $$total tests, $$fail failures"; \
	test $$fail -eq 0
endef

$(foreach t,$(TASKS),cpp-$(t)): cpp-%: cpp
	$(call run_task,cpp,$*)

$(foreach t,$(TASKS),java-$(t)): java-%: java
	$(call run_task,java,$*)

$(foreach t,$(TASKS),py-$(t)): py-%: py
	$(call run_task,py,$*)

$(foreach t,$(TASKS),test-$(t)): test-%: cpp-% java-% py-%
	@:

test: all $(foreach t,$(TASKS),test-$(t))

# Сгенерировать тесты в каталог текущей задачи.
# Использование:
#   make gen-tests                         # defaults: mixed, 10/100/1000
#   make gen-tests TASK=quick_hull GEN_MODE=circle GEN_SIZES=100:10000:1000
#   make gen-tests TASK=quick_hull GEN_SIZES=10,100,1000 GEN_SEED=7
gen-tests:
	@task="$(if $(TASK),$(TASK),$(firstword $(TASKS)))"; \
	if [ -z "$$task" ]; then echo "no TASK/TASKS set"; exit 1; fi; \
	$(PYTHON) $(GEN) \
	    --out $(TEST_ROOT)/$$task \
	    --mode $(GEN_MODE) \
	    --sizes $(GEN_SIZES) \
	    --seed $(GEN_SEED); \
	echo "task=$$task, tests in $(TEST_ROOT)/$$task"

# Удалить сгенерированные пары в каталоге задачи (осторожно: удаляет ВСЕ input*/expected*).
gen-tests-clean:
	@task="$(if $(TASK),$(TASK),$(firstword $(TASKS)))"; \
	if [ -z "$$task" ]; then echo "no TASK/TASKS set"; exit 1; fi; \
	rm -f $(TEST_ROOT)/$$task/input*.txt $(TEST_ROOT)/$$task/expected*.txt; \
	echo "cleaned $(TEST_ROOT)/$$task"

# Перегенерировать (со страховкой от затирания)
gen-tests-refresh: gen-tests-clean gen-tests

# ============================ Прочее =================================
help:
	@echo "Цели:"
	@echo "  make all | make cpp | make java | make py  — сборка/проверка"
	@echo "  make test                                  — все тесты (cpp+java+py)"
	@echo "  make test-<task>                           — тесты задачи на всех языках"
	@echo "  make cpp-<task> | java-<task> | py-<task>  — тесты задачи на одном языке"
	@echo ""
	@echo "Задачи (TASKS): $(TASKS)"

clean:
	rm -rf build target
	find $(PY_DIR) -name '__pycache__' -type d -prune -exec rm -rf {} + 2>/dev/null || true
