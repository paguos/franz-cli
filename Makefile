.PHONY: help build test run clean native install

# Default target
help:
	@echo "Franz CLI - Available targets:"
	@echo "  make build        - Compile the project"
	@echo "  make test         - Run all unit tests"
	@echo "  make run          - Run the CLI (pass args with ARGS='...')"
	@echo "  make native       - Build native binary with GraalVM"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make install      - Install dependencies via asdf"
	@echo ""
	@echo "Examples:"
	@echo "  make run ARGS='--help'"
	@echo "  make run ARGS='get topic'"
	@echo "  make run ARGS='describe cluster --health'"

# Compile the project
build:
	./gradlew compileKotlin

# Run all tests
test:
	./gradlew test

# Run the CLI with arguments
# Usage: make run ARGS='get topic'
run:
	./gradlew run --args="$(ARGS)" -q

# Build native binary
native:
	./gradlew nativeCompile

# Run native binary with arguments
# Usage: make native-run ARGS='get topic'
native-run:
	@if [ ! -f build/native/nativeCompile/franz ]; then \
		echo "Native binary not found. Run 'make native' first."; \
		exit 1; \
	fi
	./build/native/nativeCompile/franz $(ARGS)

# Clean build artifacts
clean:
	./gradlew clean

# Install dependencies via asdf
install:
	asdf install
