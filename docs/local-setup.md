# Local Setup

## Prerequisites

- Java 21
- Maven 3.9+
- Docker + Docker Compose

## Start infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL (port 5432), Redis (port 6379), and Kafka (port 9092).

A minimal `docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: notifications
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  redis:
    image: redis:7
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on: [zookeeper]
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

## Build and run

```bash
./mvnw clean package -DskipTests
java -jar microservice/target/microservice-*.jar
```

## Required environment variables

Copy and fill in the values below before running:

```bash
export SENDGRID_API_KEY=your-key
export TWILIO_ACCOUNT_SID=your-sid
export TWILIO_AUTH_TOKEN=your-token
export FCM_SERVER_KEY=your-key
```

All other variables have local defaults (see [configuration.md](configuration.md)).

## Run tests

```bash
./mvnw test                      # unit + BDD tests
./mvnw test -pl usecase          # BDD tests only
```

## Send a test event

```bash
# Produce a test message to the notifications topic
kafka-console-producer \
  --broker-list localhost:9092 \
  --topic notifications <<'EOF'
{
  "eventType": "ORDER_CONFIRMED",
  "idempotencyKey": "test-001",
  "locale": "en",
  "recipient": { "email": "test@example.com" },
  "templateData": { "orderId": "ORD-001", "amount": "100", "currency": "USD" }
}
EOF
```
