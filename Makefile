.PHONY: help build test test-unit test-integration run clean native install kafka-up kafka-down kafka-logs

# Optional flags:
# - RERUN=1: force Gradle to re-run tasks (including tests) even if up-to-date
# - NO_CACHE=1: disable Gradle build cache (prevents FROM-CACHE results)
GRADLEW ?= ./gradlew
GRADLE_FLAGS :=
ifeq ($(RERUN),1)
GRADLE_FLAGS += --rerun-tasks
endif
ifeq ($(NO_CACHE),1)
GRADLE_FLAGS += --no-build-cache
endif

# Default target
help:
	@echo "Franz CLI - Available targets:"
	@echo "  make build            - Compile the project"
	@echo "  make test             - Run all tests"
	@echo "  make test-unit        - Run unit tests only"
	@echo "  make test-integration - Run integration tests only"
	@echo "  make run              - Run the CLI (pass args with ARGS='...')"
	@echo "  make native           - Build native binary with GraalVM"
	@echo "  make clean            - Clean build artifacts"
	@echo "  make install          - Install dependencies via asdf"
	@echo ""
	@echo "Docker/Kafka:"
	@echo "  make kafka-up         - Start local Kafka cluster"
	@echo "  make kafka-down       - Stop local Kafka cluster"
	@echo "  make kafka-logs       - View Kafka logs"
	@echo ""
	@echo "Examples:"
	@echo "  make run ARGS='--help'"
	@echo "  make run ARGS='get topic'"
	@echo "  make run ARGS='describe cluster --health'"

# Compile the project
build:
	$(GRADLEW) $(GRADLE_FLAGS) compileKotlin

# Run all tests
test:
	$(GRADLEW) $(GRADLE_FLAGS) test

# Run the CLI with arguments
# Usage: make run ARGS='get topic'
run:
	$(GRADLEW) run --args="$(ARGS)" -q

# Build native binary
native:
	$(GRADLEW) $(GRADLE_FLAGS) nativeCompile

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
	$(GRADLEW) $(GRADLE_FLAGS) clean

# Install dependencies via asdf
install:
	asdf install

# Start local Kafka cluster
kafka-up:
	docker compose up -d
	@echo "Waiting for Kafka to be ready..."
	@sleep 5
	@echo "Kafka is ready at localhost:9092"

# Stop local Kafka cluster
kafka-down:
	docker compose down -v

# View Kafka logs
kafka-logs:
	docker compose logs -f kafka

# Run unit tests only
test-unit:
	$(GRADLEW) $(GRADLE_FLAGS) test --tests '*Test' -PexcludeTags=integration

# Run integration tests only
test-integration:
	$(GRADLEW) $(GRADLE_FLAGS) test --tests '*IT' -PincludeTags=integration
