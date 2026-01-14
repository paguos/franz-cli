# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Franz CLI is a Kafka management tool built with Kotlin that provides a kubectl-like interface for Apache Kafka. It uses a kubeconfig-style configuration system with contexts, clusters, and authentication profiles stored at `~/.franz/config`.

## Build & Development

### Prerequisites
- JDK 21 (GraalVM Community recommended for native builds)
- Use `make install` to install dependencies via asdf

### Common Commands

```bash
# Build
make build                  # Compile Kotlin source
./gradlew compileKotlin    # Direct Gradle build

# Testing
make test                   # Run all tests
make test-unit             # Run unit tests only (excludes integration tag)
make test-integration      # Run integration tests only (includes integration tag)
./gradlew test -PexcludeTags=integration  # Custom test filtering
./gradlew test -PincludeTags=integration  # Run specific tags

# Running
make run ARGS='get topic'                 # Run via Gradle
make native                               # Build native binary with GraalVM
make native-run ARGS='get topic'          # Run native binary

# Local Kafka cluster
make kafka-up              # Start Kafka at localhost:9092
make kafka-down            # Stop Kafka cluster
make kafka-logs            # View Kafka logs

# Gradle flags
RERUN=1 make test          # Force re-run even if up-to-date
NO_CACHE=1 make test       # Disable build cache
```

## Architecture

### Command Structure

Franz uses Clikt for CLI structure with a hierarchical command pattern:
- **Root**: `Franz` class in Franz.kt - configures KafkaService, handles `--context` and `--mock` flags
- **Verb commands**: `Get`, `Describe`, `Create`, `Delete`, `Config` in commands/
- **Resource commands**: Implementations in commands/resources/ (Topic.kt, Broker.kt, Group.kt, Acl.kt, Cluster.kt)
- **Config subcommands**: Implementations in commands/config/ (SetContext.kt, UseContext.kt, etc.)

Each command follows the pattern: `franz <verb> <resource> [args] [flags]`

### Configuration System

**Location**: `~/.franz/config` (YAML format)

**Components** (see config/model/ConfigModels.kt):
- **Contexts**: Named combinations of cluster + optional auth config
- **Clusters**: Kafka bootstrap servers
- **Auth Configs**: Security protocol (PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL) + credentials

**Key classes**:
- `ConfigManager`: Loads/saves config, resolves context by name or current-context
- `CredentialResolver`: Reads passwords from files referenced in config
- `KafkaPropertiesBuilder`: Converts resolved context to Kafka client Properties
- `ResolvedContext`: Flattened context with all connection details

### Kafka Integration Layer

**KafkaService** (kafka/KafkaService.kt):
- Singleton facade providing access to all repositories
- Can be configured with real Kafka AdminClient or mock implementations
- Configured via `configureFromContext()` or `configureMock()`

**Repository Pattern** (kafka/repository/):
- Interface-based: TopicRepository, BrokerRepository, GroupRepository, AclRepository, ClusterRepository
- Real implementations in kafka/: Use Kafka AdminClient
- Mock implementations in mock/: Return sample data for `--mock` flag
- Commands access via `KafkaService.getInstance().topics`, `.brokers`, etc.

**Models** (kafka/model/Models.kt):
- Domain objects: Topic, Broker, Group, Acl, etc.
- Independent of Kafka client types

### Testing Strategy

**Unit Tests** (*Test.kt):
- Use mock repositories via `KafkaService.setInstance()`
- No external dependencies
- Default when running `make test-unit`

**Integration Tests** (*IT.kt):
- Tagged with `@Tag("integration")`
- Extend `KafkaTestBase` which provides Testcontainers Kafka instance
- Use real Kafka AdminClient against containerized Kafka
- Run with `make test-integration`

**Test configuration** (build.gradle.kts):
- JUnit Platform with tag filtering via `-PincludeTags` / `-PexcludeTags`
- Testcontainers for Kafka (Confluent CP 7.6.0)

## Key Patterns

### Adding a new resource type

1. Define model in kafka/model/Models.kt
2. Create repository interface in kafka/repository/
3. Implement Kafka version in kafka/repository/kafka/
4. Implement mock version in kafka/repository/mock/
5. Add repository to KafkaService constructor and factory methods
6. Create command classes in commands/resources/ (GetX, DescribeX, CreateX, DeleteX)
7. Wire commands into verb commands (Get, Describe, Create, Delete)

### Adding configuration options

1. Update models in config/model/ConfigModels.kt with @Serializable and @SerialName
2. Add handling in CredentialResolver if reading from files
3. Update KafkaPropertiesBuilder to convert to Kafka client properties
4. Add config subcommands in commands/config/ if user-facing

### Native Image (GraalVM)

- Main class: dev.franz.cli.MainKt
- Binary name: franz
- Build flags: --no-fallback, -H:+ReportExceptionStackTraces
- Run `make native` to build

## Dependencies

- **Clikt 4.2.2**: CLI framework
- **Kafka Clients 4.1.0**: AdminClient for Kafka operations
- **kotlinx.serialization + kaml**: YAML config parsing
- **JUnit + MockK + AssertJ**: Testing
- **Testcontainers**: Integration tests with real Kafka
