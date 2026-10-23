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

# ---------- Тесты ----------
TEST_ROOT  := tests
OUT_DIR    := build/out

TASKS ?= quick_hull
# автовариант (если хочешь подхватывать каталоги автоматически):
# TASKS ?= $(notdir $(patsubst %/,%,$(wildcard $(TEST_ROOT)/*/)))

task_tests = $(sort $(wildcard $(TEST_ROOT)/$(1)/input*.txt))

.PHONY: all cpp java test clean help \
        $(foreach t,$(TASKS),cpp-$(t) java-$(t) test-$(t))

all: cpp java

build:
	mkdir -p build $(OUT_DIR)

# ============================ C++ ====================================
cpp: $(CXX_BIN)

$(CXX_BIN): $(CXX_SRC) $(CXX_HDRS) | build
	$(CXX) $(CXXFLAGS) -I $(CXX_INC) $(CXX_SRC) -o $@

# ============================ Java ===================================
java:
	$(MVN) -q -DskipTests compile

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

$(foreach t,$(TASKS),test-$(t)): test-%: cpp-% java-%
	@:

test: all $(foreach t,$(TASKS),test-$(t))

# ============================ Прочее =================================
help:
	@echo "Цели:"
	@echo "  make all                       — собрать cpp + java"
	@echo "  make cpp | make java           — собрать только один язык"
	@echo "  make test                      — все тесты (оба языка, все задачи)"
	@echo "  make test-<task>               — тесты задачи на обоих языках"
	@echo "  make cpp-<task> | java-<task>  — тесты задачи на одном языке"
	@echo ""
	@echo "Задачи (TASKS): $(TASKS)"

clean:
	rm -rf build target
