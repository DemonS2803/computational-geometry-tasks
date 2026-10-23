MVN        ?= mvn
JAVA       ?= /usr/lib/jvm/java-21-openjdk/bin/java
CXX        ?= g++
CXXFLAGS   ?= -O2 -std=c++17 -Wall -Wextra

CXX_INC    := src/main/c++
CXX_SRC    := $(CXX_INC)/main.cpp
CXX_BIN    := build/runner_cpp

JAVA_CP    := target/classes
JAVA_MAIN  := ru.yandex.practicum.Main

TEST_DIR   := tests/2d-convex-hull
OUT_DIR    := build/out
TESTS      := $(sort $(wildcard $(TEST_DIR)/input*.txt))

.PHONY: all cpp java test clean

all: cpp java

build:
	mkdir -p build

cpp: $(CXX_BIN)
$(CXX_BIN): $(CXX_SRC) $(CXX_INC)/solution.hpp $(CXX_INC)/quick_hull.hpp | build
	$(CXX) $(CXXFLAGS) -I $(CXX_INC) $(CXX_SRC) -o $@

java:
	$(MVN) -q -DskipTests compile

test: all
	@mkdir -p $(OUT_DIR); \
	fail=0; total=0; \
	for inp in $(TESTS); do \
	    base=$$(basename $$inp .txt); \
	    num=$${base#input}; \
	    exp=$(TEST_DIR)/expected$${num}.txt; \
	    total=$$((total+1)); \
	    printf '=== %-10s cpp : ' $$base; \
	    if ./$(CXX_BIN) quick_hull $$inp $$exp > $(OUT_DIR)/$$base.cpp.out 2> $(OUT_DIR)/$$base.cpp.err; then \
	        cat $(OUT_DIR)/$$base.cpp.out; \
	    else \
	        cat $(OUT_DIR)/$$base.cpp.out; cat $(OUT_DIR)/$$base.cpp.err; fail=$$((fail+1)); \
	    fi; \
	    printf '=== %-10s java: ' $$base; \
	    if $(JAVA) -cp $(JAVA_CP) $(JAVA_MAIN) quick_hull $$inp $$exp > $(OUT_DIR)/$$base.java.out 2> $(OUT_DIR)/$$base.java.err; then \
	        cat $(OUT_DIR)/$$base.java.out; \
	    else \
	        cat $(OUT_DIR)/$$base.java.out; cat $(OUT_DIR)/$$base.java.err; fail=$$((fail+1)); \
	    fi; \
	done; \
	echo "--- tests: $$total, failures: $$fail"; \
	test $$fail -eq 0

clean:
	rm -rf build target
