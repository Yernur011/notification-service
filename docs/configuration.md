# Configuration

All configuration lives in `microservice/src/main/resources/application.yaml`. Sensitive values are injected via environment variables.

## Full reference

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: notification-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.notification.*"

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/notifications}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

notification:
  kafka:
    topic: notifications
    dlq-topic: notifications-dlq

  deduplication:
    ttl-seconds: 86400   # 24 hours

  channels:
    email:
      provider: sendgrid
      api-key: ${SENDGRID_API_KEY}
    sms:
      provider: twilio
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
    push:
      fcm-server-key: ${FCM_SERVER_KEY}
```

## Environment variables

| Variable                  | Default                                      | Description                     |
|---------------------------|----------------------------------------------|---------------------------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`                             | Kafka broker address            |
| `DB_URL`                  | `jdbc:postgresql://localhost:5432/notifications` | PostgreSQL JDBC URL         |
| `DB_USER`                 | `postgres`                                   | Database username               |
| `DB_PASSWORD`             | `postgres`                                   | Database password               |
| `REDIS_HOST`              | `localhost`                                  | Redis host                      |
| `REDIS_PORT`              | `6379`                                       | Redis port                      |
| `SENDGRID_API_KEY`        | —                                            | SendGrid API key (email)        |
| `TWILIO_ACCOUNT_SID`      | —                                            | Twilio account SID (SMS)        |
| `TWILIO_AUTH_TOKEN`       | —                                            | Twilio auth token (SMS)         |
| `FCM_SERVER_KEY`          | —                                            | Firebase server key (push)      |
