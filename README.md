# Franz CLI

A modern CLI tool for managing Apache Kafka clusters, inspired by kubectl's user experience.

Franz provides a simple, intuitive interface for common Kafka operations with a kubeconfig-style configuration system that makes working with multiple clusters effortless.

## Features

- **Kubectl-like interface**: Familiar command structure (`franz get topic`, `franz describe cluster`)
- **Context-based configuration**: Easily switch between multiple Kafka clusters
- **Comprehensive resource management**: Topics, brokers, consumer groups, ACLs
- **Native binary support**: Fast startup with GraalVM native image
- **Secure authentication**: Support for SASL (PLAIN, SCRAM, Kerberos, OAuth) and SSL/TLS

## Installation

### Prerequisites

- Java 21 (GraalVM Community recommended for native builds)
- Docker (optional, for local Kafka cluster)

### From Source

```bash
git clone https://github.com/yourusername/franz-cli.git
cd franz-cli
make build
```

### Native Binary

Build a native executable with GraalVM for fast startup:

```bash
make native
# Binary will be at: build/native/nativeCompile/franz
```

## Quick Start

### 1. Start a Local Kafka Cluster

```bash
make kafka-up
```

This starts a Kafka broker on `localhost:9092` using Docker.

### 2. Configure Franz

```bash
# Set up a cluster
franz config set-cluster local --bootstrap-servers localhost:9092

# Create a context
franz config set-context local --cluster local

# Use the context
franz config use-context local
```

### 3. Interact with Kafka

```bash
# List topics
franz get topic

# Create a topic
franz create topic my-topic --partitions 3 --replication-factor 1

# Describe a topic
franz describe topic my-topic

# List consumer groups
franz get group

# Check cluster health
franz describe cluster --health

# List brokers
franz get broker
```

## Configuration

Franz stores configuration at `~/.franz/config` in YAML format, similar to kubectl's kubeconfig.

### Configuration Structure

```yaml
apiVersion: v1
current-context: production

contexts:
  - name: local
    cluster: local-cluster
  - name: production
    cluster: prod-cluster
    auth: prod-auth

clusters:
  - name: local-cluster
    bootstrap-servers: localhost:9092
  - name: prod-cluster
    bootstrap-servers: kafka-1.example.com:9092,kafka-2.example.com:9092

auth-configs:
  - name: prod-auth
    security-protocol: SASL_SSL
    sasl:
      mechanism: SCRAM-SHA-256
      username: admin
      password-file: ~/.franz/passwords/prod
    ssl:
      truststore-location: /path/to/truststore.jks
      truststore-password: changeit

# PEM SSL (cafile/clientfile/clientkeyfile)
  - name: arn-ssl
    security-protocol: SSL
    ssl:
      cafile: /Users/kdzd/.kaf/arn.ca.crt
      clientfile: /Users/kdzd/.kaf/arn.kaf-user.user.crt
      clientkeyfile: /Users/kdzd/.kaf/arn.kaf-user.user.key

# Advanced Kafka client overrides (escape hatch)
  - name: arn-ssl-advanced
    security-protocol: SSL
    ssl:
      cafile: /Users/kdzd/.kaf/arn.ca.crt
      clientfile: /Users/kdzd/.kaf/arn.kaf-user.user.crt
      clientkeyfile: /Users/kdzd/.kaf/arn.kaf-user.user.key
    kafka-properties:
      ssl.endpoint.identification.algorithm: ""
```

### Configuration Commands

```bash
# View current configuration
franz config view

# List all contexts
franz config get-contexts

# Switch context
franz config use-context production

# Get current context
franz config current-context

# Set cluster
franz config set-cluster staging \
  --bootstrap-servers kafka.staging.example.com:9092

# Set credentials (SASL)
franz config set-credentials prod-sasl \
  --security-protocol SASL_SSL \
  --sasl-mechanism SCRAM-SHA-256 \
  --sasl-username admin \
  --sasl-password-file ~/.franz/passwords/prod

# Set context
franz config set-context staging \
  --cluster staging \
  --auth staging-auth

# Delete context
franz config delete-context old-context
```

### Override Context

Use a different context for a single command:

```bash
franz get topic --context production
```

## Usage Examples

### Topics

```bash
# List all topics
franz get topic

# Filter topics by pattern
franz get topic payments-*

# Include internal topics
franz get topic --show-internal

# Describe a topic
franz describe topic my-topic

# Create a topic
franz create topic orders \
  --partitions 12 \
  --replication-factor 3

# Delete a topic
franz delete topic old-topic --force
```

### Consumer Groups

```bash
# List active consumer groups
franz get group

# Show empty groups too
franz get group --show-empty

# Describe a consumer group
franz describe group my-consumer-group
```

### Brokers

```bash
# List brokers
franz get broker

# Describe a specific broker
franz describe broker 1
```

### Cluster

```bash
# Show cluster information
franz describe cluster

# Check cluster health
franz describe cluster --health
```

### ACLs

```bash
# List all ACLs
franz get acl

# Filter by principal
franz get acl --principal User:alice

# Filter by resource
franz get acl --resource-type topic --resource-name payments
```

## Authentication

Franz supports multiple authentication mechanisms:

### SASL/PLAIN

```bash
franz config set-credentials my-plain \
  --security-protocol SASL_PLAINTEXT \
  --sasl-mechanism PLAIN \
  --sasl-username user \
  --sasl-password-file ~/.franz/passwords/plain
```

### SASL/SCRAM

```bash
franz config set-credentials my-scram \
  --security-protocol SASL_SSL \
  --sasl-mechanism SCRAM-SHA-256 \
  --sasl-username admin \
  --sasl-password-file ~/.franz/passwords/scram
```

### SSL/TLS (Mutual TLS)

```bash
franz config set-credentials my-mtls \
  --security-protocol SSL \
  --ssl-truststore-location /path/to/truststore.jks \
  --ssl-truststore-password changeit \
  --ssl-keystore-location /path/to/keystore.jks \
  --ssl-keystore-password changeit \
  --ssl-key-password changeit
```

## Development

### Build

```bash
make build
```

### Run Tests

```bash
# All tests
make test

# Unit tests only
make test-unit

# Integration tests only
make test-integration
```

### Run Locally

```bash
make run ARGS='get topic'
```

### Project Structure

```
src/main/kotlin/dev/franz/cli/
├── Franz.kt                    # Main CLI entry point
├── commands/                   # Command implementations
│   ├── Config.kt              # Config command
│   ├── Get.kt                 # Get command
│   ├── Describe.kt            # Describe command
│   ├── Create.kt              # Create command
│   ├── Delete.kt              # Delete command
│   ├── config/                # Config subcommands
│   └── resources/             # Resource commands (Topic, Broker, etc.)
├── config/                    # Configuration management
│   ├── ConfigManager.kt       # Config file handling
│   ├── CredentialResolver.kt  # Credential resolution
│   └── KafkaPropertiesBuilder.kt
├── kafka/                     # Kafka integration layer
│   ├── KafkaService.kt       # Main service facade
│   └── repository/           # Repository pattern for Kafka resources
└── Main.kt                    # Entry point
```

See [CLAUDE.md](CLAUDE.md) for detailed architectural documentation.

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Submit a pull request

## License

[Add your license here]

## Acknowledgments

- Built with [Clikt](https://ajalt.github.io/clikt/) for CLI parsing
- Uses Apache Kafka's [AdminClient](https://kafka.apache.org/documentation/#adminapi) for cluster operations
- Inspired by [kubectl](https://kubernetes.io/docs/reference/kubectl/) for UX design
